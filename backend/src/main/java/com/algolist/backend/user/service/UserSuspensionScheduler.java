package com.algolist.backend.user.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.algolist.backend.user.dao.UserDao;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserSuspensionScheduler {

	private final UserDao userDao;

	@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
	@Transactional
	// 매일 00:05에 정지 종료일이 지난 유저를 자동 해제
	public void releaseExpiredSuspensions() {
		int releaseCount = userDao.releaseExpiredSuspensionHistories();
		if (releaseCount > 0) {
			userDao.activateUsersWithoutActiveSuspension();
		}
	}
}
