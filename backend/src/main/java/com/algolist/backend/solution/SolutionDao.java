package com.algolist.backend.solution;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SolutionDao {

	// userId, problemId 둘 다 일치하는 Solution들 가져오기
	List<SolutionDto> selectSolutions(@Param("userId") Long userId, @Param("userProblemId") Long userProblemId);

	// userId, solutionId 둘 다 일치하는 Solution 정보 가져오기
	SolutionDto selectSolution(@Param("userId") Long userId, @Param("solutionId") Long solutionId);

	// UserProblemId가 현재 로그인한 유저의 UserProblem인지 확인(소유권 확인용)
	int countUserProblem(@Param("userId") Long userId, @Param("userProblemId") Long userProblemId);

	// 현재 선택된 문제의 풀이 개수 확인
	int countSolutions(@Param("userId") Long userId, @Param("userProblemId") Long userProblemId);

	// Solution 추가
	int insertSolution(SolutionDto solution);

	// Solution 삭제
	int deleteSolution(@Param("userId") Long userId, @Param("solutionId") Long solutionId);
}
