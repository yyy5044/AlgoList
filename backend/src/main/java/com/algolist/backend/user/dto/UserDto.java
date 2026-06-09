package com.algolist.backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 비밀번호를 포함한 유저의 대부분 데이터를 가져오기 위한 Dto(로그인 시 사용, ADMIN의 회원 목록 조회 시 사용)
public class UserDto {
	private Long userId;
	private String username;
	private String nickname;
	private String password;
	private String role;
	private String accountStatus;
	private String createdAt;
	private String deletedAt;
}
