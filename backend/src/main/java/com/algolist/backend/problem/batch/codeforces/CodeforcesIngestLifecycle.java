package com.algolist.backend.problem.batch.codeforces;

import java.util.Set;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 코드포스 수집 Job 을 앱 생명주기에 묶는다. (기존 StartupRunner 대체)
 *
 * - start(): 앱 기동 시 자동 호출. 배치를 백그라운드 스레드에서 실행해 부팅을 막지 않는다.
 *            start(job, 빈 파라미터)로 "같은 인스턴스"를 가리킴 → 처음=0부터 / 실패·정지=재개 / 완료=스킵.
 * - stop():  앱이 graceful 하게 종료될 때(IDE stop, Ctrl-C/SIGTERM, devtools restart 등) 호출된다.
 *            돌고 있는 실행에 jobOperator.stop() 을 걸면 다음 청크 경계에서 **STOPPED** 로 마감된다.
 *            → 강제 kill 때처럼 STARTED 로 박제되지 않으므로, 다음 기동에 깔끔히 재개된다.
 *
 * 주의: 강제 kill(kill -9 / taskkill /F)은 stop() 을 호출하지 못해 여전히 STARTED 가 남을 수 있다.
 *       (그것까지 막으려면 "시작 시 stale STARTED 자가복구"를 추가로 넣어야 함)
 *
 * SmartLifecycle 기본 phase(=가장 높음) → 웹서버가 뜬 뒤 시작되고, 종료 시엔 가장 먼저 멈춘다
 *   (DataSource 가 살아있는 동안 STOPPED 기록이 끝나도록).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeforcesIngestLifecycle implements SmartLifecycle {

    private final JobOperator jobOperator;
    private final Job codeforcesIngestJob; 

    private volatile Thread worker;
    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
        worker = new Thread(this::runJob, "codeforces-ingest");
        worker.setDaemon(true);
        worker.start();
    }

    private void runJob() {
        try {
            JobExecution execution = jobOperator.start(codeforcesIngestJob, new JobParameters());
            log.info("[CF-BATCH] 실행/재개 완료 executionId={} status={}",
                    execution.getId(), execution.getStatus());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[CF-BATCH] 이미 전량 적재 완료 — 이번 기동에선 스킵");
        } catch (Exception e) {
            log.error("[CF-BATCH] 실행 실패", e);
        } finally {
            running = false;
        }
    }

    @Override
    public void stop() {
        try {
            Set<Long> runningExecutions = jobOperator.getRunningExecutions(CodeforcesIngestJobConfig.JOB_NAME);
            for (Long executionId : runningExecutions) {
                log.info("[CF-BATCH] 종료 감지 — 실행 {} 정지 요청(STOPPED 로 마감)", executionId);
                jobOperator.stop(executionId);
            }
        } catch (Exception e) {
            log.warn("[CF-BATCH] 정지 요청 중 오류(무시하고 종료 진행)", e);
        }
        // 배치가 실제로 멈출 때까지 잠깐 대기 → STOPPED 기록이 끝난 뒤 컨텍스트 종료가 진행되도록
        Thread w = this.worker;
        if (w != null) {
            try {
                w.join(30_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
