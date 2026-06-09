import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataCollector {

    record Problem(String title, String number, String difficulty, String site, String link,
                   String description, List<String> categories) {}

    // 정규식 패턴 (재사용)
    static final Pattern TITLE_PATTERN = Pattern.compile("# \\[(.+?)\\]\\s*(.+?)\\s*-\\s*(\\d+)");
    static final Pattern TITLE_NO_NUM  = Pattern.compile("# \\[(.+?)\\]\\s*(.+)");
    static final Pattern LINK_PATTERN  = Pattern.compile("\\[문제 링크\\]\\((https?://[^)]+)\\)");
    static final Pattern CATEGORY_PATTERN = Pattern.compile("### 분류\\s*\\n+(.+?)\\n");
    static final Pattern LINK_NUM_PATTERN = Pattern.compile("/problem/(\\d+)");
    static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]+src=\"(https?://[^\"]+)\"");
    static final Pattern DIFFICULTY_VALID = Pattern.compile("(?i)(Bronze|Silver|Gold|Platinum|Diamond|Ruby|Unrated).*");

    // 난이도별 검색 쿼리 (GitHub 1000개 제한 우회)
    static final String[] DIFFICULTIES = {"Bronze", "Silver", "Gold", "Platinum", "Diamond", "Ruby"};

    // 중복 제거용 (전체 실행 동안 유지)
    static Set<String> seenNumbers = new HashSet<>();
    static int totalSearched = 0;
    static int totalSkipped = 0;
    static int totalDuplicates = 0;
    static int totalCollected = 0;
    static int totalImageFailed = 0;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("사용법: java DataCollector.java <GitHub_Token>");
            return;
        }
        String token = args[0];
        HttpClient client = HttpClient.newHttpClient();

        // SQL 파일 초기화
        PrintWriter pw = new PrintWriter(new FileWriter("problems_data.sql", StandardCharsets.UTF_8));
        pw.println("-- AlgoList 문제 데이터 (자동 생성)");
        pw.println();

        // === 난이도별로 검색 ===
        for (String diff : DIFFICULTIES) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("  난이도: " + diff + " 수집 시작");
            System.out.println("========================================");

            List<Problem> problems = searchByDifficulty(client, token, diff);

            // SQL 파일에 쓰기
            for (Problem p : problems) {
                writeProblemSql(pw, p);
            }
            pw.flush(); // 난이도마다 즉시 파일에 기록 (중간 중단 대비)

            System.out.println("[" + diff + "] 수집 완료: " + problems.size() + "개");
        }

        pw.close();

        // === 최종 통계 ===
        System.out.println();
        System.out.println("============================================");
        System.out.println("  전체 수집 완료");
        System.out.println("============================================");
        System.out.println("총 검색: " + totalSearched + "개 파일");
        System.out.println("건너뜀: " + totalSkipped + "개");
        System.out.println("중복: " + totalDuplicates + "개");
        System.out.println("이미지 변환 실패: " + totalImageFailed + "개");
        System.out.println("최종 수집: " + totalCollected + "개 문제");
        System.out.println("SQL 파일: problems_data.sql");
    }

    /** 특정 난이도로 검색하여 문제 목록 반환 (최대 1000개 = 10페이지) */
    static List<Problem> searchByDifficulty(HttpClient client, String token, String difficulty) throws Exception {
        List<Problem> problems = new ArrayList<>();

        for (int page = 1; page <= 10; page++) {
            String query = URLEncoder.encode(
                difficulty + " \"### 문제 설명\" filename:README.md",
                StandardCharsets.UTF_8
            );
            String searchUrl = "https://api.github.com/search/code?q=" + query
                + "&per_page=100&page=" + page;

            // 검색 API 호출
            HttpResponse<String> searchResponse = apiCall(client, token, searchUrl);
            if (searchResponse.statusCode() != 200) {
                System.out.println("  [page " + page + "] 검색 실패 (HTTP " + searchResponse.statusCode() + ") - 건너뜀");
                // 검색 rate limit (30/min) 대기
                if (searchResponse.statusCode() == 403 || searchResponse.statusCode() == 422) {
                    System.out.println("  Rate limit 대기 60초...");
                    Thread.sleep(60000);
                    page--; // 같은 페이지 재시도
                    continue;
                }
                break;
            }

            String searchBody = searchResponse.body();

            // total_count 확인 (첫 페이지에서만)
            if (page == 1) {
                int tcStart = searchBody.indexOf("\"total_count\":") + 14;
                int tcEnd = searchBody.indexOf(",", tcStart);
                String totalCount = searchBody.substring(tcStart, tcEnd);
                System.out.println("  검색 결과: " + totalCount + "개 파일");
            }

            // 결과에서 파일 URL 추출
            List<String> fileUrls = extractFileUrls(searchBody);
            if (fileUrls.isEmpty()) {
                break; // 더 이상 결과 없음
            }

            System.out.println("  [page " + page + "] " + fileUrls.size() + "개 파일 처리 중...");

            // 각 파일 처리
            for (String fileUrl : fileUrls) {
                totalSearched++;
                try {
                    Problem p = processFile(client, token, fileUrl);
                    if (p != null) {
                        problems.add(p);
                        totalCollected++;
                        System.out.println("    [" + totalCollected + "] " + p.difficulty() + " | " + p.title() + " (" + p.number() + ")");
                    }
                } catch (Exception e) {
                    totalSkipped++;
                    System.out.println("    [오류] " + e.getMessage() + " - 건너뜀");
                }
                Thread.sleep(800); // API 제한 방지
            }

            // 검색 API rate limit 대기 (30회/분)
            Thread.sleep(2000);
        }

        return problems;
    }

    /** 검색 결과 JSON에서 파일 URL 목록 추출 */
    static List<String> extractFileUrls(String searchBody) {
        List<String> urls = new ArrayList<>();
        int searchFrom = 0;
        while (true) {
            int urlIndex = searchBody.indexOf("\"url\":\"https://api.github.com/repositories", searchFrom);
            if (urlIndex == -1) break;
            int urlStart = searchBody.indexOf("\"", urlIndex + 6) + 1;
            int urlEnd = searchBody.indexOf("\"", urlStart);
            urls.add(searchBody.substring(urlStart, urlEnd));
            searchFrom = urlEnd;
        }
        return urls;
    }

    /** 개별 README 파일을 가져와서 파싱. 실패 시 null 반환 */
    static Problem processFile(HttpClient client, String token, String fileUrl) throws Exception {
        // 파일 내용 가져오기
        HttpResponse<String> contentResponse = apiCall(client, token, fileUrl);
        if (contentResponse.statusCode() != 200) {
            totalSkipped++;
            return null;
        }
        String contentBody = contentResponse.body();

        // Base64 디코딩
        int contentStart = contentBody.indexOf("\"content\":\"") + 11;
        int contentEnd = contentBody.indexOf("\"", contentStart);
        if (contentStart < 11 || contentEnd == -1) {
            totalSkipped++;
            return null;
        }
        String base64Content = contentBody.substring(contentStart, contentEnd).replace("\\n", "");

        String readme;
        try {
            readme = new String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8);
        } catch (Exception e) {
            totalSkipped++;
            return null;
        }

        // === 파싱 ===
        String firstLine = readme.split("\n")[0];

        // 링크 추출
        Matcher linkMatcher = LINK_PATTERN.matcher(readme);
        String link = linkMatcher.find() ? linkMatcher.group(1) : "";

        // 문제 번호 추출 (링크에서)
        String number = "";
        Matcher linkNumMatcher = LINK_NUM_PATTERN.matcher(link);
        if (linkNumMatcher.find()) number = linkNumMatcher.group(1);

        // 제목, 난이도 추출
        String diff = "";
        String title = "";
        Matcher titleMatcher = TITLE_PATTERN.matcher(firstLine);
        if (titleMatcher.find()) {
            diff = titleMatcher.group(1).trim();
            title = titleMatcher.group(2).trim();
            if (number.isEmpty()) number = titleMatcher.group(3).trim();
        } else {
            Matcher noNumMatcher = TITLE_NO_NUM.matcher(firstLine);
            if (noNumMatcher.find()) {
                diff = noNumMatcher.group(1).trim();
                title = noNumMatcher.group(2).trim();
            }
        }
        title = title.replaceAll("^\\[|\\]$", "").trim();

        // 필수 필드 체크
        if (title.isEmpty() || number.isEmpty() || diff.isEmpty() || link.isEmpty()) {
            totalSkipped++;
            return null;
        }

        // 난이도 정상 값 체크
        if (!DIFFICULTY_VALID.matcher(diff).matches()) {
            totalSkipped++;
            return null;
        }

        // 중복 체크
        if (seenNumbers.contains(number)) {
            totalDuplicates++;
            return null;
        }
        seenNumbers.add(number);

        // 사이트 판별
        String site = "UNKNOWN";
        if (link.contains("acmicpc.net")) site = "BOJ";
        else if (link.contains("programmers.co.kr")) site = "PROGRAMMERS";
        else if (link.contains("swexpertacademy")) site = "SWEA";

        // 카테고리 추출
        List<String> categories = extractCategories(readme);

        // 문제 본문 추출 + 이미지 처리
        String description = extractDescription(client, readme);

        return new Problem(title, number, diff, site, link, description, categories);
    }

    /** 카테고리 목록 추출 */
    static List<String> extractCategories(String readme) {
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

    /** 문제 본문 추출 + 이미지 Base64 변환 (실패 시 대체 텍스트) */
    static String extractDescription(HttpClient client, String readme) throws Exception {
        int descStart = readme.indexOf("### 문제 설명");
        if (descStart == -1) return "";

        String description = readme.substring(descStart).trim();
        description = description.replaceFirst("### 문제 설명\\s*", "").trim();

        // 이미지를 Base64로 변환
        Matcher imgMatcher = IMG_PATTERN.matcher(description);
        StringBuilder sb = new StringBuilder();
        while (imgMatcher.find()) {
            String imgUrl = imgMatcher.group(1);
            try {
                HttpRequest imgRequest = HttpRequest.newBuilder()
                    .uri(URI.create(imgUrl))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();
                HttpResponse<byte[]> imgResponse = client.send(imgRequest, HttpResponse.BodyHandlers.ofByteArray());
                if (imgResponse.statusCode() == 200) {
                    String contentType = imgResponse.headers().firstValue("content-type").orElse("image/png");
                    String imgBase64 = Base64.getEncoder().encodeToString(imgResponse.body());
                    String dataUri = "data:" + contentType + ";base64," + imgBase64;
                    imgMatcher.appendReplacement(sb, Matcher.quoteReplacement(
                        imgMatcher.group().replace(imgUrl, dataUri)));
                } else {
                    // 이미지 로드 실패 — 대체 텍스트로 교체
                    totalImageFailed++;
                    imgMatcher.appendReplacement(sb, Matcher.quoteReplacement(
                        "<em>[이미지를 불러올 수 없습니다]</em>"));
                }
            } catch (Exception e) {
                totalImageFailed++;
                imgMatcher.appendReplacement(sb, Matcher.quoteReplacement(
                    "<em>[이미지를 불러올 수 없습니다]</em>"));
            }
        }
        imgMatcher.appendTail(sb);
        return sb.toString();
    }

    /** GitHub API 호출 (공통) — URL 특수문자([, ] 등) 자동 처리 */
    static HttpResponse<String> apiCall(HttpClient client, String token, String url) throws Exception {
        URI uri;
        try {
            uri = URI.create(url); // 일반 URL은 그대로
        } catch (IllegalArgumentException e) {
            // [PS]BOJ 같은 특수문자가 경로에 있으면 인코딩 처리
            uri = URI.create(url.replace("[", "%5B").replace("]", "%5D")
                               .replace("{", "%7B").replace("}", "%7D"));
        }
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github.v3+json")
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Problem을 SQL INSERT문으로 변환하여 PrintWriter에 쓰기 */
    static void writeProblemSql(PrintWriter pw, Problem p) {
        String escapedTitle = escape(p.title());
        String escapedDesc = escape(p.description());

        pw.println("INSERT IGNORE INTO problems (title, number, difficulty, site, link, description) VALUES ('"
            + escapedTitle + "', '"
            + p.number() + "', '"
            + p.difficulty() + "', '"
            + p.site() + "', '"
            + p.link() + "', '"
            + escapedDesc + "');");

        for (String cat : p.categories()) {
            pw.println("INSERT IGNORE INTO problem_categories (problem_id, category_name) "
                + "SELECT problem_id, '" + escape(cat) + "' FROM problems WHERE number='"
                + p.number() + "' AND site='" + p.site() + "';");
        }
        pw.println();
    }

    /** SQL 문자열 이스케이프 */
    static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
