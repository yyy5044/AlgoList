package com.algolist.backend.problem.github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * HTML 본문 내 외부 이미지 URL을 Base64 data URI로 변환한다.
 * GitHub과 무관한 범용 컴포넌트 — 어떤 사이트의 본문이든 처리 가능.
 */
@Component
public class ImageConverter {

    private final HttpClient client = HttpClient.newHttpClient();
    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]+src=\"(https?://[^\"]+)\"");

    /**
     * HTML 내 모든 <img src="https://..."> 를 찾아
     * 이미지를 다운로드한 뒤 Base64 data URI로 교체한다.
     * 다운로드 실패 시 대체 텍스트로 교체.
     */
    public String processImages(String html) {
        if (html == null || !html.contains("<img")) return html;

        Matcher imgMatcher = IMG_PATTERN.matcher(html);
        StringBuilder sb = new StringBuilder();

        while (imgMatcher.find()) {
            String imgUrl = imgMatcher.group(1);
            String dataUri = downloadAsBase64(imgUrl);

            if (dataUri != null) {
                imgMatcher.appendReplacement(sb,
                    Matcher.quoteReplacement(imgMatcher.group().replace(imgUrl, dataUri)));
            } else {
                imgMatcher.appendReplacement(sb,
                    Matcher.quoteReplacement("<em>[이미지를 불러올 수 없습니다]</em>"));
            }
        }

        imgMatcher.appendTail(sb);
        return sb.toString();
    }

    /** 이미지 URL → Base64 data URI. 실패 시 null */
    private String downloadAsBase64(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                String contentType = response.headers()
                    .firstValue("content-type").orElse("image/png");
                String base64 = Base64.getEncoder().encodeToString(response.body());
                return "data:" + contentType + ";base64," + base64;
            }
        } catch (Exception e) {
            // 다운로드 실패
        }
        return null;
    }
}
