import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProblemCollectorImpl implements ProblemCollector {

    private final HttpClient client;
    private final String token;

    // 정규식 패턴 (컴파일 1회, 재사용)
    private static final Pattern TITLE_PATTERN    = Pattern.compile("# \\[(.+?)\\]\\s*(.+?)\\s*-\\s*(\\d+)");
    private static final Pattern TITLE_NO_NUM     = Pattern.compile("# \\[(.+?)\\]\\s*(.+)");
    private static final Pattern LINK_PATTERN     = Pattern.compile("\\[문제 링크\\]\\((https?://[^)]+)\\)");
    private static final Pattern LINK_NUM_PATTERN = Pattern.compile("/problem/(\\d+)");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("### 분류\\s*\\n+(.+?)\\n");
    private static final Pattern IMG_PATTERN      = Pattern.compile("<img[^>]+src=\"(https?://[^\"]+)\"");
    private static final Pattern DIFFICULTY_VALID  = Pattern.compile("(?i)(Bronze|Silver|Gold|Platinum|Diamond|Ruby|Unrated).*");

    public ProblemCollectorImpl(String token) {
        this.client = HttpClient.newHttpClient();
        this.token = token;
    }

    // ──────────────────────────────────────────────
    //  GitHub 검색
    // ──────────────────────────────────────────────

    @Override
    public List<String> searchGitHub(String query, int page, int perPage) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://api.github.com/search/code?q=" + encoded
                       + "&per_page=" + perPage + "&page=" + page;

            String body = githubApiGet(url);
            if (body == null) return List.of();

            return extractFileUrls(body);
        } catch (Exception e) {
            System.out.println("[검색 오류] " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public String fetchReadmeContent(String fileUrl) {
        try {
            String body = githubApiGet(fileUrl);
            if (body == null) return null;

            // JSON에서 "content" 필드 추출 후 Base64 디코딩
            int contentStart = body.indexOf("\"content\":\"") + 11;
            int contentEnd = body.indexOf("\"", contentStart);
            if (contentStart < 11 || contentEnd == -1) return null;

            String base64 = body.substring(contentStart, contentEnd).replace("\\n", "");
            return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    // ──────────────────────────────────────────────
    //  README 파싱
    // ──────────────────────────────────────────────

    @Override
    public ParsedProblem parseReadme(String readmeContent) {
        if (readmeContent == null || readmeContent.isBlank()) return null;

        String firstLine = readmeContent.split("\n")[0];

        // 링크 추출
        Matcher linkMatcher = LINK_PATTERN.matcher(readmeContent);
        String link = linkMatcher.find() ? linkMatcher.group(1) : "";

        // 문제 번호 (링크에서 추출)
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

        // 난이도 유효성 검증
        if (!DIFFICULTY_VALID.matcher(difficulty).matches()) {
            return null;
        }

        // 사이트 판별
        String site = determineSite(link);

        // 카테고리 추출
        List<String> categories = extractCategories(readmeContent);

        // 본문 추출 (이미지 처리는 별도 단계)
        String description = extractRawDescription(readmeContent);

        return new ParsedProblem(title, number, difficulty, site, link, description, categories);
    }

    // ──────────────────────────────────────────────
    //  이미지 처리
    // ──────────────────────────────────────────────

    @Override
    public String convertImageToBase64(String imageUrl) {
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
            // 변환 실패
        }
        return null;
    }

    @Override
    public String processDescriptionImages(String description) {
        if (description == null || !description.contains("<img")) return description;

        Matcher imgMatcher = IMG_PATTERN.matcher(description);
        StringBuilder sb = new StringBuilder();

        while (imgMatcher.find()) {
            String imgUrl = imgMatcher.group(1);
            String dataUri = convertImageToBase64(imgUrl);

            if (dataUri != null) {
                // 성공: 원본 URL → Base64 data URI로 교체
                imgMatcher.appendReplacement(sb,
                    Matcher.quoteReplacement(imgMatcher.group().replace(imgUrl, dataUri)));
            } else {
                // 실패: img 태그 전체를 대체 텍스트로 교체
                imgMatcher.appendReplacement(sb,
                    Matcher.quoteReplacement("<em>[이미지를 불러올 수 없습니다]</em>"));
            }
        }

        imgMatcher.appendTail(sb);
        return sb.toString();
    }

    // ──────────────────────────────────────────────
    //  내부 헬퍼 메서드
    // ──────────────────────────────────────────────

    /** GitHub API GET 요청 (인증 포함). 실패 시 null 반환 */
    private String githubApiGet(String url) throws Exception {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) { // url에 특수문자가 올 때 퍼센트 인코딩 처리
            uri = URI.create(url.replace("[", "%5B").replace("]", "%5D")
                               .replace("{", "%7B").replace("}", "%7D"));
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github.v3+json")
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        }
        return null;
    }

    /** 검색 결과 JSON에서 파일 URL 목록 추출 */
    private List<String> extractFileUrls(String searchBody) {
        List<String> urls = new ArrayList<>();
        int from = 0;
        while (true) {
            int idx = searchBody.indexOf("\"url\":\"https://api.github.com/repositories", from);
            if (idx == -1) break;
            int start = searchBody.indexOf("\"", idx + 6) + 1;
            int end = searchBody.indexOf("\"", start);
            urls.add(searchBody.substring(start, end));
            from = end;
        }
        return urls;
    }

    /** 링크 URL로 사이트 판별 */
    private String determineSite(String link) {
        if (link.contains("acmicpc.net"))       return "BOJ";
        if (link.contains("programmers.co.kr")) return "PROGRAMMERS";
        if (link.contains("swexpertacademy"))   return "SWEA";
        return "UNKNOWN";
    }

    /** 카테고리 목록 추출 */
    private List<String> extractCategories(String readme) {
        List<String> categories = new ArrayList<>();
        Matcher m = CATEGORY_PATTERN.matcher(readme);
        if (m.find()) {
            String raw = m.group(1).trim();
            raw = raw.replaceAll("<[^>]+>", "").trim();     // HTML 태그 제거
            raw = raw.replaceAll("[\\[\\]]", "").trim();    // 대괄호 제거
            if (!raw.equals("문제 분류") && !raw.isEmpty()) {
                for (String cat : raw.split(",")) {
                    String trimmed = cat.trim();
                    if (!trimmed.isEmpty()) categories.add(trimmed);
                }
            }
        }
        return categories;
    }

    /** 문제 본문 추출 (이미지 처리 없이 원본 그대로) */
    private String extractRawDescription(String readme) {
        int descStart = readme.indexOf("### 문제 설명");
        if (descStart == -1) return "";
        String desc = readme.substring(descStart).trim();
        return desc.replaceFirst("### 문제 설명\\s*", "").trim();
    }
}
