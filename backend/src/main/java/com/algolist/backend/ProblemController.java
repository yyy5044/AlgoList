package com.algolist.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class ProblemController {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Autowired
    private ProblemMapper problemMapper;

    // 검색 모달에서 문제 검색
    @GetMapping("/api/search")
    public List<ProblemDto> searchProblem(@RequestParam String query) {
        List<ProblemDto> results = new ArrayList<>();

        try {
            String url = "https://solved.ac/api/v3/search/problem?query=" + query;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();
            
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

                for (Map<String, Object> item : items) {
                    // 알고리즘 태그 추출
                    List<String> categories = new ArrayList<>(); // 알고리즘 태그를 담을 리스트
                    List<Map<String, Object>> tags = (List<Map<String, Object>>) item.get("tags"); // tags를 키로 하는 값들만 따로 가져오기
                    if (tags != null) {
                        for (Map<String, Object> tag : tags) {
                            List<Map<String, Object>> displayNames = (List<Map<String, Object>>) tag.get("displayNames");
                            for (Map<String, Object> name : displayNames) {
                                if ("ko".equals(name.get("language"))) { // 언어가 한국어일 때만
                                    categories.add((String) name.get("name")); // 알고리즘 분류 리스트에 태그 이름 넣기
                                }
                            }
                        }
                    }

                    ProblemDto problem = new ProblemDto(
                        (String) item.get("titleKo"),
                        String.valueOf(item.get("problemId")),
                        convertLevel((int) item.get("level")),
                        "BOJ",
                        "https://www.acmicpc.net/problem/" + item.get("problemId"), // 링크는 따로 제공이 안 되어서 백준 링크 규칙으로 만들어주기 
                        categories.isEmpty() ? List.of("미분류") : categories // 카테고리 비어있으면 미분류로 채우기
                    );

                    results.add(problem);
                }
            }
        } catch (Exception e) {
            System.out.println("검색 실패: " + e.getMessage());
        }

//        // solved.ac에서 온 응답 출력 코드
//        System.out.println("검색 결과 수: " + results.size());
//        for (ProblemDto p : results) {
//            System.out.println(p.getNumber() + " | " + p.getTitle() + " | " + p.getDifficulty());
//        }
        
        return results;
    }
    
    // 사용자의 문제 목록 조회
    @GetMapping("/api/problems")
    public List<ProblemDto> getProblems(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        List<ProblemDto> problems = problemMapper.getAllProblems(userId);
        for (ProblemDto problem : problems) {
            List<String> categories = problemMapper.getCategoriesByProblemId(problem.getId());
            problem.setCategory(categories.isEmpty() ? List.of("미분류") : categories);
        }
        return problems;
    }
    
    // 문제 저장
    @PostMapping("/api/problems")
    public ProblemDto addProblem(@RequestBody ProblemDto problem, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        problem.setUserId(userId);
        problem.setSolveCount(0);
        problemMapper.insertProblem(problem);
        if (problem.getCategory() != null) {
            for (String category : problem.getCategory()) {
                problemMapper.insertCategory(problem.getId(), category);
            }
        }
        return problem;
    }

    // 문제 삭제
    @DeleteMapping("/api/problems/{id}")
    public void deleteProblem(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        problemMapper.deleteProblem(id, userId);
    }

    private String convertLevel(int level) {
        String[] tiers = {"Unrated", 
            "Bronze V", "Bronze IV", "Bronze III", "Bronze II", "Bronze I",
            "Silver V", "Silver IV", "Silver III", "Silver II", "Silver I",
            "Gold V", "Gold IV", "Gold III", "Gold II", "Gold I",
            "Platinum V", "Platinum IV", "Platinum III", "Platinum II", "Platinum I",
            "Diamond V", "Diamond IV", "Diamond III", "Diamond II", "Diamond I",
            "Ruby V", "Ruby IV", "Ruby III", "Ruby II", "Ruby I"};
        if (level >= 0 && level < tiers.length) {
            return tiers[level];
        }
        return "Unknown";
    }
}