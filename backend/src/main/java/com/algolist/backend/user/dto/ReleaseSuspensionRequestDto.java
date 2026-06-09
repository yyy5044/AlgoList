package com.algolist.backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 정지 해제 시 정지 사유를 적기 위한 Dto
public class ReleaseSuspensionRequestDto {
	private String releaseReason;
}
