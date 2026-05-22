package com.algolist.backend.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration		// 이 빈을 설정 클래스야
@EnableWebSecurity 	// Spring Security 웹 보안 활성화
public class SecurityConfig {
	
	// 권한 부여(ADMIN은 USER의 상위 권한, USER는 GUEST의 상위 권한
    @Bean
    RoleHierarchy roleHierachy() {
            return RoleHierarchyImpl.withDefaultRolePrefix() // role의 기본 prefix 설정: ROLE_
                            .role("ADMIN").implies("USER").role("USER").implies("GUEST")
                            .build();
    }

	
	@Bean
	PasswordEncoder passwordEncoder() {
		// 기본 encoder로 bcrypt 방식
		// 비밀번호가 같은지 비교하려면 passwordEncoder.matches(a, b) 메서드를 이용할 것
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
	
	// UserDetailService를 구현하지 않았을 때 임시로 사용할 빈(계정 자동 생성)
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    UserDetailsService userDetailService(PasswordEncoder pe) {
    	UserDetails admin = User.builder().username("admin@ssafy.com").password(pe.encode("1234")).roles("ADMIN").build();
    	UserDetails user = User.builder().username("user@ssafy.com").password(pe.encode("1234")).roles("USER").build();
    	
    	return new InMemoryUserDetailsManager(admin, user);
    }
    
    // 로그인, 로그아웃을 위한 SecurityFilterChain
//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    	http.formLogin(login -> login.loginPage("/member/login-form")
//    			.loginProcessingUrl("/member/login")
//    			.usernameParameter("email")
//    			.failureForwardUrl("/member/login-form?error")
//    			.defaultSuccessUrl("/")
//    			);
//    	http.logout(logout -> logout.logoutUrl("/member/logout")
//    			.invalidateHttpSession(true)
//    			.logoutSuccessUrl("/") // logout
//    			);
//    	http.rememberMe(rememberMe -> rememberMe.tokenValiditySeconds(60 * 1));
//    	http.csrf(csrf -> csrf.disable());
//    	return http.build();
//    }
	
	
}
