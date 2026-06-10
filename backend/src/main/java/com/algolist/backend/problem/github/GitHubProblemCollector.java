package com.algolist.backend.problem.github;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.algolist.backend.problem.ProblemDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubProblemCollector {

    private final GitHubClient client;
    private final ReadmeParser parser;
    private final ImageConverter imageConverter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 사용자 검색어를 기반으로 GitHub에서 문제를 찾아
     * 파싱된 ProblemDto 리스트로 반환한다.
     */
    public List<ProblemDto> searchProblems(String query, int maxResults) {
        String searchQuery = query + " \"### 문제 설명\" filename:README.md";

        String json = client.searchCode(searchQuery, maxResults);
        if (json == null) return List.of();

        List<String> fileUrls = extractFileUrls(json);

        List<ProblemDto> results = new ArrayList<>();
        for (String fileUrl : fileUrls) {
            // 개별 파일 실패는 건너뛰고 나머지 수집을 계속한다 (best-effort)
            try {
                ProblemDto dto = fetchAndParse(fileUrl);
                if (dto != null) results.add(dto);
            } catch (Exception e) {
                log.warn("문제 수집 실패 (건너뜀): {} ({})", fileUrl, e.getMessage());
            }
        }
        return results;
    }

    /** 하나의 파일 URL → README 조회 → 파싱 → 이미지 처리 */
    private ProblemDto fetchAndParse(String fileUrl) {
        String readme = client.fetchFileContent(fileUrl);
        if (readme == null) return null;

        ProblemDto dto = parser.parse(readme);
        if (dto == null) return null;

        dto.setDescription(imageConverter.processImages(dto.getDescription()));
        return dto;
    }

    /** Search API 응답 JSON에서 파일의 Contents API URL을 추출한다 */
    private List<String> extractFileUrls(String json) {
        List<String> urls = new ArrayList<>();
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            // checked → unchecked로 전환하여 전역 핸들러까지 전파 (삼키지 않음)
            throw new IllegalStateException("GitHub 검색 응답 파싱 실패", e);
        }

        JsonNode items = root.get("items");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                JsonNode urlNode = item.get("url");
                if (urlNode != null) {
                    urls.add(urlNode.asText());
                }
            }
        }
        return urls;
    }
}
