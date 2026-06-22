
package com.algolist.backend.problem.batch.codeforces;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.algolist.backend.problem.batch.common.ProblemItemWriter;
import com.algolist.backend.problem.batch.common.ProblemParseException;
import com.algolist.backend.problem.dto.ProblemDto;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 코드포스 문제 수집 Job 정의 (Chunk 지향: Read -> Process -> Write).
 *
 * 한눈에 보는 구조
 *   codeforcesIngestJob                                  (실행 단위, 관리자 트리거로 실행)
 *     └─ codeforcesIngestStep (chunk = page-size)        (작업 한 덩어리)
 *          ├─ reader   : CodeforcesPageReader  (전량을 페이징하며 읽음 + offset 체크포인트)
 *          ├─ processor: CodeforcesParser::parse(JsonNode -> ProblemDto, 실패 시 예외)
 *          └─ writer   : ProblemItemWriter     (algolist DB 적재, 소스 공용)
 *
 * 핵심 동작
 * - 1 실행 = 데이터셋 전량 적재. Reader 가 offset 을 청크마다 ExecutionContext 에 저장 → 중단 시 같은 인스턴스
 *   재시작으로 그 지점부터 재개(표준 재시작). 그래서 Job 에 incrementer 를 달지 않는다(고정 파라미터).
 * - 청크 크기 == page-size. Reader 가 한 페이지(100)를 한 청크로 흘려보내 "청크=페이지"가 일치(체크포인트 정합성).
 * - faultTolerant + skip(ProblemParseException): 파싱 실패한 행은 건너뛰고 작업은 계속. (실패 수는 skipCount 로 집계)
 */
@Configuration
public class CodeforcesIngestJobConfig {

    public static final String JOB_NAME = "codeforcesIngestJob";
    public static final String STEP_NAME = "codeforcesIngestStep";

    @Bean
    public Step codeforcesIngestStep(
            JobRepository jobRepository,
            PlatformTransactionManager txManager,   // algolist 데이터소스용 트랜잭션 매니저(자동 구성, 유일)
            CodeforcesPageReader reader,
            CodeforcesParser parser,
            ProblemItemWriter writer,
            IngestChunkListener chunkListener,
            @Value("${codeforces.batch.page-size:100}") int pageSize,
            @Value("${codeforces.batch.skip-limit:1000}") int skipLimit) {

        return new StepBuilder(STEP_NAME, jobRepository)
                .<JsonNode, ProblemDto>chunk(pageSize)   // 100건씩 한 청크 (== 한 페이지)
                .reader(reader)                 // 읽기 (허깅페이스 전량, offset 체크포인트)
                .processor(parser::parse)       // 변환 (JsonNode → ProblemDto), 실패 시 ProblemParseException
                .writer(writer)                 // 저장 (algolist)
                .transactionManager(txManager)  // 청크 한 묶음의 commit/rollback 경계
                .faultTolerant()                // 내결함성 켜기
                .skip(ProblemParseException.class)  // 파싱 실패 행은 skip(실패로 집계)하고 계속
                .skipLimit(skipLimit)           // 안전장치: 실패가 이 수를 넘으면 스텝을 실패시킴(체계적 오류 감지)
                .listener(chunkListener)        // 청크마다 성공/실패 로그
                .build();
    }

    /**
     * [Job] Step 하나로 구성. incrementer 없음(고정 파라미터로 실행).
     * → 시작 러너가 같은 파라미터로 start 하면, 완료된 인스턴스는 "이미 완료" 예외, 실패한 인스턴스는 재개된다.
     */
    @Bean
    public Job codeforcesIngestJob(JobRepository jobRepository, Step codeforcesIngestStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(codeforcesIngestStep)
                .build();
    }
}
