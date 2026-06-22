package com.algolist.backend.user.service;

import java.time.LocalDateTime;

public interface EmailVerificationService {

	public void sendVerificationCode(String email);

	public void confirmVerificationCode(String email, String code);

	public String normalizeEmail(String email);

	public LocalDateTime validateSignupEmail(String email);

	public void consumeVerifiedEmail(String email);

	public int deleteExpiredOrConsumedBefore(LocalDateTime threshold);
}
