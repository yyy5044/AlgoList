package com.algolist.backend.problem.translation;

import java.util.ArrayList;
import java.util.List;
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

	private static final Pattern MATH_PATTERN = Pattern.compile("\\${2,3}.*?\\${2,3}", Pattern.DOTALL);

	private final String systemPrompt = """
	        당신은 번역 전문 AI입니다.
	        제목과 본문을 자연스러운 한국어로 번역하세요.
	        단, [MATH_0], [MATH_1] 처럼 대괄호로 감싼 토큰은 번역하거나 수정하지 말고 그대로 두세요.
	        위치만 한국어 어순에 맞게 자연스럽게 배치하면 됩니다.
	        """;
	
	public TranslationServiceImpl(TranslationDao tDao, ChatClient.Builder builder) {
		this.tDao = tDao;
		this.chatClient = builder
				.defaultSystem(systemPrompt)
				.build();
	}

	@Override
	public TranslatedProblemDto getTranslatedProblem(Long problemId) {
		// 1. DB에 있는지 확인
		TranslatedProblemDto result = tDao.getTranslatedProblem(problemId);
		
		// 2. 없으면 AI 번역
		if (result == null) {
			OriginalProblemDto origin = tDao.getOriginalProblem(problemId);
			
			if(origin == null) throw new UnknownProblemIdException("없는 문제이거나 번역이 불가능한 문제입니다.");

			// 수식($$$...$$$)을 [MATH_n] 플레이스홀더로 치환 (title/description이 store를 공유)
			List<String> mathStore = new ArrayList<>();
			String maskedTitle = maskMath(origin.getOriginalTitle(), mathStore);
			String maskedDescription = maskMath(origin.getOriginalDescription(), mathStore);

			String userPrompt = "제목: " + maskedTitle + " 본문: " + maskedDescription;
			log.info(">>> USER PROMPT:\n{}", userPrompt);

			result = chatClient.prompt()
					.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
					.user(userPrompt)
					.call()
					.entity(TranslatedProblemDto.class);

			log.info("<<< ASSISTANT: title={}, desc={}",
					result.getTranslatedTitle(),
					result.getTranslatedDescription());

			// 번역 결과의 [MATH_n]을 원본 수식으로 복원
			result.setTranslatedTitle(restoreMath(result.getTranslatedTitle(), mathStore));
			result.setTranslatedDescription(restoreMath(result.getTranslatedDescription(), mathStore));

			// 3. DB에 저장
			result.setProblemId(problemId);
			tDao.insertTranslatedProblem(result);
		}
		
		return result;
	}

	// $$$...$$$ 수식을 [MATH_n]으로 치환하고, 원본은 store에 순서대로 보관
	private String maskMath(String text, List<String> store) {
		Matcher matcher = MATH_PATTERN.matcher(text);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			store.add(matcher.group());                              // 원본 수식 보관
			matcher.appendReplacement(sb, "[MATH_" + (store.size() - 1) + "]");
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	// [MATH_n]을 store에 보관한 원본 수식으로 되돌림 (replace는 정규식이 아닌 리터럴 치환)
	private String restoreMath(String text, List<String> store) {
		for (int i = 0; i < store.size(); i++) {
			text = text.replace("[MATH_" + i + "]", store.get(i));
		}
		return text;
	}
}
