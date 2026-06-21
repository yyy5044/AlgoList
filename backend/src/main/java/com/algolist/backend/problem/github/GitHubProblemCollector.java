package com.algolist.backend.problem.github;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.algolist.backend.problem.dto.ProblemDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        String searchQuery = query + " \"### 문제 설명\" filename:README.md";

        List<String> fileUrls = client.searchCode(searchQuery, maxResults, 1);

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

}
