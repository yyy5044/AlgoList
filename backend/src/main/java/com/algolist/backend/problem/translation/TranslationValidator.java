package com.algolist.backend.problem.translation;

import java.util.Collection;

import org.springframework.stereotype.Component;

/**
 * LLM 번역 응답의 이상 여부를 판정한다.
 * 정상이면 null, 이상이면 사유 문자열을 반환한다(자기수정 루프가 로그/재시도에 사용).
 *
 * 현재 감지 항목:
 *  1) placeholder 유실 — [MATH_0..n-1] 중 응답에서 사라진 토큰
 *  2) 빈 필드     — 요청에 포함된 섹션(또는 제목)의 번역 결과가 비어있음
 * (언어 비율/길이 축소/메타 텍스트 등은 추후 여기에 추가한다.)
 */
@Component
public class TranslationValidator {

	/** 정상이면 null, 이상이면 사유. */
	public String detect(SectionedTranslationResponse resp, Collection<String> presentSections, int placeholderCount) {
		if (resp == null) {
			return "응답 없음";
		}

		String emptyField = detectEmptyField(resp, presentSections);
		if (emptyField != null) {
			return emptyField;
		}

		return detectMissingPlaceholder(resp, presentSections, placeholderCount);
	}

	/** 2) 빈 필드: 제목 또는 요청에 포함된 섹션의 번역 결과가 비어있음. */
	private String detectEmptyField(SectionedTranslationResponse resp, Collection<String> presentSections) {
		if (isBlank(resp.getTitle())) {
			return "제목 필드 비어있음";
		}
		for (String name : presentSections) {
			if (isBlank(resp.section(name))) {
				return name + " 섹션 비어있음";
			}
		}
		return null;
	}

	/** 1) placeholder 유실: 전체 응답 텍스트에서 [MATH_i]가 사라진 게 있는지. */
	private String detectMissingPlaceholder(SectionedTranslationResponse resp, Collection<String> presentSections, int placeholderCount) {
		StringBuilder combined = new StringBuilder(orEmpty(resp.getTitle()));
		for (String name : presentSections) {
			combined.append('\n').append(orEmpty(resp.section(name)));
		}
		String all = combined.toString();
		for (int i = 0; i < placeholderCount; i++) {
			if (!all.contains("[MATH_" + i + "]")) {
				return "placeholder 유실 [MATH_" + i + "]";
			}
		}
		return null;
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static String orEmpty(String s) {
		return s == null ? "" : s;
	}
}
