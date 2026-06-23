package com.algolist.backend.global.exception;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 핸들러.
 * 컨트롤러까지 전파된 예외를 일괄 처리하여 일관된 응답을 반환한다.
 *
 * TODO: 도메인별 커스텀 예외(예: ExternalApiException, ResourceNotFoundException)를
 *       정의하고 핸들러 메서드를 추가할 것.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 중복 아이디 → 409 */
	@ExceptionHandler(DuplicateUsernameException.class)
	public ResponseEntity<Map<String, String>> handleDuplicateUsernameException(DuplicateUsernameException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
	}

	/** 중복 이메일 → 409 */
	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<Map<String, String>> handleDuplicateEmailException(DuplicateEmailException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
	}

	/** 잘못된 요청 파라미터 → 400 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
	}

	/** 그 외 처리되지 않은 모든 예외 → 500 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception e) {
		log.error("처리되지 않은 예외 발생", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body("서버 내부 오류가 발생했습니다.");
	}
	
	/** DB단에서 발생한 중복 문제 삽입 예외 */
	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<String> handleDuplicateKeyException(DuplicateKeyException e){
		log.warn("중복 문제 삽입 예외");
		return ResponseEntity.status(HttpStatus.CONFLICT).body("중복 문제 삽입 요청");
	}
	
	/** 프론트에서 없는 자원에 요청 보낼 때 예외 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException e) {
	    log.warn("존재하지 않는 경로 요청: /{}", e.getResourcePath());
	    return ResponseEntity.status(HttpStatus.NOT_FOUND)
	            .body(Map.of("message", "존재하지 않는 경로입니다."));
	}
}
