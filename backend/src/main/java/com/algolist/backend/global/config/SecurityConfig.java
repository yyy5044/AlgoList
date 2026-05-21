package com.algolist.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration		// 이 빈을 설정 클래스야
@EnableWebSecurity 	// Spring Security 웹 보안 활성화
public class SecurityConfig {
	
	// TODO: 비밀번호 인코딩을 위한 PasswordEncoder를 만들어보자.
	@Bean
	PasswordEncoder passwordEncoder() {
		// 기본 encoder로 bcrypt 방식
		// 비밀번호가 같은지 비교하려면 passwordEncoder.matches(a, b) 메서드를 이용할 것
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
	
	
}
