package com.algolist.backend.problem;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.algolist.backend.problem.dto.ProblemDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProblemWriter {

    private final ProblemDao dao;

    
    @Transactional
    public void save(ProblemDto dto) {
        dao.insertProblem(dto);

        if (dto.getProblemId() != null && dto.getProblemId() > 0
                && dto.getCategory() != null) {
            for (String cat : dto.getCategory()) {
                dao.insertCategory(dto.getProblemId(), cat);
            }
        }
    }
}
