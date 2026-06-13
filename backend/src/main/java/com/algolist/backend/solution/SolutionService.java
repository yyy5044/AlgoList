package com.algolist.backend.solution;

import java.util.List;

import com.algolist.backend.problem.dto.UserProblemDto;

public interface SolutionService {
	// 모든 Solution 목록 가져오기
	public List<SolutionDto> selectSolutions(Long userId, Long userProblemId);

	// Solution 상세 정보 가져오기
	public SolutionDto selectSolution(Long userId, Long solutionId);

	// Solution 추가하기
	public UserProblemDto insertSolution(Long userId, SolutionDto solution);

	// Solution 삭제하기
	public boolean deleteSolution(Long userId, Long solutionId);
}
