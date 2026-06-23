package com.algolist.backend.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.algolist.backend.user.dto.response.UserSuspensionHistoryPageResponseDto;
import com.algolist.backend.user.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/user-suspensions")
public class AdminUserSuspensionController {

	private final AdminUserService adminUserService;

	@GetMapping
	// 전체 정지 이력 목록 조회 요청(페이징)
	public ResponseEntity<UserSuspensionHistoryPageResponseDto> selectUserSuspensions(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String searchType,
			@RequestParam(required = false) String keyword) {
		UserSuspensionHistoryPageResponseDto suspensions =
				adminUserService.selectUserSuspensions(page, size, status, searchType, keyword);

		return ResponseEntity.status(HttpStatus.OK).body(suspensions);
	}
}
