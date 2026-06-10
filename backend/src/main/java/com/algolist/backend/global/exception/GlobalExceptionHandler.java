package com.algolist.backend.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    /** 그 외 처리되지 않은 모든 예외 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("서버 내부 오류가 발생했습니다.");
    }
}
