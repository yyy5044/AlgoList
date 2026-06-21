package com.algolist.backend.problem.batch.github;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class GitHubIngestLauncher {
	private final JobOperator jobOperator;
	private final JobRepository jobRepository;
	private final Job githubIngestJob;
	
	private volatile Thread worker;
	private volatile boolean running = false;
	
	public synchronized boolean launch() {
		if (running) {
			log.info("[GH-BATCH] 이미 실행 중 — 트리거 무시");
			return false;
		}
		
		running = true;
		worker = new Thread(this::runJob, "github-ingest");
		worker.setDaemon(true);
		
		worker.start();
		return true;
	}
	
	private void runJob() {
	    try {
	        jobOperator.start(githubIngestJob, new JobParameters());
	    } catch (JobInstanceAlreadyCompleteException e) {
	        log.info("[GH-BATCH] 이미 전량 적재 완료 — 스킵");
	    } catch (Exception e) {
	        log.error("[GH-BATCH] 실행 실패", e);
	    } finally {
	        running = false;
	    }
	}
	
	public void requestStop() {
		try {
			for (JobExecution execution : jobRepository.findRunningJobExecutions(GitHubIngestJobConfig.JOB_NAME)) {
				log.info("[GH-BATCH] 중지 요청 실행={}", execution.getId());
				jobOperator.stop(execution);
			}
		} catch (Exception e) {
			log.warn("[GH-BATCH] 중지 요청 중 오류(무시)", e);
		}
	}
	
	/**
	 * 30초까지 기다리는 건 어드민에게 멈췄다는 응답을 주기 위해 기다리는 시간.
	 * 워커는 결국 종료하게 되어있지만 30초 이후에 종료하면 응답을 바로 못 주는 것일 뿐임.
	 * */
	public boolean stopGracefully() {
	    requestStop();
	    Thread w = this.worker;
	    if (w == null) {
	        return true;
	    }
	    try {
	        w.join(30_000); // 멈출 때까지 30초 기다림
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }
	    return !w.isAlive(); // worker가 죽었으면 true
	}
	
	public boolean isRunning() {
	    return running;
	}
}
