package com.algolist.backend.user.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 로그인 시 정지된 유저의 사유, 기간을 조회하기 위한 Dto
public class UserSuspensionInfoDto {
	private String reason;
	private LocalDateTime suspendedUntil;
}
