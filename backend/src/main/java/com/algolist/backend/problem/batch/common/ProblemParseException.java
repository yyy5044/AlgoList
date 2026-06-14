package com.algolist.backend.problem.batch.common;

/**
 * 한 건의 raw 데이터를 ProblemDto 로 변환하지 못했을 때 던지는 예외.
 *
 * Step 을 faultTolerant + skip(ProblemParseException) 으로 구성하면, 이 예외가 난 아이템은
 * "실패"로 집계(skipCount)되고 건너뛴 뒤 작업이 계속된다. (필드 누락 + 예기치 못한 파싱 오류 모두 이걸로 통일)
 */
public class ProblemParseException extends RuntimeException {

    public ProblemParseException(String message) {
        super(message);
    }

    public ProblemParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
