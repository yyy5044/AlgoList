package com.algolist.backend.global.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	// handler에서 응답 시 body에 json 파일을 넣어주기 위한 ObjectMapper
	private final ObjectMapper objectMapper;
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// ⚠️ 개발용 설정: 모든 요청 허용 + CSRF 비활성화.
	// 로그인/회원 기능 구현 시, 아래를 인증 규칙(formLogin, authorizeHttpRequests 등)으로 교체할 것.
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()) // REST API라 세션 기반 CSRF 토큰이 없으므로 끈다 (안 끄면 POST/DELETE가 403)
			.authorizeHttpRequests(auth -> auth.requestMatchers("/api/login").permitAll() // 로그인 요청은 모두 가능
			.requestMatchers(HttpMethod.POST, "/api/users").permitAll() // POST 요청으로 오는 /api/users(회원가입) 요청은 모두 가능
			.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll() // swagger 관련 요청은 모두 허용
			.anyRequest().authenticated()) // 로그인을 제외한 나머지 요청들은 로그인해야 가능하도록 설정
			.exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> { // 인증되지 않은 요청이 들어올 시 가장 먼저 처리하는 지점
			response.setStatus(401); // UNAUTHORIZED(401) 응답 보내기
		}))
		.formLogin(login -> login.loginProcessingUrl("/api/login") // 로그인 요청은 /api/login 요청일 때
		.successHandler((request, response, authenticaiton) -> { // 로그인 성공 시 실행할 로직
			response.setStatus(200); // 성공 시 OK 설정
			response.setContentType("application/json;charset=UTF-8"); // json 응답을 보낼 것이므로 ContentType 설정
			objectMapper.writeValue(response.getWriter(), Map.of("username", authenticaiton.getName())); // objectMapper를 사용해 json에 username을 넣어준 상태로 응답함
		})
		.failureHandler((request, response, authentication) -> {
			response.setStatus(400); // 실패 시 BAD REQUEST 설정
		}))
		.logout(logout -> logout.logoutUrl("/api/logout") // 로그아웃 요청은 /api/logout일 때
			.invalidateHttpSession(true) // 세션 초기화
			.deleteCookies("JSESSIONID") // JSESSIONID 쿠키 삭제
			.logoutSuccessHandler((request, response, authentication) -> {
				response.setStatus(200); // 성공 시 OK 설정
		})
		); 

		return http.build();
	}
}
