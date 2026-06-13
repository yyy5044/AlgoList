package com.algolist.backend.solution;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.algolist.backend.problem.dto.UserProblemDto;

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

	// 풀이 업로드 성공 시 유저 문제의 최근 풀이 정보 갱신
	int updateUserProblemSolvedStatus(@Param("userId") Long userId, @Param("userProblemId") Long userProblemId,
			@Param("lastSolvedDate") LocalDate lastSolvedDate);

	// 갱신된 유저 문제 정보 조회
	UserProblemDto selectUserProblem(@Param("userId") Long userId, @Param("userProblemId") Long userProblemId);

	// Solution 삭제
	int deleteSolution(@Param("userId") Long userId, @Param("solutionId") Long solutionId);
}
