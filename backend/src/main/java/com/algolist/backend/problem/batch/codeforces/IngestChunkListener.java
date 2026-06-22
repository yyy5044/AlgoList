package com.algolist.backend.problem.batch.codeforces;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.algolist.backend.problem.dto.ProblemDto;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * 청크가 끝날 때마다 "이번 청크 성공 N건 / 실패 M건"을 로그로 남긴다.
 *
 * 주의(Batch 6): 새 ChunkOrientedStep 은 레거시 afterChunk(ChunkContext) 가 아니라
 *   타입형 afterChunk(Chunk<O>) 를 호출한다. 그래서 그쪽을 오버라이드한다.
 *   - 성공 = chunk.size()           (이번 청크에서 Writer 까지 도달한 건수)
 *   - 실패 = skipCount 의 증가분      (이번 청크에서 skip 된 건수)
 *
 * @StepScope 라 매 실행마다 새로 만들어지고, #{stepExecution} 로 현재 스텝 실행을 주입받아 누적 카운트를 읽는다.
 */
@Slf4j
@Component
@StepScope
public class IngestChunkListener implements ChunkListener<JsonNode, ProblemDto> {

    private final StepExecution stepExecution;
    private long prevSkip = 0;

    public IngestChunkListener(@Value("#{stepExecution}") StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void afterChunk(Chunk<ProblemDto> chunk) {
        long skip = stepExecution.getSkipCount();
        long failThisChunk = skip - prevSkip;
        prevSkip = skip;

        log.info("[CF-CHUNK] 성공 {}건 / 실패 {}건 (누적: 성공 {}, 실패 {})",
                chunk.size(), failThisChunk, stepExecution.getWriteCount(), skip);
    }
}
