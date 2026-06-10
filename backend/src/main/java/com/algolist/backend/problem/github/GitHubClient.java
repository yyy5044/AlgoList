package com.algolist.backend.problem.github;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * GitHub API 통신만 담당하는 클라이언트.
 * 요청을 보내고 응답 JSON을 돌려주는 것까지만 책임진다.
 */
@Slf4j
@Component
public class GitHubClient {

    private final RestClient restClient;

    public GitHubClient(@Value("${github.token:}") String token) {
        RestClient.Builder builder = RestClient.builder()
            .defaultHeader("Accept", "application/vnd.github.v3+json");

        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        this.restClient = builder.build();
    }

    /** GitHub Search API 호출 → 응답 JSON 반환 (실패 시 null) */
    public String searchCode(String query, int perPage) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = toUri("https://api.github.com/search/code?q=" + encoded + "&per_page=" + perPage);
            return restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            log.error("searchCode 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /** GitHub Contents API로 파일 내용을 가져와 Base64 디코딩 후 반환 */
    public String fetchFileContent(String fileUrl) {
        try {
            String body = restClient.get()
                .uri(toUri(fileUrl))
                .retrieve()
                .body(String.class);
            if (body == null) return null;

            int start = body.indexOf("\"content\":\"") + 11;
            int end = body.indexOf("\"", start);
            if (start < 11 || end == -1) return null;

            String base64 = body.substring(start, end).replace("\\n", "");
            return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("fetchFileContent 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 이미 인코딩된 URL 문자열을 URI로 변환한다.
     * RestClient의 .uri(URI)는 재인코딩을 하지 않으므로 이중 인코딩을 방지한다.
     * 대괄호/중괄호 등으로 URI.create가 실패하면 수동 치환 후 재시도.
     */
    private URI toUri(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException e) {
            return URI.create(url.replace("[", "%5B").replace("]", "%5D")
                                 .replace("{", "%7B").replace("}", "%7D"));
        }
    }
}
