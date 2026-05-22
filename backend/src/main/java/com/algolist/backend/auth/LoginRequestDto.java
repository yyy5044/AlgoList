package com.algolist.backend.auth;

import lombok.Data;

@Data
public class LoginRequestDto {
	private String username;
	private String password;
}
