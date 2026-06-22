package com.algolist.backend.problem.batch.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedProblemRecorder {

    private final FailedProblemDao dao;

    @Transactional(transactionManager = "batchSystemTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void record(String site, String number, String fileUrl, String reason) {
        dao.insertFailedProblem(site, number, fileUrl, reason);
    }
}
