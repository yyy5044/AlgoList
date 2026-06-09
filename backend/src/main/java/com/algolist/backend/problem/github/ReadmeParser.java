package com.algolist.backend.problem.github;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.algolist.backend.problem.ProblemDto;

/**
 * 백준허브가 생성한 README.md를 파싱하여 문제 메타데이터를 추출한다.
 * 순수 문자열 처리만 수행하며, 외부 의존성 없음.
 */
@Component
public class ReadmeParser {

    private static final Pattern TITLE_PATTERN    = Pattern.compile("# \\[(.+?)\\]\\s*(.+?)\\s*-\\s*(\\d+)");
    private static final Pattern TITLE_NO_NUM     = Pattern.compile("# \\[(.+?)\\]\\s*(.+)");
    private static final Pattern LINK_PATTERN     = Pattern.compile("\\[문제 링크\\]\\((https?://[^)]+)\\)");
    private static final Pattern LINK_NUM_PATTERN = Pattern.compile("/problem/(\\d+)");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("### 분류\\s*\\n+(.+?)\\n");
    private static final Pattern DIFFICULTY_VALID = Pattern.compile("(?i)(Bronze|Silver|Gold|Platinum|Diamond|Ruby|Unrated).*");

    /** README 전체 텍스트 → ProblemDto. 파싱 실패 시 null 반환 */
    public ProblemDto parse(String readmeContent) {
        if (readmeContent == null || readmeContent.isBlank()) return null;

        String firstLine = readmeContent.split("\n")[0];

        // 링크
        Matcher linkMatcher = LINK_PATTERN.matcher(readmeContent);
        String link = linkMatcher.find() ? linkMatcher.group(1) : "";

        // 문제 번호 (링크에서)
        String number = "";
        Matcher linkNumMatcher = LINK_NUM_PATTERN.matcher(link);
        if (linkNumMatcher.find()) number = linkNumMatcher.group(1);

        // 제목 + 난이도
        String difficulty = "";
        String title = "";

        Matcher titleMatcher = TITLE_PATTERN.matcher(firstLine);
        if (titleMatcher.find()) {
            difficulty = titleMatcher.group(1).trim();
            title = titleMatcher.group(2).trim();
            if (number.isEmpty()) number = titleMatcher.group(3).trim();
        } else {
            Matcher noNumMatcher = TITLE_NO_NUM.matcher(firstLine);
            if (noNumMatcher.find()) {
                difficulty = noNumMatcher.group(1).trim();
                title = noNumMatcher.group(2).trim();
            }
        }

        title = title.replaceAll("^\\[|\\]$", "").trim();

        // 필수 필드 검증
        if (title.isEmpty() || number.isEmpty() || difficulty.isEmpty() || link.isEmpty()) {
            return null;
        }

        // 난이도 유효성
        if (!DIFFICULTY_VALID.matcher(difficulty).matches()) {
            return null;
        }

        // 사이트 판별
        String site = determineSite(link);

        // 카테고리
        List<String> categories = extractCategories(readmeContent);

        // 본문 (이미지 처리는 GitHubClient가 담당)
        String description = extractRawDescription(readmeContent);

        // ProblemDto로 반환 (problemId는 DB 저장 시 세팅됨)
        ProblemDto dto = new ProblemDto();
        dto.setTitle(title);
        dto.setNumber(number);
        dto.setDifficulty(difficulty);
        dto.setSite(site);
        dto.setLink(link);
        dto.setDescription(description);
        dto.setCategory(categories);
        return dto;
    }

    private String determineSite(String link) {
        if (link.contains("acmicpc.net"))       return "BOJ";
        if (link.contains("programmers.co.kr")) return "PROGRAMMERS";
        if (link.contains("swexpertacademy"))   return "SWEA";
        return "UNKNOWN";
    }

    private List<String> extractCategories(String readme) {
        List<String> categories = new ArrayList<>();
        Matcher m = CATEGORY_PATTERN.matcher(readme);
        if (m.find()) {
            String raw = m.group(1).trim();
            raw = raw.replaceAll("<[^>]+>", "").trim();
            raw = raw.replaceAll("[\\[\\]]", "").trim();
            if (!raw.equals("문제 분류") && !raw.isEmpty()) {
                for (String cat : raw.split(",")) {
                    String trimmed = cat.trim();
                    if (!trimmed.isEmpty()) categories.add(trimmed);
                }
            }
        }
        return categories;
    }

    /** 알려진 섹션 헤더 목록 — 이 중 하나가 나오면 해당 섹션까지만 포함한다 */
    private static final List<String> KNOWN_SECTIONS = List.of(
        "### 문제 설명", "### 입력", "### 출력",
        "### 예제 입력", "### 예제 출력",
        "### 힌트", "### 노트", "### 제한"
    );

    /**
     * README에서 문제 본문만 추출한다.
     * '### 문제 설명'부터 시작하되, 알려진 섹션(입력/출력/예제/힌트 등)은 포함하고
     * 레포 주인이 임의로 추가한 섹션(풀이 노트, 핵심 아이디어 등)은 제외한다.
     */
    private String extractRawDescription(String readme) {
        int descStart = readme.indexOf("### 문제 설명");
        if (descStart == -1) return "";

        String body = readme.substring(descStart);

        // ### 또는 ## 헤더를 기준으로 섹션을 분리
        // 각 헤더가 알려진 섹션이면 포함, 아니면 거기서 자른다
        StringBuilder result = new StringBuilder();
        String[] lines = body.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // ### 또는 ## 로 시작하는 헤더를 만났을 때
            if (trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
                if (isKnownSection(trimmed)) {
                    result.append(line).append("\n");
                } else {
                    // 알 수 없는 섹션 → 여기서 중단
                    break;
                }
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString().replaceFirst("### 문제 설명\\s*", "").trim();
    }

    /** 헤더가 알려진 백준 문제 섹션인지 판별 */
    private boolean isKnownSection(String header) {
        String trimmed = header.trim();
        for (String known : KNOWN_SECTIONS) {
            if (trimmed.startsWith(known)) return true;
        }
        return false;
    }
}
