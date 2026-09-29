package com.algolist.backend;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class BatchMeasurementTest {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job codeforcesIngestJob;

    @Test
    void 배치_동기_실행_힙측정() throws Exception {
        long start = System.currentTimeMillis();

        JobParameters params = new JobParametersBuilder()
                .addLong("measureTime", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobOperator.start(codeforcesIngestJob, params);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("=== 배치 종료 status=" + execution.getStatus()
                + " 소요=" + elapsed + "ms ===");
    }
}