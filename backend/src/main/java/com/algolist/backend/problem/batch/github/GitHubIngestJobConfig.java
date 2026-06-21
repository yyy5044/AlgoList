package com.algolist.backend.problem.batch.github;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.algolist.backend.problem.batch.common.ProblemItemWriter;
import com.algolist.backend.problem.dto.ProblemDto;

@Configuration
public class GitHubIngestJobConfig {
	
	public static final String JOB_NAME = "githubIngestJob";
	public static final String STEP_NAME = "githubIngestStep";
	
	@Bean
	Step githubIngestStep(
			JobRepository jobRepository,
			PlatformTransactionManager txManager,
			GitHubProblemReader reader,
			GitHubProblemProcessor processor,
			ProblemItemWriter writer // 재활용
			) {
		
		return new StepBuilder(STEP_NAME, jobRepository)
				.<String, ProblemDto>chunk(100)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.transactionManager(txManager)
				.build();
	}
	
	@Bean
	Job githubIngestJob(JobRepository jobRepository, Step githubIngestStep) {
		return new JobBuilder(JOB_NAME, jobRepository)
				.start(githubIngestStep)
				.build();
	}
}
