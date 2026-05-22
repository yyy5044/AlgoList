package com.algolist.backend.problem;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProblemDao {
	// 문제 생성: problems에 1행 INSERT. useGeneratedKeys로 생성된 problem_id를 problem.id에 채운다.
	int insertProblem(ProblemDto problem);

	// 문제의 카테고리들을 problem_categories에 N행 INSERT.
	int insertCategories(@Param("problemId") Long problemId,
	                     @Param("categories") List<String> categories);
	
}
