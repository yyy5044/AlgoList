package com.algolist.backend.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.algolist.backend.user.dao.EmailVerificationDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationAttemptService {

	private final EmailVerificationDao emailVerificationDao;

	// 인증 실패 시에도 실패 횟수는 증가한 채로 남아야 하므로 별도의 트랜잭션 구현
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordFailedAttempt(String email) {
		emailVerificationDao.incrementFailedAttempts(email);
	}
}
