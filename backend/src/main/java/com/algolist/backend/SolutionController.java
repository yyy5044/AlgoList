package com.algolist.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SolutionController {

    @Autowired
    private SolutionMapper solutionMapper;

    @Autowired
    private ProblemMapper problemMapper; // 문제 번호 가져오기용

    @PostMapping("/api/solutions")
    public SolutionDto addSolution(@RequestBody SolutionDto solution) {
        solutionMapper.insertSolution(solution);
        Long problemNumber = problemMapper.getNumberById(solution.getProblemId());
        solution.setProblemNumber(problemNumber);
        return solution;
    }

    // 특정 문제의 풀이 목록 조회
    @GetMapping("/api/solutions/{problemId}")
    public List<SolutionDto> getSolutions(@PathVariable Long problemId) {
        return solutionMapper.getSolutionsByProblemId(problemId);
    }

    // 특정 풀이 상세 조회
    @GetMapping("/api/solutions/detail/{id}")
    public SolutionDto getSolution(@PathVariable Long id) {
        return solutionMapper.getSolution(id);
    }

    // 풀이 삭제
    @DeleteMapping("/api/solutions/{id}")
    public void deleteSolution(@PathVariable Long id) {
        solutionMapper.deleteSolution(id);
    }
}