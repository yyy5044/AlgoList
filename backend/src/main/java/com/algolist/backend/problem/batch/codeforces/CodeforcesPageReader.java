package com.algolist.backend.problem.batch.codeforces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * [READ 단계] 허깅페이스 datasets-server API 에서 코드포스 문제를 읽는다. (재시작 가능, ItemStreamReader)
 *
 * 동작
 *   - 내부 커서 nextOffset 을 0 → 100 → 200 … 으로 직접 넘기며 페이지를 차례로 받아 한 건씩 흘려보낸다.
 *   - 청크가 커밋될 때마다 update() 가 호출되어 nextOffset 을 ExecutionContext(배치 메타데이터)에 저장한다.
 *   - 중단 후 재시작하면 open() 이 저장된 offset 을 복구해 거기서 이어서 읽는다.
 *
 * 중요한 불변식: 청크 크기 == 페이지 크기(둘 다 page-size). 이게 같아야
 *   "한 청크에 한 페이지(100건)"가 정확히 대응되어, 청크 커밋 시 저장하는 offset 이
 *   실제로 커밋된 지점과 일치한다. (다르면 재시작 시 일부 건너뜀/중복이 생길 수 있음)
 *
 * @StepScope 인 이유: nextOffset/exhausted 같은 가변 상태를 매 실행마다 0부터 깨끗이 시작하기 위함.
 */
@Slf4j
@Component
@StepScope
public class CodeforcesPageReader implements ItemStreamReader<JsonNode> {

    private static final String DATASET_URL =
            "https://datasets-server.huggingface.co/rows?dataset=open-r1/codeforces&config=default&split=train&offset=%d&length=%d";

    /** ExecutionContext 에 offset 을 저장할 때 쓰는 키. */
    private static final String OFFSET_KEY = "codeforces.read.offset";

    private final int pageSize;        // 허깅페이스 API 1회 요청당 가져올 개수(상한 100) == 청크 크기
    private final long requestDelayMs; // 페이지 요청 사이에 쉬는 시간(ms) — rate limit 예방용 throttle
    private final int maxRetries;      // 일시 오류(502/429 등) 시 재시도 횟수

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private long nextOffset = 0;             // 다음에 가져올 페이지의 시작 위치(내부 커서)
    private Iterator<JsonNode> currentPage;  // 지금 흘려보내는 중인 페이지의 행들
    private boolean exhausted = false;       // 데이터셋을 다 읽었는가?

    public CodeforcesPageReader(
            @Value("${codeforces.batch.page-size:100}") int pageSize,
            @Value("${codeforces.batch.request-delay-ms:300}") long requestDelayMs,
            @Value("${codeforces.batch.max-retries:3}") int maxRetries) {
        this.pageSize = pageSize;
        this.requestDelayMs = requestDelayMs;
        this.maxRetries = maxRetries;
    }

    /** 스텝 시작/재시작 시 1번 호출 — 저장된 offset 이 있으면 거기서 재개, 없으면 0부터. */
    @Override
    public void open(ExecutionContext ctx) {
        if (ctx.containsKey(OFFSET_KEY)) {
            nextOffset = ctx.getLong(OFFSET_KEY);
            log.info("[CF-READ] 재시작 감지 — offset {} 부터 재개", nextOffset);
        } else {
            nextOffset = 0;
        }
        currentPage = null;
        exhausted = false;
    }

    @Override
    public JsonNode read() {
        // 현재 페이지를 다 흘려보냈으면 다음 페이지를 가져온다.
        if (currentPage == null || !currentPage.hasNext()) {
            if (exhausted) {
                return null; // 더 읽을 게 없음 → 스텝 종료 신호
            }
            List<JsonNode> page = fetchPage(nextOffset);
            nextOffset += pageSize;

            if (page.isEmpty()) {            // 빈 페이지 = 데이터셋 끝
                exhausted = true;
                return null;
            }
            if (page.size() < pageSize) {    // 덜 찬 페이지 = 마지막 페이지
                exhausted = true;
            }
            currentPage = page.iterator();
        }
        return currentPage.next();
    }

    /** 청크 커밋 직전 호출 — 지금까지 읽은 위치를 체크포인트로 저장(청크 트랜잭션과 함께 커밋되어 원자적). */
    @Override
    public void update(ExecutionContext ctx) {
        ctx.putLong(OFFSET_KEY, nextOffset);
    }

    @Override
    public void close() {
        // 정리할 리소스 없음
    }

    private List<JsonNode> fetchPage(long offset) {
        throttle(); // 요청 사이 간격 두기 — 허깅페이스 rate limit 예방

        String url = String.format(DATASET_URL, offset, pageSize);
        log.info("[CF-READ] offset={} length={} 요청", offset, pageSize);

        String response = requestWithRetry(url);

        List<JsonNode> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode rows = root.get("rows");
            if (rows != null && rows.isArray()) {
                for (JsonNode rowWrapper : rows) {
                    // 응답 구조: { "rows": [ { "row": {...실제필드...} }, ... ] } → 한 겹 벗긴다.
                    JsonNode row = rowWrapper.get("row");
                    if (row != null) {
                        result.add(row);
                    }
                }
            } else {
                log.warn("[CF-READ] rows 필드가 없거나 배열이 아님 (offset={})", offset);
            }
        } catch (Exception e) {
            // 페이지 단위 파싱 실패 시 빈 페이지로 처리 → 다음 read() 에서 끝으로 간주된다.
            log.error("[CF-READ] 페이지 파싱 실패 offset={}", offset, e);
            return Collections.emptyList();
        }

        log.info("[CF-READ] offset={} 에서 {}건 확보", offset, result.size());
        return result;
    }

    /**
     * 페이지 요청을 보내되, 일시적 서버 오류(5xx, 예: 502)나 rate limit(429)이면 backoff 후 재시도한다.
     * 그 밖의 오류(4xx 등)나 재시도 소진 시에는 예외를 그대로 던진다(→ 스텝 실패, 다음 기동에 재개).
     */
    private String requestWithRetry(String url) {
        int attempt = 0;
        while (true) {
            try {
                return restClient.get().uri(url).retrieve().body(String.class);
            } catch (HttpStatusCodeException e) {
                HttpStatusCode status = e.getStatusCode();
                boolean retryable = status.is5xxServerError() || status.value() == 429;
                if (!retryable || attempt >= maxRetries) {
                    throw e; // 재시도 불가하거나 횟수 소진 → 그대로 던짐
                }
                attempt++;
                long backoff = requestDelayMs * attempt * 5L; // 점점 더 길게 쉬기
                log.warn("[CF-READ] 일시 오류 {} — {}ms 후 재시도 {}/{}", status, backoff, attempt, maxRetries);
                sleep(backoff);
            }
        }
    }

    /** 요청 사이에 requestDelayMs 만큼 쉰다. (단일 스레드 배치라 그냥 sleep 으로 충분) */
    private void throttle() {
        sleep(requestDelayMs);
    }

    private void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
        }
    }
}
