package com.algolist.backend.problem.batch.common;

import com.algolist.backend.problem.dto.ProblemDto;

/**
 * [확장 이음새] "어떤 소스의 raw 데이터 한 건(T)"을 우리 도메인 객체 ProblemDto 로 변환하는 규약.
 *
 * 소스가 늘어날 때(코드포스 / 백준 / ...) 각 소스가 이 인터페이스를 구현하면 된다.
 * 추상 메서드가 parse 하나뿐인 함수형 인터페이스라, Step 의 processor 자리에 parser::parse 메서드 참조로 꽂힌다.
 *
 * 변환에 실패하면(필수 필드 누락 등) {@link ProblemParseException} 을 던진다.
 * → Step 의 skip 설정에 걸려 그 아이템은 "실패"로 집계되고 건너뛰어진다.
 *
 * @param <T> 소스의 raw 타입 (예: 허깅페이스 JSON 한 건이면 JsonNode)
 */
@FunctionalInterface
public interface ProblemParser<T> {
    ProblemDto parse(T raw);
}
