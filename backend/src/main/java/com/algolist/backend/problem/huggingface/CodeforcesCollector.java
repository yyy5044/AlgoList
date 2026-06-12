package com.algolist.backend.problem.huggingface;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.algolist.backend.problem.ProblemWriter;
import com.algolist.backend.problem.dto.ProblemDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeforcesCollector {

    private static final String 데이터셋_URL =
            "https://datasets-server.huggingface.co/rows?dataset=open-r1/codeforces&config=default&split=train&offset=%d&length=%d";

    private final ProblemWriter 저장기;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void 미리보기(int 시작위치, int 개수) {
        String url = String.format(데이터셋_URL, 시작위치, 개수);
        log.info("요청 URL: {}", url);

        String 응답 = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        try {
            JsonNode 루트 = objectMapper.readTree(응답);
            JsonNode 행목록 = 루트.get("rows");
            if (행목록 == null || !행목록.isArray()) {
                log.warn("rows 필드가 없거나 배열이 아닙니다. 응답 키: {}", 루트.fieldNames());
                log.info("전체 응답:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(루트));
                return;
            }

            log.info("총 {}건 조회됨", 행목록.size());

            for (JsonNode 행래퍼 : 행목록) {
                JsonNode 행 = 행래퍼.get("row");
                if (행 == null) {
                    log.info("row 래핑 없음 — 직접 출력: {}", 행래퍼.fieldNames());
                    continue;
                }

                log.info("=== 문제 ===");
                행.fieldNames().forEachRemaining(필드 ->
                        log.info("  {} = {}", 필드,
                                행.get(필드).asText().length() > 200
                                        ? 행.get(필드).asText().substring(0, 200) + "..."
                                        : 행.get(필드).asText())
                );
            }
        } catch (Exception e) {
            log.error("응답 파싱 실패", e);
            log.info("원본 응답 (앞 2000자):\n{}", 응답 != null && 응답.length() > 2000 ? 응답.substring(0, 2000) : 응답);
        }
    }

    public int 수집(int 시작위치, int 개수) {
        String url = String.format(데이터셋_URL, 시작위치, 개수);
        String 응답 = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        List<ProblemDto> 문제목록 = 파싱(응답);
        int 저장수 = 0;
        for (ProblemDto dto : 문제목록) {
            try {
                저장기.save(dto);
                저장수++;
            } catch (Exception e) {
                log.warn("저장 실패 (건너뜀): {} ({})", dto.getNumber(), e.getMessage());
            }
        }
        log.info("{}건 중 {}건 저장 완료", 문제목록.size(), 저장수);
        return 저장수;
    }

    private List<ProblemDto> 파싱(String json) {
        List<ProblemDto> 결과 = new ArrayList<>();
        try {
            JsonNode 루트 = objectMapper.readTree(json);
            JsonNode 행목록 = 루트.get("rows");
            if (행목록 == null || !행목록.isArray()) return 결과;

            for (JsonNode 행래퍼 : 행목록) {
                JsonNode 행 = 행래퍼.get("row");
                if (행 == null) continue;

                ProblemDto dto = new ProblemDto();
                dto.setSite("CODEFORCES");

                if (행.has("title")) dto.setTitle(행.get("title").asText());

                if (행.has("contest_id") && 행.has("index")) {
                    String 대회번호 = 행.get("contest_id").asText();
                    String 문제인덱스 = 행.get("index").asText();
                    dto.setNumber(대회번호 + 문제인덱스);
                    dto.setLink("https://codeforces.com/problemset/problem/" + 대회번호 + "/" + 문제인덱스);
                }

                if (행.has("rating") && !행.get("rating").isNull()) {
                    dto.setDifficulty(행.get("rating").asText());
                }

                if (행.has("tags") && 행.get("tags").isArray()) {
                    List<String> 태그목록 = new ArrayList<>();
                    for (JsonNode 태그 : 행.get("tags")) {
                        태그목록.add(태그.asText());
                    }
                    dto.setCategory(태그목록);
                }

                StringBuilder 본문 = new StringBuilder();
                if (행.has("description") && !행.get("description").isNull()) {
                    본문.append(행.get("description").asText());
                }
                if (행.has("input_format") && !행.get("input_format").isNull()) {
                    본문.append("\n\n**Input**\n").append(행.get("input_format").asText());
                }
                if (행.has("output_format") && !행.get("output_format").isNull()) {
                    본문.append("\n\n**Output**\n").append(행.get("output_format").asText());
                }
                if (행.has("note") && !행.get("note").isNull()) {
                    본문.append("\n\n**Note**\n").append(행.get("note").asText());
                }
                dto.setDescription(본문.toString());

                결과.add(dto);
            }
        } catch (Exception e) {
            log.error("파싱 실패", e);
        }
        return 결과;
    }
}
