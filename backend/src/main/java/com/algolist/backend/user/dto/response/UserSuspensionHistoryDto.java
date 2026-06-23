package com.algolist.backend.user.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 유저 정지 이력을 조회하기 위한 Dto
public class UserSuspensionHistoryDto {
	private Long suspensionId;
	private Long userId;
	private String username;
	private String nickname;
	private String accountStatus;
	private String reason;
	private String suspendedAt;
	private String suspendedUntil;
	private String suspendedByUsername;
	private String releasedAt;
	private String releasedByUsername;
	private String releaseReason;
	private String suspensionStatus;
}
