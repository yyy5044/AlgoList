package com.algolist.backend.problem.batch.common;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.algolist.backend.problem.ProblemDao;
import com.algolist.backend.problem.dto.ProblemDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [WRITE 단계 - 모든 소스 공용] ProblemDto 들을 비즈니스 DB(algolist)에 적재한다.
 *
 * 소스(코드포스/백준/...)가 무엇이든 결과가 ProblemDto 이기만 하면 그대로 저장하므로,
 * 모든 수집 Job 이 이 한 Writer 를 공유한다. (그래서 codeforces 패키지가 아니라 common 에 둔다.)
 *
 * 설계 메모
 * - write(Chunk) 는 청크(최대 chunk-size 건) 단위로 한 번 호출되고, 그 전체가 청크 트랜잭션 안에서 돈다.
 *   → 건당 @Transactional 을 걸지 않는다. 트랜잭션 경계는 Step 의 청크가 잡는다.
 * - 매퍼가 INSERT IGNORE 라 number 중복이면 조용히 무시 → 재실행해도 멱등.
 *   IGNORE 로 무시되면 LAST_INSERT_ID 가 0 이 되어 problemId 가 0 으로 채워지므로,
 *   problemId > 0(=실제로 새로 들어간 행)일 때만 카테고리를 넣는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemItemWriter implements ItemWriter<ProblemDto> {

    private final ProblemDao dao;

    @Override
    public void write(Chunk<? extends ProblemDto> chunk) {
        for (ProblemDto dto : chunk) {
            dao.insertProblem(dto); // useGeneratedKeys 로 problemId 채움 (신규면 >0, 중복이면 0)

            if (dto.getProblemId() != null && dto.getProblemId() > 0
                    && dto.getCategory() != null) {
                for (String category : dto.getCategory()) {
                    dao.insertCategory(dto.getProblemId(), category);
                }
            }
        }
        log.info("[WRITE] {}건 적재 시도 완료(중복은 INSERT IGNORE 로 무시됨)", chunk.size());
    }
}
