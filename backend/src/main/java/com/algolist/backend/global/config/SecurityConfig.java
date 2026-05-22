package com.algolist.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// ⚠️ 개발용 설정: 모든 요청 허용 + CSRF 비활성화.
	// 로그인/회원 기능 구현 시, 아래를 인증 규칙(formLogin, authorizeHttpRequests 등)으로 교체할 것.
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()) // REST API라 세션 기반 CSRF 토큰이 없으므로 끈다 (안 끄면 POST/DELETE가 403)
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // 모든 엔드포인트(API, Swagger UI 포함) 인증 없이 허용

		return http.build();
	}
}
