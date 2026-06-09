package com.algolist.backend.problem.github;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.algolist.backend.problem.ProblemDto;

import lombok.RequiredArgsConstructor;

/**
 * GitHub를 통한 문제 수집의 전체 흐름을 조율하는 퍼사드.
 *
 * ProblemService는 이 클래스만 의존하며, 내부적으로
 * GitHubClient(API 통신), ReadmeParser(파싱), ImageConverter(이미지 처리)를 사용한다.
 */
@Component
@RequiredArgsConstructor
public class GitHubProblemCollector {

    private final GitHubClient client;
    private final ReadmeParser parser;
    private final ImageConverter imageConverter;

    /**
     * 사용자 검색어를 기반으로 GitHub에서 문제를 찾아
     * 파싱된 ProblemDto 리스트로 반환한다.
     */
    public List<ProblemDto> searchProblems(String query, int maxResults) {
        // 1. 사용자 쿼리 → GitHub Search API용 쿼리 변환
        String searchQuery = query + " \"### 문제 설명\" filename:README.md";

        // 2. Search API 호출 → 응답 JSON
        String json = client.searchCode(searchQuery, maxResults);
        if (json == null) return List.of();

        // 3. JSON에서 파일 URL 추출
        List<String> fileUrls = extractFileUrls(json);

        // 4. 각 URL: Contents API → README → 파싱 → 이미지 처리
        List<ProblemDto> results = new ArrayList<>();
        for (String fileUrl : fileUrls) {
            try {
                ProblemDto dto = fetchAndParse(fileUrl);
                if (dto != null) results.add(dto);
            } catch (Exception e) {
                // 개별 파일 실패는 무시하고 다음 파일 진행
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
        int from = 0;
        while (true) {
            int idx = json.indexOf("\"url\":\"https://api.github.com/repositories", from);
            if (idx == -1) break;
            int start = json.indexOf("\"", idx + 6) + 1;
            int end = json.indexOf("\"", start);
            urls.add(json.substring(start, end));
            from = end;
        }
        return urls;
    }
}
