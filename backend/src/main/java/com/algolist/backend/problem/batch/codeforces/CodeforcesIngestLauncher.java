package com.algolist.backend.problem.batch.codeforces;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 코드포스 수집 Job 의 실행/중지/상태를 한 곳에서 관리한다.
 *
 * 시작 시 자동 실행(SmartLifecycle)과 관리자 수동 트리거(AdminBatchController)가 모두 이 한 곳을 거치므로,
 * worker 스레드와 running 플래그가 한 군데에서만 관리된다.
 *
 * - launch()         : 백그라운드 스레드에서 Job 실행. 이미 돌고 있으면 false (중복 실행 방지).
 *                      start(job, 빈 파라미터)라 같은 인스턴스를 가리킴 → 처음=0부터 / 실패·정지=재개 / 완료=스킵.
 * - stopGracefully() : 돌고 있는 실행에 stop 을 걸어 다음 청크 경계에서 STOPPED 로 마감. 다음 실행 시 체크포인트부터 재개.
 * - isRunning()      : 현재 실행 중 여부.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeforcesIngestLauncher {

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Job codeforcesIngestJob;

    private volatile Thread worker;
    private volatile boolean running = false;

    /** 백그라운드 스레드에서 Job 실행. 이미 실행 중이면 아무것도 안 하고 false 반환. */
    public synchronized boolean launch() {
        if (running) {
            log.info("[CF-BATCH] 이미 실행 중 — 트리거 무시");
            return false;
        }
        running = true;
        worker = new Thread(this::runJob, "codeforces-ingest");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    private void runJob() {
        try {
            JobExecution execution = jobOperator.start(codeforcesIngestJob, new JobParameters());
            log.info("[CF-BATCH] 실행/재개 완료 executionId={} status={}",
                    execution.getId(), execution.getStatus());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[CF-BATCH] 이미 전량 적재 완료 — 스킵");
        } catch (Exception e) {
            log.error("[CF-BATCH] 실행 실패", e);
        } finally {
            running = false;
        }
    }

    /**
     * 중지 "요청"만 보낸다(즉시 반환). 실제 중지는 다음 청크 경계에서 워커 스레드가 처리하고,
     * 그때 worker 의 finally 에서 running 이 false 가 된다. → 관리자 중지 버튼(엔드포인트)용.
     */
    public void requestStop() {
        try {
            // Batch 6: id 기반 getRunningExecutions/stop(long) 은 deprecated → 객체 기반 API 사용
            for (JobExecution execution : jobRepository.findRunningJobExecutions(CodeforcesIngestJobConfig.JOB_NAME)) {
                log.info("[CF-BATCH] 중지 요청 실행={}", execution.getId());
                jobOperator.stop(execution);
            }
        } catch (Exception e) {
            log.warn("[CF-BATCH] 중지 요청 중 오류(무시)", e);
        }
    }

    /**
     * 중지 요청 + 워커가 "실제로 멈출 때까지" 대기한다. (관리자 중지 버튼 / 앱 종료 둘 다 사용)
     * 무한 대기는 스레드를 영구 점유할 수 있으니 상한(30초)을 둔다.
     *
     * @return true = 실제로 멈춤(STOPPED 까지 완료) / false = 상한 내에 못 멈춤(아직 정리 중)
     */
    public boolean stopGracefully() {
        requestStop();
        Thread w = this.worker;
        if (w == null) {
            return true; // 돌고 있는 워커가 없으면 이미 멈춘 상태로 본다
        }
        try {
            w.join(30_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return !w.isAlive(); // 스레드가 끝났으면 실제로 멈춤, 아직 살아있으면(타임아웃) 정리 중
    }

    public boolean isRunning() {
        return running;
    }
}
