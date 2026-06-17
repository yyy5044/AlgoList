package com.algolist.backend.problem.translation;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TranslationDao {
	TranslatedProblemDto getTranslatedProblem(Long problemId);			 // 문제 조회
	int insertTranslatedProblem(TranslatedProblemDto translatedProblem); // 문제 추가
	OriginalProblemDto getOriginalProblem(Long problemId);				 // 원본 문제 조회
}
