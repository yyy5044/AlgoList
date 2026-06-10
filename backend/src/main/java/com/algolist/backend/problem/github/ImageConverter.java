package com.algolist.backend.problem.github;

import java.net.URI;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * HTML 본문 내 외부 이미지 URL을 Base64 data URI로 변환한다.
 * GitHub과 무관한 범용 컴포넌트 — 어떤 사이트의 본문이든 처리 가능.
 */
@Slf4j
@Component
public class ImageConverter {

    private final RestClient restClient;
    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]+src=\"(https?://[^\"]+)\"");

    public ImageConverter() {
        // 외부 이미지 서버가 응답을 지연하면 요청 스레드가 묶이므로 타임아웃 필수
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

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
            ResponseEntity<byte[]> response = restClient.get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .toEntity(byte[].class);

            byte[] body = response.getBody();
            if (body == null) return null;

            MediaType contentType = response.getHeaders().getContentType();
            String type = (contentType != null) ? contentType.toString() : "image/png";
            return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(body);
        } catch (Exception e) {
            log.warn("이미지 다운로드 실패: {} ({})", imageUrl, e.getMessage());
            return null;
        }
    }
}
