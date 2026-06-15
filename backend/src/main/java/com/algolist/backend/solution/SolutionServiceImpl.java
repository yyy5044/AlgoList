package com.algolist.backend.solution;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SolutionServiceImpl implements SolutionService {

	// 문제별로 저장할 수 있는 최대 풀이의 수
	private static final int MAX_SOLUTIONS_PER_USER_PROBLEM = 20;
	// 작성하는 알고리즘의 최대 글자 수 (VARCHAR(50))
	private static final int MAX_ALGORITHM_LENGTH = 50;
	// 코드의 최대 크기(60KB, 데이터베이스 자료형인 TEXT는 64KB가 제한)
	private static final int MAX_CODE_BYTES = 60 * 1024;
	// 허용하는 풀이 유형의 종류(잘못된 요청 방지)
	private static final Set<String> ALLOWED_TYPES = Set.of("FIRST", "RECAP", "OPT");
	// 허용되는 파일 확장자명의 종류(다른 확장자명인 파일이 들어오지 않도록 방지)
	private static final Map<String, List<String>> ALLOWED_FILE_EXTENSIONS = Map.of(
			"java", List.of("java", "txt"),
			"python", List.of("py", "txt"),
			"cpp", List.of("cpp", "cc", "cxx", "txt"));

	private final SolutionDao solutionDao;

	@Override
	// 모든 Solution 목록 가져오기
	public List<SolutionDto> selectSolutions(Long userId, Long userProblemId) {

		return solutionDao.selectSolutions(userId, userProblemId);
	}

	@Override
	// Solution 상세 정보 가져오기
	public SolutionDto selectSolution(Long userId, Long solutionId) {
		if (userId == null || solutionId == null) {
			return null;
		}

		return solutionDao.selectSolution(userId, solutionId);
	}

	@Override
	@Transactional
	// Solution 추가하기
	public boolean insertSolution(Long userId, SolutionDto solution) {
		validateSolution(solution);

		if (!existsUserProblem(userId, solution.getUserProblemId())) {
			return false;
		}

		// 현재 유저가 올린 풀이의 개수를 확인하고 20개 이상이면 제한(DoS 방지)
		if (solutionDao.countSolutions(userId, solution.getUserProblemId()) >= MAX_SOLUTIONS_PER_USER_PROBLEM) {
			throw new IllegalArgumentException("문제당 풀이 업로드는 최대 20개까지 가능합니다.");
		}

		int result = solutionDao.insertSolution(solution);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	// Solution 삭제하기
	public boolean deleteSolution(Long userId, Long solutionId) {
		if (userId == null || solutionId == null) {
			return false;
		}

		int result = solutionDao.deleteSolution(userId, solutionId);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	// 현재 로그인한 유저의 userProblemId가 맞는지 검증(다른 유저의 userProblemId를 가지고 요청을 보낼 수 있기 때문)
	private boolean existsUserProblem(Long userId, Long userProblemId) {
		return userId != null && userProblemId != null && solutionDao.countUserProblem(userId, userProblemId) == 1;
	}

	// sql에서 컬럼들 모두 NOT NULL로 선언되어 있으므로 값들이 모두 존재하는지 확인
	private void validateSolution(SolutionDto solution) {
		if (solution == null || solution.getUserProblemId() == null || !StringUtils.hasText(solution.getAlgorithm())
				|| !StringUtils.hasText(solution.getType()) || !StringUtils.hasText(solution.getLanguage())
				|| !StringUtils.hasText(solution.getFileName()) || !StringUtils.hasText(solution.getCode())) {
			// 하나라도 값이 없다면 예외 반환
			throw new IllegalArgumentException("풀이 정보를 모두 입력해주세요.");
		}

		// 앞, 뒤에 있는 공백은 제거
		solution.setAlgorithm(solution.getAlgorithm().trim());
		solution.setType(solution.getType().trim());
		// 터키어와 같은 특정 로케일에서 소문자 변환 시 에러 방지용(Locale.ROOT)
		solution.setLanguage(solution.getLanguage().trim().toLowerCase(Locale.ROOT));
		solution.setFileName(solution.getFileName().trim());

		// 알고리즘 길이가 허용된 길이 이내인지 확인
		validateTextLength(solution.getAlgorithm(), MAX_ALGORITHM_LENGTH, "주요 알고리즘");
		// 풀이 유형이 올바른 타입인지 확인(FIRST, RECAP, OPT)
		validateType(solution.getType());
		// 언어가 올바른 종류인지 확인(java, python, cpp)
		validateLanguage(solution.getLanguage());
		// 파일 확장자명을 확인해서 올바른 파일인지 확인
		validateFileExtension(solution);
		// 용량을 초과하지 않는 코드 파일인지 확인
		validateCodeLength(solution);
	}

	private void validateTextLength(String value, int maxLength, String fieldName) {
		if (value.codePointCount(0, value.length()) > maxLength) {
			throw new IllegalArgumentException(fieldName + "은(는) 최대 " + maxLength + "자까지 입력할 수 있습니다.");
		}
	}

	private void validateType(String type) {
		if (!ALLOWED_TYPES.contains(type)) {
			throw new IllegalArgumentException("지원하지 않는 풀이 유형입니다.");
		}
	}

	private void validateLanguage(String language) {
		if (!ALLOWED_FILE_EXTENSIONS.containsKey(language)) {
			throw new IllegalArgumentException("지원하지 않는 언어입니다.");
		}
	}

	// 파일 확장자명을 확인해서 잘못된 파일이 들어오지 않도록 제한
	private void validateFileExtension(SolutionDto solution) {
		List<String> allowedExtensions = ALLOWED_FILE_EXTENSIONS.get(solution.getLanguage());
		// 확장자명만 추출해서 allowedExtensions에 확장자명이 포함되는지 확인
		String extension = StringUtils.getFilenameExtension(solution.getFileName());
		if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("선택한 언어와 맞는 소스 코드 파일만 업로드할 수 있습니다.");
		}
	}

	private void validateCodeLength(SolutionDto solution) {
		int codeBytes = solution.getCode().getBytes(StandardCharsets.UTF_8).length;
		if (codeBytes > MAX_CODE_BYTES) {
			throw new IllegalArgumentException("소스 코드는 최대 60KB까지 업로드할 수 있습니다.");
		}
	}
}
