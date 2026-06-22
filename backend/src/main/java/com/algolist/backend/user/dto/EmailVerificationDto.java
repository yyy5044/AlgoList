package com.algolist.backend.user.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 이메일 인증 진행 시 인증을 진행한 이메일, 코드, 코드 만료일자 등 인증 데이터를 가져오기 위한 Dto
public class EmailVerificationDto {
	private Long verificationId;
	private String email;
	private String codeHash;
	private LocalDateTime expiresAt;
	private LocalDateTime verifiedAt;
	private LocalDateTime consumedAt;
	private int failedAttempts;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
