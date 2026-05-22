package com.algolist.backend.auth;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.algolist.backend.user.UserDto;

import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails { // UserDetail 객체 생성을 위한 구현
	
	private final UserDto user;
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() { // 역할 반환을 위한 로직
		String role = user.getRole();
		if (role != null) {
			return List.of(new SimpleGrantedAuthority("ROLE_" + role)); // DB에는 USER로 저장되어 있으므로 ROLE_USER로 반환하기
		} else {
			return List.of(); 
		}
	}
	
	@Override
	public @Nullable String getPassword() { // 비밀번호 반환을 위한 로직
		return user.getPassword();
	}
	
	@Override
	public String getUsername() { // username 반환을 위한 로직(상황따라 email 등으로도 변경 가능)
		return user.getUsername();
	}

}
