package com.algolist.backend;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SolutionMapper {
    // 소스코드 저장
    void insertSolution(SolutionDto solution);

    // 특정 문제의 풀이 목록 조회
    List<SolutionDto> getSolutionsByProblemId(@Param("problemId") Long problemId);

    // 특정 풀이 상세 조회
    SolutionDto getSolution(@Param("id") Long id);

    // 풀이 삭제
    void deleteSolution(@Param("id") Long id);
}
