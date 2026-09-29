package com.algolist.backend;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.algolist.backend.problem.ProblemService;
import com.algolist.backend.problem.dto.ProblemDto;

/**
 * 문제 검색 플로우(DB 미스 → GitHub 검색 → 파싱 → 이미지 처리 → 저장 → 재조회)의
 * DB 커넥션 점유 시간을 측정한다.
 *
 * 측정 방식: algoListDataSource 를 데코레이터로 감싸 getConnection() ~ close() 구간을
 * 체크아웃 단위로 기록. 트랜잭션이 걸려 있으면 커밋까지 반납이 안 되므로
 * "커넥션을 몇 번, 각각 얼마나 물고 있었는지"가 그대로 드러난다.
 *
 * before(searchProblem 에 @Transactional): 체크아웃 1회 ≈ 전체 소요시간 (GitHub I/O 동안 점유)
 * after(현재 main, DB 작업만 ProblemWriter 트랜잭션): 짧은 체크아웃 여러 회
 *
 * 실행 전제: github.token 프로퍼티 필요 (application-secret.properties)
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(ConnectionHoldMeasurementTest.RecorderConfig.class)
class ConnectionHoldMeasurementTest {

    /** DB에 없어서 GitHub 경로를 타게 만드는 검색어 (한글 → codeforces 영문 타이틀과 미스) */
    private static final String QUERY = "피보나치";

    @Autowired
    private ProblemService problemService;

    @Autowired
    private DataSource dataSource;

    @Value("${github.token:}")
    private String githubToken;

    @Test
    void 검색_커넥션_점유시간_측정() {
        if (githubToken.isBlank()) {
            throw new IllegalStateException(
                    "github.token 이 없음 — application-secret.properties 에 github.token=... 을 넣어야 GitHub 검색 경로가 동작함");
        }

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        cleanup(jdbc); // 이전 실행 잔여물 제거 → 반드시 DB 미스로 시작

        Integer dbHit = jdbc.queryForObject(
                "SELECT COUNT(*) FROM problems WHERE REPLACE(title,' ','') LIKE CONCAT('%', REPLACE(?, ' ', ''), '%')",
                Integer.class, QUERY);
        System.out.println("=== 사전 확인: DB 매칭 " + dbHit + "건 (0이어야 GitHub 경로) ===");

        try {
            Recorder.reset();
            long start = System.currentTimeMillis();

            List<ProblemDto> results = problemService.searchProblem(QUERY);

            long wall = System.currentTimeMillis() - start;
            System.out.println("=== 검색 완료: " + results.size() + "건, 전체 소요(wall) " + wall + "ms ===");
            Recorder.report(start);
        } finally {
            cleanup(jdbc); // 이번 실행이 넣은 행 제거 → 다음 실행(다른 브랜치)도 동일 조건
        }
    }

    /** QUERY 로 검색되는 문제(및 카테고리)를 삭제해 DB 미스 상태로 되돌린다. */
    private void cleanup(JdbcTemplate jdbc) {
        int categories = jdbc.update(
                "DELETE pc FROM problem_categories pc JOIN problems p USING (problem_id) "
                        + "WHERE REPLACE(p.title,' ','') LIKE CONCAT('%', REPLACE(?, ' ', ''), '%')", QUERY);
        int problems = jdbc.update(
                "DELETE FROM problems WHERE REPLACE(title,' ','') LIKE CONCAT('%', REPLACE(?, ' ', ''), '%')", QUERY);
        if (problems > 0) {
            System.out.println("=== 정리: problems " + problems + "건, categories " + categories + "건 삭제 ===");
        }
    }

    // ---------- 계측 ----------

    /** 체크아웃 1건: 시작 시각(wall)과 점유 시간(ms) */
    record Checkout(long startMs, long heldMs) {}

    static class Recorder {
        static final Queue<Checkout> CHECKOUTS = new ConcurrentLinkedQueue<>();

        static void reset() {
            CHECKOUTS.clear();
        }

        static void report(long searchStartMs) {
            List<Checkout> list = List.copyOf(CHECKOUTS);
            long sum = 0;
            long max = 0;
            int i = 0;
            System.out.println("=== 커넥션 체크아웃 " + list.size() + "회 ===");
            for (Checkout c : list) {
                i++;
                System.out.printf("    #%d: 점유 %dms (검색 시작 +%dms 시점)%n",
                        i, c.heldMs(), c.startMs() - searchStartMs);
                sum += c.heldMs();
                max = Math.max(max, c.heldMs());
            }
            System.out.println("=== 점유 합계 " + sum + "ms / 최장 단일 점유 " + max + "ms ===");
        }
    }

    @TestConfiguration
    static class RecorderConfig {
        /** algoListDataSource(Primary)만 감싼다 — 배치 메타데이터 DS 는 검색 플로우와 무관 */
        @Bean
        static BeanPostProcessor connectionHoldRecordingPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if ("algoListDataSource".equals(beanName) && bean instanceof DataSource ds) {
                        return new DelegatingDataSource(ds) {
                            @Override
                            public Connection getConnection() throws SQLException {
                                return recording(super.getConnection());
                            }

                            @Override
                            public Connection getConnection(String username, String password) throws SQLException {
                                return recording(super.getConnection(username, password));
                            }
                        };
                    }
                    return bean;
                }
            };
        }

        /** close() 시점에 점유 시간을 기록하는 Connection 프록시 (중복 close 는 1회만 기록) */
        private static Connection recording(Connection real) {
            long startWall = System.currentTimeMillis();
            long startNano = System.nanoTime();
            AtomicBoolean recorded = new AtomicBoolean(false);
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] { Connection.class },
                    (proxy, method, args) -> {
                        if ("close".equals(method.getName()) && recorded.compareAndSet(false, true)) {
                            Recorder.CHECKOUTS.add(new Checkout(
                                    startWall, (System.nanoTime() - startNano) / 1_000_000));
                        }
                        try {
                            return method.invoke(real, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }
}
