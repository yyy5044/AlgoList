package com.algolist.backend.global.config;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.algolist.backend.auth.CustomUserDetails;
import com.algolist.backend.user.dto.response.UserSuspensionInfoDto;
import com.algolist.backend.user.service.UserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	// handler에서 응답 시 body에 json 파일을 넣어주기 위한 ObjectMapper
	private final ObjectMapper objectMapper;
	private final UserService userService;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
		http.csrf(csrf -> csrf.spa())
			.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/csrf").permitAll() // CSRF 토큰을 받는 요청은 모두 가능
			.requestMatchers(HttpMethod.POST, "/api/login").permitAll() // 로그인 요청은 모두 가능
			.requestMatchers(HttpMethod.POST, "/api/users").permitAll() // POST 요청으로 오는 /api/users(회원가입) 요청은 모두 가능
			.requestMatchers("/api/admin/**").hasRole("ADMIN") // 관리자 API는 ADMIN만 가능
			.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll() // swagger 관련 요청은 모두 허용
			.anyRequest().authenticated()) // 로그인을 제외한 나머지 요청들은 로그인해야 가능하도록 설정
			.sessionManagement(session -> session.maximumSessions(-1) // 동시 로그인 수는 제한을 두지 않음
				.sessionRegistry(sessionRegistry)
				.expiredSessionStrategy(event -> { // 만료된 세션일 때는 해당 에러 타입과 메시지를 보냄
					writeErrorResponse(event.getResponse(), HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다.");
				}))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) -> { // 인증되지 않은 요청이 들어올 시 가장 먼저 처리하는 지점
					writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."); // UNAUTHORIZED(401) 응답 보내기
				})
				.accessDeniedHandler((request, response, accessDeniedException) -> {
					if (accessDeniedException instanceof CsrfException) { // 유효하지 않은 CSRF 토큰이 들어왔을 때 예외 처리
						writeErrorResponse(response, HttpStatus.FORBIDDEN, "CSRF 토큰이 유효하지 않습니다.");
						return;
					}
					writeErrorResponse(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
				}))
		.formLogin(login -> login.loginProcessingUrl("/api/login") // 로그인 요청은 /api/login 요청일 때
		.successHandler((request, response, authenticaiton) -> { // 로그인 성공 시 실행할 로직
			if (authenticaiton.getPrincipal() instanceof CustomUserDetails userDetails
					&& "SUSPENDED".equals(userDetails.getUser().getAccountStatus())) { // 로그인했는데 SUSPENDED인 계정이면
				UserSuspensionInfoDto suspension = userService.selectActiveSuspension(userDetails.getUser().getUserId()); // 정지 정보를 찾아서 전달
				HttpSession session = request.getSession(false);
				if (session != null) {
					session.invalidate();
				}
				SecurityContextHolder.clearContext();

				Map<String, Object> body = new LinkedHashMap<>();
				body.put("message", "정지된 계정입니다.");
				body.put("reason", suspension != null && suspension.getReason() != null && !suspension.getReason().isBlank()
						? suspension.getReason()
						: "등록된 정지 사유가 없습니다.");
				body.put("suspendedUntilDate", getSuspendedUntilDate(suspension));

				response.setStatus(403);
				response.setContentType("application/json;charset=UTF-8");
				objectMapper.writeValue(response.getWriter(), body);
				return;
			}

			String role = authenticaiton.getAuthorities().stream()
				.findFirst()
				.map(authority -> authority.getAuthority().replace("ROLE_", ""))
				.orElse("");
			response.setStatus(200); // 성공 시 OK 설정
			response.setContentType("application/json;charset=UTF-8"); // json 응답을 보낼 것이므로 ContentType 설정
			objectMapper.writeValue(response.getWriter(), Map.of("username", authenticaiton.getName(), "role", role)); // objectMapper를 사용해 json에 username과 role을 넣어준 상태로 응답함
		})
		.failureHandler((request, response, authentication) -> {
			writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 틀렸습니다.");
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

	@Bean
	// 현재 로그인 세션 목록을 추적할 수 있는 객체
	SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	// 로그아웃이나 세션 만료 등의 이벤트를 전달하는 객체(세션이 만료되었을 때 레지스트리도 자동으로 정리)
	HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws java.io.IOException {
		response.setStatus(status.value());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getWriter(), Map.of("message", message));
	}

	// 정지 기간 포맷팅해서 전달하기
	private String getSuspendedUntilDate(UserSuspensionInfoDto suspension) {
		if (suspension == null || suspension.getSuspendedUntil() == null) {
			return "";
		}

		LocalDate suspendedUntilDate = suspension.getSuspendedUntil().toLocalDate().minusDays(1);
		return suspendedUntilDate.toString();
	}
}
