package com.algolist.backend.problem.translation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.algolist.backend.problem.exception.UnknownProblemIdException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TranslationServiceImpl implements TranslationService {
	private final TranslationDao tDao;
	private final ChatClient chatClient;
	private final TranslationValidator validator;

	/**
	 * problemId별 번역 락. 같은 문제에 동시 요청이 와도 LLM 번역을 한 번만 수행하도록 직렬화한다.
	 * 단일 JVM 한정(다중 인스턴스에선 멱등 insert가 안전망). 같은 키엔 항상 같은 락 객체가 나온다.
	 */
	private final ConcurrentHashMap<Long, Object> translationLocks = new ConcurrentHashMap<>();

	/** 자기수정 루프 최대 시도 횟수(첫 호출 포함). */
	private static final int MAX_ATTEMPTS = 3;

	/** $$$...$$$ 수식 토큰. */
	private static final Pattern MATH_PATTERN = Pattern.compile("\\${2,3}.*?\\${2,3}", Pattern.DOTALL);

	/** buildDescription이 생성하는 섹션 헤더("## Xxx" 한 줄). 순서는 원본을 그대로 따른다. */
	private static final Pattern SECTION_PATTERN =
			Pattern.compile("(?m)^## (Description|Input|Output|Examples|Note)[ \\t]*\\R");

	/** 번역 대상이 아닌(원문 그대로 보관할) 섹션 이름. */
	private static final String EXAMPLES = "Examples";

	private final String systemPrompt = """
	        당신은 알고리즘 문제를 한국어로 번역하는 전문 번역가입니다.
	        제목과 여러 섹션(Description, Input, Output, Note)이 라벨과 함께 주어집니다.
	        각 섹션을 자연스러운 한국어로 번역하여 응답의 대응되는 필드에 채워 넣으세요.

	        규칙:
	        - 주어진 모든 섹션을 빠짐없이 번역하세요. 어떤 섹션도 비우거나 생략하지 마세요.
	        - [MATH_0], [MATH_1] 처럼 대괄호로 감싼 토큰은 번역/수정/삭제하지 말고 그대로 두세요.
	          위치만 한국어 어순에 맞게 자연스럽게 옮기면 됩니다.
	        - 주어지지 않은 섹션의 필드는 빈 문자열로 두세요.
	        - 제약 조건(Input 범위 등)에서 지수가 깨져 일반 숫자로 표기된 부분을, 문맥에 맞게
	          올바른 지수 표기로 교정하세요. 교정 시 반드시 LaTeX 형식 $...$ 로 감싸세요.
	           - 예: 0 ≤ X, Y ≤ 106  →  0 ≤ X, Y ≤ $10^6$
	           - 예: 1 ≤ N ≤ 109     →  1 ≤ N ≤ $10^9$
	        - 단, 문맥상 단순 수치나 개수(예: "106번 정점", "정답이 106인 경우")가 확실하면 원본 숫자를
	          그대로 두세요. '범위/제약'임이 분명할 때만 교정합니다.
	        - 지수 표기는 마크다운 캐럿(10^6)이 아니라 반드시 $...$ 형식만 사용하세요.
	        """;

	public TranslationServiceImpl(TranslationDao tDao, ChatClient.Builder builder, TranslationValidator validator) {
		this.tDao = tDao;
		this.validator = validator;
		this.chatClient = builder
				.defaultSystem(systemPrompt)
				.build();
	}
	
	@Override
	public TranslatedProblemDto getTranslatedProblem(Long problemId) {
		// 1. 락 밖 캐시 확인 — 이미 번역된 문제(대부분의 요청)는 락 없이 바로 반환한다.
		TranslatedProblemDto cached = tDao.getTranslatedProblem(problemId);
		if (cached != null) {
			return cached;
		}

		// 2. problemId 전용 락 — 같은 문제의 동시 번역을 한 스레드로 직렬화한다.
		Object lock = translationLocks.computeIfAbsent(problemId, k -> new Object());
		synchronized (lock) {
			// 3. 더블체크 — 락을 기다리는 동안 다른 스레드가 이미 번역해 저장했을 수 있다.
			cached = tDao.getTranslatedProblem(problemId);
			if (cached != null) {
				return cached;
			}

			// 4. 정말 첫 번역인 경우에만 실제 번역을 수행한다.
			return translateAndStore(problemId);
		}
	}

	/** 캐시에 없는 문제를 실제로 번역해 저장한다. 반드시 번역 락 안에서 호출한다. */
	private TranslatedProblemDto translateAndStore(Long problemId) {
		// 원본 조회
		OriginalProblemDto origin = tDao.getOriginalProblem(problemId);
		if (origin == null) {
			throw new UnknownProblemIdException("없는 문제이거나 번역이 불가능한 문제입니다.");
		}

		// 섹션 분해 (원본 순서 보존). Examples는 번역 대상에서 제외하고 원문 그대로 보관한다.
		List<Section> sections = splitSections(origin.getOriginalDescription());

		// 수식 마스킹 (title + 번역 대상 섹션이 store 하나를 공유 → [MATH_n] 인덱스 전역 유일)
		List<String> mathStore = new ArrayList<>();
		String maskedTitle = maskMath(origin.getOriginalTitle(), mathStore);
		Map<String, String> maskedSections = new LinkedHashMap<>();
		for (Section s : sections) {
			if (s.translatable()) {
				maskedSections.put(s.name(), maskMath(s.body(), mathStore));
			}
		}

		// 번역 + 자기수정 루프 (이상: placeholder 유실 / 섹션 필드 비어있음)
		String userPrompt = buildUserPrompt(maskedTitle, maskedSections);
		SectionedTranslationResponse resp = translateWithRetry(userPrompt, maskedSections.keySet(), mathStore.size(), problemId);

		// 최종 실패 → 캐시하지 않고 원문 그대로 반환(조용한 깨진 저장 방지, 다음 요청에서 재시도)
		if (resp == null) {
			log.error("번역 자기수정 {}회 실패 → 원문 반환(미저장) problemId={}", MAX_ATTEMPTS, problemId);
			return fallbackToOriginal(problemId, origin);
		}

		// 수식 복원 + 원본 순서대로 재조립
		String translatedTitle = restoreMath(resp.getTitle(), mathStore);
		String translatedDescription = reassemble(sections, resp, mathStore);

		// 저장 후 반환
		TranslatedProblemDto result = new TranslatedProblemDto();
		result.setProblemId(problemId);
		result.setTranslatedTitle(translatedTitle);
		result.setTranslatedDescription(translatedDescription);
		tDao.insertTranslatedProblem(result);
		return result;
	}

	/** 번역 호출 후 이상 응답이면 재시도. 모든 시도 실패 시 null 반환. */
	private SectionedTranslationResponse translateWithRetry(
			String userPrompt, Collection<String> presentSections, int placeholderCount, Long problemId) {

		log.debug(">>> 번역 요청 problemId={}\n[USER PROMPT]\n{}", problemId, userPrompt);

		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			if (attempt > 1) {
				log.info("[Self-Correction] problemId={} 재시도 {}/{}", problemId, attempt, MAX_ATTEMPTS);
			}

			SectionedTranslationResponse resp = chatClient.prompt()
					.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
					.user(userPrompt)
					.call()
					.entity(SectionedTranslationResponse.class);

			log.debug("<<< 번역 응답 problemId={} (시도 {}/{})\n[title] {}\n[description] {}\n[input] {}\n[output] {}\n[note] {}",
					problemId, attempt, MAX_ATTEMPTS,
					resp.getTitle(), resp.getDescription(), resp.getInput(), resp.getOutput(), resp.getNote());

			String anomaly = validator.detect(resp, presentSections, placeholderCount);
			if (anomaly == null) {
				if (attempt > 1) {
					log.info("[Self-Correction] problemId={} 재시도 {}회 만에 정상 응답", problemId, attempt);
				}
				return resp;
			}
			log.warn("[이상 감지] problemId={} (시도 {}/{}): {}", problemId, attempt, MAX_ATTEMPTS, anomaly);
		}
		return null;
	}

	/** 원본 description을 헤더 기준으로 분해한다(원본 순서 유지). */
	private List<Section> splitSections(String description) {
		List<Section> sections = new ArrayList<>();
		if (description == null || description.isBlank()) {
			return sections;
		}

		Matcher m = SECTION_PATTERN.matcher(description);
		String currentName = null;
		int bodyStart = -1;
		while (m.find()) {
			if (currentName != null) {
				sections.add(makeSection(currentName, description.substring(bodyStart, m.start())));
			}
			currentName = m.group(1);
			bodyStart = m.end();
		}
		if (currentName != null) {
			sections.add(makeSection(currentName, description.substring(bodyStart)));
		}
		return sections;
	}

	private Section makeSection(String name, String body) {
		return new Section(name, body.strip(), !EXAMPLES.equals(name));
	}

	/** 마스킹된 제목/섹션을 라벨과 함께 프롬프트로 만든다. Examples는 보내지 않는다. */
	private String buildUserPrompt(String maskedTitle, Map<String, String> maskedSections) {
		StringBuilder sb = new StringBuilder();
		sb.append("[Title]\n").append(maskedTitle).append("\n\n");
		for (Map.Entry<String, String> e : maskedSections.entrySet()) {
			sb.append('[').append(e.getKey()).append("]\n").append(e.getValue()).append("\n\n");
		}
		return sb.toString().strip();
	}

	/** 번역 결과(수식 복원)와 원문 Examples를 원본 순서대로 다시 합친다. */
	private String reassemble(List<Section> sections, SectionedTranslationResponse resp, List<String> mathStore) {
		List<String> blocks = new ArrayList<>();
		for (Section s : sections) {
			String body = s.translatable()
					? restoreMath(resp.section(s.name()), mathStore)   // 번역본
					: s.body();                                        // Examples: 원문 그대로
			blocks.add("## " + s.name() + "\n\n" + body);
		}
		return String.join("\n\n", blocks);
	}

	private TranslatedProblemDto fallbackToOriginal(Long problemId, OriginalProblemDto origin) {
		TranslatedProblemDto result = new TranslatedProblemDto();
		result.setProblemId(problemId);
		result.setTranslatedTitle(origin.getOriginalTitle());
		result.setTranslatedDescription(origin.getOriginalDescription());
		return result;
	}

	// $$$...$$$ 수식을 [MATH_n]으로 치환하고, 원본은 store에 순서대로 보관
	private String maskMath(String text, List<String> store) {
		if (text == null) return "";
		Matcher matcher = MATH_PATTERN.matcher(text);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			store.add(matcher.group());
			matcher.appendReplacement(sb, Matcher.quoteReplacement("[MATH_" + (store.size() - 1) + "]"));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	// [MATH_n]을 store에 보관한 원본 수식으로 되돌림 (replace는 정규식이 아닌 리터럴 치환)
	private String restoreMath(String text, List<String> store) {
		if (text == null) return "";
		for (int i = 0; i < store.size(); i++) {
			text = text.replace("[MATH_" + i + "]", store.get(i));
		}
		return text;
	}

	/** 분해된 섹션 한 조각. translatable=false면 원문 그대로 재조립(Examples). */
	private record Section(String name, String body, boolean translatable) {}
}
