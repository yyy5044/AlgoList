package com.algolist.backend.problem.batch.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 실패한 페이지를 failed_pages 테이블에 기록한다.
 *
 * REQUIRES_NEW 인 이유: 이 기록은 진행 중인 청크 트랜잭션과 "독립적으로" 즉시 커밋되어야 한다.
 * (청크가 나중에 롤백되더라도 "이 페이지 실패함" 기록은 남아야 하므로)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedPageRecorder {

    private final FailedPageDao dao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String source, long pageOffset, int pageSize, String reason) {
        String trimmed = (reason != null && reason.length() > 1000)
                ? reason.substring(0, 1000) : reason;
        dao.insertFailedPage(source, pageOffset, pageSize, trimmed);
        log.warn("[FAILED-PAGE] 기록됨 source={} offset={} size={} reason={}",
                source, pageOffset, pageSize, trimmed);
    }
}