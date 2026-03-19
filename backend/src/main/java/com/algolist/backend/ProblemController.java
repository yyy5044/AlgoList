package com.algolist.backend;

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

    @GetMapping("/api/search")
    public List<Map<String, Object>> searchProblem(@RequestParam String query) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            String url = "https://solved.ac/api/v3/search/problem?query=" + query;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map body = response.getBody();

            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

                for (Map<String, Object> item : items) {
                    Map<String, Object> problem = new HashMap<>();
                    problem.put("title", item.get("titleKo"));
                    problem.put("number", String.valueOf(item.get("problemId")));
                    problem.put("difficulty", convertLevel((int) item.get("level")));
                    problem.put("site", "BOJ");
                    problem.put("link", "https://www.acmicpc.net/problem/" + item.get("problemId"));

                    // 알고리즘 태그 추출
                    List<String> categories = new ArrayList<>();
                    List<Map<String, Object>> tags = (List<Map<String, Object>>) item.get("tags");
                    if (tags != null) {
                        for (Map<String, Object> tag : tags) {
                            List<Map<String, Object>> displayNames = (List<Map<String, Object>>) tag.get("displayNames");
                            for (Map<String, Object> name : displayNames) {
                                if ("ko".equals(name.get("language"))) {
                                    categories.add((String) name.get("name"));
                                }
                            }
                        }
                    }
                    problem.put("category", categories.isEmpty() ? List.of("미분류") : categories);

                    results.add(problem);
                }
            }
        } catch (Exception e) {
            System.out.println("검색 실패: " + e.getMessage());
        }

        return results;
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