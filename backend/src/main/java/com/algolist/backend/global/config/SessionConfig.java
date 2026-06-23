package com.algolist.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
// 순환 참조 이슈를 피하기 위해 세션 관련 Bean은 SecurityConfig와 분리
public class SessionConfig {

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
}
