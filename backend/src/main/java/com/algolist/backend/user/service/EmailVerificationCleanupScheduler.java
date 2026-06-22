package com.algolist.backend.user.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

	private static final int RETENTION_DAYS = 7;

	private final EmailVerificationService emailVerificationService;

	// 매일 새벽 3시에 만료된 인증 데이터들 삭제
	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void cleanupEmailVerifications() {
		emailVerificationService.deleteExpiredOrConsumedBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
	}
}
