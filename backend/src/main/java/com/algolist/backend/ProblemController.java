package com.algolist.backend;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class ProblemController {

    @GetMapping("/api/search")
    public List<Map<String, String>> searchProblem(@RequestParam String query) {
        // 더미 데이터로 검색 결과 반환
        List<Map<String, String>> results = new ArrayList<>();

        if (query.contains("게리") || query.contains("17471")) {
            results.add(Map.of(
                "title", "게리맨더링",
                "number", "17471",
                "difficulty", "gold3",
                "site", "BOJ",
                "link", "https://www.acmicpc.net/problem/17471"
            ));
        }

        if (query.contains("활주") || query.contains("4014")) {
            results.add(Map.of(
                "title", "활주로 건설",
                "number", "4014",
                "difficulty", "gold3",
                "site", "SWEA",
                "link", "https://swexpertacademy.com/main/code/problem/problemDetail.do?contestProbId=AWIeW7FakkUDFAVH"
            ));
        }

        return results;
    }
}