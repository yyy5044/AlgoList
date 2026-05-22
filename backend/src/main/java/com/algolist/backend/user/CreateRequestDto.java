package com.algolist.backend.user;

import lombok.Getter;

@Getter
// 생성 요청 시 body에 담긴 데이터를 받기 위한 Dto
public class CreateRequestDto {
	private String username;
	private String password;
}
