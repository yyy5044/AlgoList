package com.algolist.backend.user;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 유저 정지 시 사유와 정지 기간을 적기 위한 메서드
public class SuspendUserRequestDto {
	private String reason;
	private LocalDate suspendedUntil;
}
