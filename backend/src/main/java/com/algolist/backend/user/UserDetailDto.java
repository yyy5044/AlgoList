package com.algolist.backend.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetailDto {
	private String username;
	private String nickname;
	private String profileImageUrl;
	private String bio;
	private String role;
}
