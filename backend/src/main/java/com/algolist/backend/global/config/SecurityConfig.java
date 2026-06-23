package com.algolist.backend.global.config;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;

import com.algolist.backend.auth.CustomUserDetails;
import com.algolist.backend.user.dto.response.UserSuspensionInfoDto;
import com.algolist.backend.user.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final ObjectMapper objectMapper;
	private final UserService userService;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
		http.csrf(csrf -> csrf.spa())
			.headers(headers -> headers.contentTypeOptions(contentTypeOptions -> {
			}))
			.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
			.requestMatchers(HttpMethod.POST, "/api/login").permitAll()
			.requestMatchers(HttpMethod.POST, "/api/users").permitAll()
			.requestMatchers(HttpMethod.POST, "/api/email-verifications", "/api/email-verifications/confirm").permitAll()
			.requestMatchers("/api/admin/**").hasRole("ADMIN")
			.anyRequest().authenticated())
			.sessionManagement(session -> session.maximumSessions(-1)
				.sessionRegistry(sessionRegistry)
				.expiredSessionStrategy(event -> {
					writeErrorResponse(event.getResponse(), HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다.");
				}))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) -> {
					writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
				})
				.accessDeniedHandler((request, response, accessDeniedException) -> {
					if (accessDeniedException instanceof CsrfException) {
						writeErrorResponse(response, HttpStatus.FORBIDDEN, "CSRF 토큰이 유효하지 않습니다.");
						return;
					}
					writeErrorResponse(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
				}))
			.formLogin(login -> login.loginProcessingUrl("/api/login")
				.successHandler((request, response, authentication) -> {
					if (authentication.getPrincipal() instanceof CustomUserDetails userDetails
							&& "SUSPENDED".equals(userDetails.getUser().getAccountStatus())) {
						UserSuspensionInfoDto suspension =
								userService.selectActiveSuspension(userDetails.getUser().getUserId());
						HttpSession session = request.getSession(false);
						if (session != null) {
							session.invalidate();
						}
						SecurityContextHolder.clearContext();

						Map<String, Object> body = new LinkedHashMap<>();
						body.put("message", "정지된 계정입니다.");
						body.put("reason", suspension != null && suspension.getReason() != null
								&& !suspension.getReason().isBlank()
								? suspension.getReason()
								: "등록된 정지 사유가 없습니다.");
						body.put("suspendedUntilDate", getSuspendedUntilDate(suspension));

						response.setStatus(403);
						response.setContentType("application/json;charset=UTF-8");
						objectMapper.writeValue(response.getWriter(), body);
						return;
					}

					String role = authentication.getAuthorities().stream()
						.findFirst()
						.map(authority -> authority.getAuthority().replace("ROLE_", ""))
						.orElse("");
					response.setStatus(200);
					response.setContentType("application/json;charset=UTF-8");
					objectMapper.writeValue(response.getWriter(),
							Map.of("username", authentication.getName(), "role", role));
				})
				.failureHandler((request, response, authentication) -> {
					writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 틀렸습니다.");
				}))
			.logout(logout -> logout.logoutUrl("/api/logout")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
				.logoutSuccessHandler((request, response, authentication) -> {
					response.setStatus(200);
				}));

		return http.build();
	}

	private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws java.io.IOException {
		response.setStatus(status.value());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getWriter(), Map.of("message", message));
	}

	private String getSuspendedUntilDate(UserSuspensionInfoDto suspension) {
		if (suspension == null || suspension.getSuspendedUntil() == null) {
			return "";
		}

		LocalDate suspendedUntilDate = suspension.getSuspendedUntil().toLocalDate().minusDays(1);
		return suspendedUntilDate.toString();
	}
}
