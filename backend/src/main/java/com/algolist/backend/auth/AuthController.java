package com.algolist.backend.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

	@GetMapping("/csrf")
	// POST/PUT/PATCH/DELETE 요청 시 csrf 토큰을 받기 위한 api/csrf 요청
	public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
		return ResponseEntity.status(HttpStatus.OK)
			.body(Map.of("headerName", csrfToken.getHeaderName(), "token", csrfToken.getToken()));
	}
	
	@GetMapping("/me")
	public ResponseEntity<?> me(Authentication authentication) {
		if(authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		String role = authentication.getAuthorities().stream()
			.findFirst()
			.map(authority -> authority.getAuthority().replace("ROLE_", ""))
			.orElse("");
		return ResponseEntity.status(HttpStatus.OK).body(Map.of("username", authentication.getName(), "role", role));
	}

}
