package com.algolist.backend.user;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	@PostMapping
	// 회원가입 요청
	public ResponseEntity<?> regist(@RequestBody CreateRequestDto request) {
		boolean success = userService.insertUser(request.getUsername(), request.getPassword());

		if (success) {
			return ResponseEntity.status(HttpStatus.CREATED).build(); // 생성 성공 시 CREATED(201) 반환
		} else {
			return ResponseEntity.status(HttpStatus.CONFLICT).build(); // username 중복 등 오류 발생 시 CONFLICT(409) 에러
		}
	}

	@GetMapping("/{username}")
	// 특정 유저 상세정보 조회 요청
	public ResponseEntity<UserDetailDto> selectUser(@PathVariable String username, Authentication authentication) {
		if (!isCurrentUser(username, authentication)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		UserDetailDto user = userService.selectUser(username);

		if (user != null) {
			return ResponseEntity.status(HttpStatus.OK).body(user);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 조회된 정보가 없으므로 NOT_FOUND(404) 에러
		}
	}

	@PutMapping(value = "/{username}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	// 유저 프로필 수정 요청
	public ResponseEntity<?> updateUser(@PathVariable String username, @ModelAttribute UpdateRequestDto request, Authentication authentication) {
		if (!isCurrentUser(username, authentication)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		boolean success;
		try {
			success = userService.updateUser(username, request);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
		}

		if (success) {
			return ResponseEntity.status(HttpStatus.OK).build();
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 조회된 정보가 없으므로 NOT_FOUND(404) 에러
		}
	}
	
	@DeleteMapping("/{username}")
	// 유저 삭제 요청
	public ResponseEntity<?> deleteUser(@PathVariable String username, Authentication authentication) {
		if (!isCurrentUser(username, authentication)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		boolean success = userService.deleteUser(username);
		
		if (success) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 삭제 완료했으므로 NO_CONTENT(204) 반환
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 조회된 정보가 없으므로 NOT_FOUND(404) 에러
		}
	}

	private boolean isCurrentUser(String username, Authentication authentication) {
		return authentication != null && username.equals(authentication.getName());
	}

}
