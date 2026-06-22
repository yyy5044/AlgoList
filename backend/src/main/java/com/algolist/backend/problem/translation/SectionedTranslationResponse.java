package com.algolist.backend.problem.translation;

import lombok.Getter;
import lombok.Setter;

/**
 * LLM structured output 수신용 DTO.
 * 원본 description을 섹션 단위로 분해해 번역을 요청하고, 섹션별 결과를 각 필드로 받는다.
 * 주어지지 않은 섹션은 빈 문자열로 돌아온다(요청 시점에 존재하던 섹션만 검증한다).
 * Examples는 번역 대상이 아니므로 여기에 없다.
 */
@Getter
@Setter
public class SectionedTranslationResponse {
	private String title;
	private String description;
	private String input;
	private String output;
	private String note;
}
