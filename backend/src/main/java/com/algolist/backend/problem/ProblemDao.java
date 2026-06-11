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
	
	// 유저 문제 추가
	int insertUserProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);
	
	// 단일 행 조회 (문제 삽입 후 기본값 채워진 객체를 보내주기 위한 용도)
	UserProblemDto selectOne(@Param("userId") Long userId, @Param("problemId") Long problemId);
	
	// 문제 삭제
	int deleteUserProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);

	// 문제 추가
	int insertProblem(ProblemDto problem);

	// 문제 카테고리 추가 (문제 추가할 때 반드시 같이 실행되어야 함.)
	int insertCategory(@Param("problemId") Long problemId, @Param("categoryName") String categoryName);
	
	// 한 페이지 분량의 문제 가져오기
	List<ProblemDto> selectPage(@Param("site") String site, @Param("offset") int offset, @Param("size") int size);
	
	// 문제 본문 가져오기
	String selectDescription(Long problemId);
}
