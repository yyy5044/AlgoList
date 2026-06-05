package com.algolist.backend.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 유저 세부정보 조회를 위해 사용하는 DTO
public class UserDetailDto {
	private String username;
	private String nickname;
	private String profileImageUrl;
	private String bio;
	private String role;
}
