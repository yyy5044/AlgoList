package com.algolist.backend.problem;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProblemMapper {
	boolean insertProblem(int user, ProblemDto problem); 		// 문제 생성
	boolean deleteProblem(int user, int problemId);				// 문제 삭제
	List<ProblemDto> findAllByUserId(int userId);				// 유저의 전체 문제 조회
}
