package com.algolist.backend.problem.batch.github;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GitHubIngestLifecycle implements SmartLifecycle{
	
	private final GitHubIngestLauncher launcher;
	
	@Override
	public void start() {
		// 자동 실행은 안 함. 실행은 어드민 트리거로만.
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
