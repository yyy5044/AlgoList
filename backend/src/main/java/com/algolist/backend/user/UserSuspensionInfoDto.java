package com.algolist.backend.user;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSuspensionInfoDto {
	private String reason;
	private LocalDateTime suspendedUntil;
}
