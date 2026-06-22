package com.algolist.backend.user.dao;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.algolist.backend.user.dto.EmailVerificationDto;

@Mapper
// 이메일 인증을 위한 Dao
public interface EmailVerificationDao {

	// 이메일 기반으로 EmailVerficationDto 객체 가져오기
	public EmailVerificationDto selectByEmail(@Param("email") String email);

	// 새로운 인증 요청 시 객체 생성
	public int insertVerification(@Param("email") String email,
			@Param("codeHash") String codeHash,
			@Param("expiresAt") LocalDateTime expiresAt);

	// 이미 인증 요청을 보낸 이메일로 다시 인증 요청을 보낼 때 갱신
	public int refreshVerification(@Param("email") String email,
			@Param("codeHash") String codeHash,
			@Param("expiresAt") LocalDateTime expiresAt);

	// 인증 실패 시 failed_attempts 값 증가
	public int incrementFailedAttempts(@Param("email") String email);

	// 인증 성공 시 verified_at 값 갱신
	public int markVerified(@Param("email") String email, @Param("verifiedAt") LocalDateTime verifiedAt);

	// 인증 성공한 email로 계정 생성 시 consumed_at 값 갱신
	public int consumeVerifiedEmail(@Param("email") String email);

	// threshold가 지난 데이터들 삭제
	public int deleteExpiredOrConsumedBefore(@Param("threshold") LocalDateTime threshold);
}
