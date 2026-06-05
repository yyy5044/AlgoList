package com.algolist.backend.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
	private Long userId;
	private String username;
	private String password;
	private String role;
	private String accountStatus;
}
