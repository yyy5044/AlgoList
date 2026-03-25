package com.algolist.backend;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProblemMapper {
    List<ProblemDto> getAllProblems(@Param("userId") Long userId);
    void insertProblem(ProblemDto problem);
    void deleteProblem(@Param("id") Long id, @Param("userId") Long userId);
    void insertCategory(@Param("problemId") Long problemId, @Param("categoryName") String categoryName);
    List<String> getCategoriesByProblemId(@Param("problemId") Long problemId);
    Long getNumberById(Long id); // 문제 번호 주기용
}