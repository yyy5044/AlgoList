package com.algolist.backend.problem.batch.codeforces;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.algolist.backend.problem.batch.common.ProblemParseException;
import com.algolist.backend.problem.batch.common.ProblemParser;
import com.algolist.backend.problem.dto.ProblemDto;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * [PROCESS 단계 - 코드포스 전용] 허깅페이스 raw row(JsonNode) 한 건을 ProblemDto 로 변환한다.
 *
 * ProblemParser&lt;JsonNode&gt; 를 구현 → Step 의 processor 자리에 parser::parse 메서드 참조로 꽂힌다.
 * 변환 불가(필수 필드 누락) 또는 예기치 못한 오류는 모두 ProblemParseException 으로 던진다.
 *   → Step 의 skip(ProblemParseException) 설정에 걸려 "실패"로 집계되고 건너뛰어진다.
 */
@Component
public class CodeforcesParser implements ProblemParser<JsonNode> {

    private static final String SITE = "CODEFORCES";

    @Override
    public ProblemDto parse(JsonNode row) {
        try {
            // 문제 번호(number)와 링크를 만들 수 없는 행은 변환 불가 → 실패로 던진다.
            if (!row.hasNonNull("contest_id") || !row.hasNonNull("index")) {
                throw new ProblemParseException("contest_id/index 누락");
            }

            ProblemDto dto = new ProblemDto();
            dto.setSite(SITE);

            String contestId = row.get("contest_id").asText();
            String index = row.get("index").asText();
            dto.setNumber(contestId + index);
            dto.setLink("https://codeforces.com/problemset/problem/" + contestId + "/" + index);

            if (row.hasNonNull("title")) {
                dto.setTitle(row.get("title").asText());
            }
            if (row.hasNonNull("rating")) {
                dto.setDifficulty(row.get("rating").asText());
            }
            if (row.has("tags") && row.get("tags").isArray()) {
                List<String> tags = new ArrayList<>();
                for (JsonNode tag : row.get("tags")) {
                    tags.add(tag.asText());
                }
                dto.setCategory(tags);
            }

            dto.setDescription(buildDescription(row));
            return dto;

        } catch (ProblemParseException e) {
            throw e; // 위에서 던진 "필드 누락"은 그대로 전달
        } catch (Exception e) {
            // 그 밖의 예기치 못한 오류도 "실패"로 통일해서 skip 되게 한다.
            throw new ProblemParseException("파싱 중 예외: " + e.getMessage(), e);
        }
    }

    /** description / input_format / output_format / note 를 하나의 본문으로 합친다. */
    private String buildDescription(JsonNode row) {
        StringBuilder description = new StringBuilder();
        if (row.hasNonNull("description")) {
            description.append("## Description\n\n").append(row.get("description").asText());
        }
        if (row.hasNonNull("input_format")) {
            description.append("\n\n## Input\n\n").append(row.get("input_format").asText());
        }
        if (row.hasNonNull("output_format")) {
            description.append("\n\n## Output\n\n").append(row.get("output_format").asText());
        }
        if (row.has("examples") && row.get("examples").isArray() && !row.get("examples").isEmpty()) {
            description.append("\n\n## Examples\n\n");
            for (JsonNode example : row.get("examples")) {
                description.append("<div class=\"example-box\">");
                if (example.hasNonNull("input")) {
                    description.append("<div class=\"example-header\">Input</div>")
                               .append("<pre class=\"example-data\">").append(example.get("input").asText()).append("</pre>");
                }
                if (example.hasNonNull("output")) {
                    description.append("<div class=\"example-header\">Output</div>")
                               .append("<pre class=\"example-data\">").append(example.get("output").asText()).append("</pre>");
                }
                description.append("</div>");
            }
        }
        if (row.hasNonNull("note")) {
            description.append("\n\n## Note\n\n").append(row.get("note").asText());
        }
        return description.toString();
    }
}
