package com.algolist.backend.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
	
	private final UserDao userDao;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // username 기반으로 받아온 객체가 존재하는지 확인
		UserDto user = userDao.selectUserForAuth(username);
		if(user == null) {
			throw new UsernameNotFoundException(username);
		}
		
		return new CustomUserDetails(user); // 존재하는 유저 객체가 있다면 만들어놓은 CustomUserDetails 객체로 반환받음
	}

}
