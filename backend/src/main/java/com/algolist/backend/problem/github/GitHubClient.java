package com.algolist.backend.problem.github;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * GitHub API 통신만 담당하는 클라이언트.
 * 요청을 보내고 응답 JSON을 돌려주는 것까지만 책임진다.
 * 통신 실패 시 예외를 그대로 던지며, 처리 정책은 호출자가 결정한다.
 */
@Component
public class GitHubClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GitHubClient(@Value("${github.token:}") String token) {
        RestClient.Builder builder = RestClient.builder()
            .defaultHeader("Accept", "application/vnd.github.v3+json");

        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        this.restClient = builder.build();
    }

    /** GitHub Search API 호출 → 응답 JSON 반환 */
    public List<String> searchCode(String query, int perPage, int page) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = toUri("https://api.github.com/search/code?q=" + encoded + "&per_page=" + perPage + "&page=" + page);
        String json = restClient.get()
					            .uri(uri)
					            .retrieve()
					            .body(String.class);
        
        return extractFileUrls(json);
    }

    /** GitHub Contents API로 파일 내용을 가져와 Base64 디코딩 후 반환 */
    public String fetchFileContent(String fileUrl) {
        String body = restClient.get()
            .uri(toUri(fileUrl))
            .retrieve()
            .body(String.class);
        if (body == null) return null;

        JsonNode contentNode;
        try {
            contentNode = objectMapper.readTree(body).get("content");
        } catch (JsonProcessingException e) {
            // checked → unchecked로 전환 (개별 파일 실패는 Collector 루프가 흡수)
            throw new IllegalStateException("Contents API 응답 파싱 실패", e);
        }
        if (contentNode == null) return null;

        // getMimeDecoder는 base64 중간의 줄바꿈을 무시하므로 별도 치환이 필요 없다
        byte[] decoded = Base64.getMimeDecoder().decode(contentNode.asText());
        return new String(decoded, StandardCharsets.UTF_8);
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
