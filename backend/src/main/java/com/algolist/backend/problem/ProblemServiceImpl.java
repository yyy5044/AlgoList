package com.algolist.backend.problem;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService{

	private final ProblemDao dao;

	@Override
	@Transactional
	public ProblemDto addProblem(Long userId, ProblemDto problem) {
		problem.setUserId(userId);

		// problems에 INSERT → useGeneratedKeys로 problem.id가 채워진다
		dao.insertProblem(problem);

		// 카테고리가 있으면 problem_categories에 N행 INSERT
		List<String> categories = problem.getCategory();
		if (categories != null && !categories.isEmpty()) {
			dao.insertCategories(problem.getId(), categories);
		}

		// id까지 채워진 problem을 그대로 반환 → 프론트가 리스트에 추가
		return problem;
	}

	@Override
	public boolean deleteProblem(Long userId, Long problemId) {
		int affectedRows = dao.deleteProblem(userId, problemId);
		boolean result = false;
		
		if (affectedRows > 0) result = true; 
		
		return result;
	}

	@Override
	public List<ProblemDto> selectAll(Long userId) {
		// TODO: dao.selectAll이 실패하면 예외 -> 예외 핸들러 필요
		return dao.selectAll(userId);
	}
	
}
