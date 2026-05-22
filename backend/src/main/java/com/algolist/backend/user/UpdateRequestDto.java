package com.algolist.backend.user;

import lombok.Getter;

@Getter
// 수정 요청 시 body에 담긴 데이터를 받기 위한 Dto
public class UpdateRequestDto {
	private String password;
}
