package com.algolist.backend.problem.github;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * GitHub API 통신만 담당하는 클라이언트.
 * 요청을 보내고 응답 JSON을 돌려주는 것까지만 책임진다.
 */
@Component
public class GitHubClient {

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${github.token:}")
    private String token;

    /** GitHub Search API 호출 → 응답 JSON 반환 (null이면 실패) */
    public String searchCode(String query, int perPage) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://api.github.com/search/code?q=" + encoded
                       + "&per_page=" + perPage;
            return githubGet(url);
        } catch (Exception e) {
            return null;
        }
    }

    /** GitHub Contents API로 파일 내용을 가져와 Base64 디코딩 후 반환 */
    public String fetchFileContent(String fileUrl) {
        try {
            String body = githubGet(fileUrl);
            if (body == null) return null;

            int start = body.indexOf("\"content\":\"") + 11;
            int end = body.indexOf("\"", start);
            if (start < 11 || end == -1) return null;

            String base64 = body.substring(start, end).replace("\\n", "");
            return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private String githubGet(String url) throws Exception {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            uri = URI.create(url.replace("[", "%5B").replace("]", "%5D")
                               .replace("{", "%7B").replace("}", "%7D"));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(uri)
            .header("Accept", "application/vnd.github.v3+json");

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return (response.statusCode() == 200) ? response.body() : null;
    }
}
