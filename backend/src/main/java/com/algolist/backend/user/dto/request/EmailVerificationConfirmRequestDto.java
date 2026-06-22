package com.algolist.backend.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 인증 요청 후 코드를 입력할 때 사용할 Dto
public class EmailVerificationConfirmRequestDto {
	private String email;
	private String code;
}
