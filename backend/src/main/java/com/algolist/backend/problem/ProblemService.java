package com.algolist.backend.problem;

import java.util.List;

public interface ProblemService {
	ProblemDto addProblem(Long userId, ProblemDto problem);
	boolean deleteProblem(Long userId, Long problemId);
	List<ProblemDto> selectAll(Long userId);
}
