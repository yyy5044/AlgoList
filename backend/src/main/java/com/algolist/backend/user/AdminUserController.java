package com.algolist.backend.user;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algolist.backend.auth.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final UserService userService;

	@GetMapping
	// 전체 회원 목록 조회 요청(페이징)
	public ResponseEntity<UserPageResponseDto> selectAllUsers(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String searchType,
			@RequestParam(required = false) String keyword) {
		UserPageResponseDto users = userService.selectUsers(page, size, status, searchType, keyword);

		return ResponseEntity.status(HttpStatus.OK).body(users);
	}

	@GetMapping("/{username}")
	// 특정 유저 상세정보 조회 요청
	public ResponseEntity<UserDetailDto> selectUser(@PathVariable String username) {
		UserDetailDto user = userService.selectUser(username);

		if (user != null) {
			return ResponseEntity.status(HttpStatus.OK).body(user);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 조회된 정보가 없으므로 NOT_FOUND(404) 에러
		}
	}

	@DeleteMapping("/{username}")
	// 유저 삭제 요청
	public ResponseEntity<?> deleteUser(@PathVariable String username) {
		boolean success = userService.deleteUser(username);

		if (success) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 삭제 완료했으므로 NO_CONTENT(204) 반환
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 조회된 정보가 없으므로 NOT_FOUND(404) 에러
		}
	}

	@PostMapping("/{username}/suspensions")
	// 유저 정지 요청
	public ResponseEntity<?> suspendUser(@PathVariable String username, @RequestBody SuspendUserRequestDto request,
			Authentication authentication) {
		boolean success;
		try {
			success = userService.suspendUser(username, request, getCurrentUserId(authentication));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
		}

		if (success) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PatchMapping("/{username}/suspensions/release")
	// 유저 정지 해제 요청
	public ResponseEntity<?> releaseUserSuspension(@PathVariable String username,
			@RequestBody ReleaseSuspensionRequestDto request, Authentication authentication) {
		boolean success;
		try {
			success = userService.releaseUserSuspension(username, request, getCurrentUserId(authentication));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
		}

		if (success) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	private Long getCurrentUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
			return userDetails.getUser().getUserId();
		}

		return null;
	}

}
