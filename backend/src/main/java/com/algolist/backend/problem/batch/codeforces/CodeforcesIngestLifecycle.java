package com.algolist.backend.problem.batch.codeforces;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 앱이 graceful 하게 종료될 때(IDE stop, SIGTERM 등) 돌고 있던 코드포스 수집 배치를
 * STOPPED 로 마감하기 위한 "종료 훅".
 *
 * 실행은 관리자 트리거(AdminBatchController → CodeforcesIngestLauncher)로만 하므로 start() 는 아무것도 하지 않는다.
 *
 * SmartLifecycle 을 쓰는 이유: stop() 이 빈 소멸(DataSource 등 정리)보다 "먼저" 호출되므로,
 * 배치가 DB·메타데이터에 STOPPED 기록을 끝낼 때까지 안전하게 기다릴 수 있다. (@PreDestroy 보다 타이밍이 안전)
 * isRunning() 이 true 일 때만 stop() 이 호출되므로, 돌고 있는 배치가 있을 때만 정지 처리된다.
 */
@Component
@RequiredArgsConstructor
public class CodeforcesIngestLifecycle implements SmartLifecycle {

    private final CodeforcesIngestLauncher launcher;

    @Override
    public void start() {
        // 자동 실행하지 않는다. 실행은 관리자 트리거로만.
    }

    @Override
    public void stop() {
        launcher.stopGracefully();
    }

    @Override
    public boolean isRunning() {
        return launcher.isRunning();
    }
}
