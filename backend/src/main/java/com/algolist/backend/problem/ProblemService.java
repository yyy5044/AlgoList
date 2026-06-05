package com.algolist.backend.problem;

import java.util.List;

public interface ProblemService {
	List<UserProblemDto> selectAllByUserId(Long userId); // 초기에 1회 유저 문제 가져오기
	List<ProblemDto> searchProblem(String query); // 문제 검색
	UserProblemDto insertUserProblem(Long userId, Long problemId); // 문제 선택 -> user_problems 테이블에 추가
	int deleteUserProblem(Long userId, Long problemId);
}
