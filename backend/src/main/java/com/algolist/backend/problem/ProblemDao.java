package com.algolist.backend.problem;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProblemDao {
	// 초기에 유저 문제 전체 조회
	List<UserProblemDto> selectAllByUserId(Long userId);
	
	// 문제 검색
	List<ProblemDto> searchProblem(String query);
	
	// 문제 삽입
	int insertUserProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);
	
	// 단일 행 조회 (문제 삽입 후 기본값 채워진 객체를 보내주기 위한 용도)
	UserProblemDto selectOne(@Param("userId") Long userId, @Param("problemId") Long problemId);
	
	// 문제 삭제
	int deleteUserProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);
}
