package com.algolist.backend.user;

import lombok.Data;

@Data
public class UserDto {
	private Long userId;
	private String username;
	private String password;
	private String role;
}
