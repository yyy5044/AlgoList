package com.algolist.backend.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algolist.backend.user.dto.request.EmailVerificationConfirmRequestDto;
import com.algolist.backend.user.dto.request.EmailVerificationRequestDto;
import com.algolist.backend.user.service.EmailVerificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email-verifications")
public class EmailVerificationController {

	private final EmailVerificationService emailVerificationService;

	@PostMapping
	// 이메일 주소를 받은 후 인증 코드 발송을 위한 요청
	public ResponseEntity<Map<String, String>> sendVerificationCode(@RequestBody EmailVerificationRequestDto request) {
		emailVerificationService.sendVerificationCode(request.getEmail());
		return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "인증 코드가 발송되었습니다."));
	}

	@PostMapping("/confirm")
	// 이메일 주소와 코드 값을 받은 후 인증 여부를 확인하는 요청
	public ResponseEntity<Map<String, String>> confirmVerificationCode(@RequestBody EmailVerificationConfirmRequestDto request) {
		emailVerificationService.confirmVerificationCode(request.getEmail(), request.getCode());
		return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "이메일이 인증되었습니다."));
	}
}
