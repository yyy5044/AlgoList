package com.algolist.backend.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 인증 요청을 보낼 때 사용할 Dto
public class EmailVerificationRequestDto {
	private String email;
}
