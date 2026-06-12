package com.algolist.backend.problem;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algolist.backend.problem.dto.ProblemDto;
import com.algolist.backend.problem.dto.UserProblemDto;
import com.algolist.backend.problem.github.GitHubProblemCollector;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

	private final ProblemDao dao;
	private final GitHubProblemCollector gitHub;
	private final ProblemWriter writer;

	@Override
	public List<UserProblemDto> selectAllByUserId(Long userId) {
		return dao.selectAllByUserId(userId);
	}

	/**
	 * 문제 검색: DB 우선 → DB에 없으면 GitHub에서 실시간 수집 후 저장
	 *
	 * @Transactional 없음 — GitHub API 호출 중 DB 커넥션 점유를 방지하기 위해
	 * DB 저장은 ProblemWriter.save()에서 문제 단위로 트랜잭션 처리
	 */
	@Override
	public List<ProblemDto> searchProblem(String query) {
		// 1. DB 검색
		List<ProblemDto> results = dao.searchProblem(query);

		// 2. DB에 있으면 바로 반환
		if (results != null && !results.isEmpty()) {
			return results;
		}

		// 3. GitHub 검색
		List<ProblemDto> problems = gitHub.searchProblems(query, 5);
		if (problems == null || problems.isEmpty()) { 
			return List.of(); // 결과 없으면 빈 리스트 반환
		}

		// 4. 문제 단위로 저장 (각 save()가 개별 트랜잭션)
		for (ProblemDto dto : problems) {
			writer.save(dto);
		}

		// 5. 저장 후 다시 DB 검색하여 반환 (중복 문제를 거르고 전달하기 위해서)
		return dao.searchProblem(query);
	}

	@Override
	@Transactional
	public UserProblemDto insertUserProblem(Long userId, Long problemId) {
		UserProblemDto temp = dao.selectOne(userId, problemId); // 먼저 있는지 조회
		
		if (temp != null) {
			throw new IllegalArgumentException("이미 등록된 문제입니다.");
		}
		
		dao.insertUserProblem(userId, problemId);
		return dao.selectOne(userId, problemId);
	}

	@Override
	public int deleteUserProblem(Long userId, Long problemId) {
		return dao.deleteUserProblem(userId, problemId);
	}

	@Override
	public List<ProblemDto> selectPage(String site, int page, int size) {
		int offset = (page - 1) * size;
		return dao.selectPage(site, offset, size);
	}

	@Override
	public String selectDescription(Long problemId) {
		return dao.selectDescription(problemId);
	}
}
