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

    // 파싱된 문제 데이터
    record Problem(String title, String number, String difficulty, String site, String link, String description, List<String> categories) {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("사용법: java DataCollector.java <GitHub_Token>");
            return;
        }
        String token = args[0];
        HttpClient client = HttpClient.newHttpClient();

        // 중복 제거용 Set (문제 번호 기준)
        Set<String> seenNumbers = new HashSet<>();
        List<Problem> problems = new ArrayList<>();

        // 정규식 패턴
        Pattern titlePattern = Pattern.compile("# \\[(.+?)\\]\\s*(.+?)\\s*-\\s*(\\d+)");
        Pattern linkPattern = Pattern.compile("\\[문제 링크\\]\\((https?://[^)]+)\\)");
        Pattern categoryPattern = Pattern.compile("### 분류\\s*\\n+(.+?)\\n");

        // === 1단계: 검색 (per_page=100) ===
        String query = URLEncoder.encode(
            "\"### 문제 설명\" \"### 분류\" filename:README.md",
            StandardCharsets.UTF_8
        );
        String searchUrl = "https://api.github.com/search/code?q=" + query + "&per_page=100";

        System.out.println("검색 중...");
        HttpRequest searchRequest = HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github.v3+json")
            .build();

        HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
        String searchBody = searchResponse.body();

        // total_count 출력
        int tcStart = searchBody.indexOf("\"total_count\":") + 14;
        int tcEnd = searchBody.indexOf(",", tcStart);
        System.out.println("검색 결과: " + searchBody.substring(tcStart, tcEnd) + "개 파일");
        System.out.println();

        // === 2단계 + 3단계: 파일 내용 가져오기 + 파싱 ===
        int searchFrom = 0;
        int total = 0;
        int skipped = 0;
        int duplicates = 0;

        while (total < 100) {
            int urlIndex = searchBody.indexOf("\"url\":\"https://api.github.com/repositories", searchFrom);
            if (urlIndex == -1) break;

            int urlStart = searchBody.indexOf("\"", urlIndex + 6) + 1;
            int urlEnd = searchBody.indexOf("\"", urlStart);
            String fileUrl = searchBody.substring(urlStart, urlEnd);
            searchFrom = urlEnd;

            total++;

            // 파일 내용 가져오기
            HttpRequest contentRequest = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .build();

            HttpResponse<String> contentResponse = client.send(contentRequest, HttpResponse.BodyHandlers.ofString());
            String contentBody = contentResponse.body();

            // Base64 디코딩
            int contentStart = contentBody.indexOf("\"content\":\"") + 11;
            int contentEnd = contentBody.indexOf("\"", contentStart);
            String base64Content = contentBody.substring(contentStart, contentEnd)
                .replace("\\n", "");

            String readme;
            try {
                readme = new String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.out.println("[" + total + "] 디코딩 실패 (건너뜀)");
                skipped++;
                continue;
            }

            // 파싱
            String firstLine = readme.split("\n")[0];

            Matcher linkMatcher = linkPattern.matcher(readme);
            String link = linkMatcher.find() ? linkMatcher.group(1) : "";

            String number = "";
            Matcher linkNumMatcher = Pattern.compile("/problem/(\\d+)").matcher(link);
            if (linkNumMatcher.find()) number = linkNumMatcher.group(1);

            Matcher titleMatcher = titlePattern.matcher(firstLine);
            String difficulty = "";
            String title = "";

            if (titleMatcher.find()) {
                difficulty = titleMatcher.group(1).trim();
                title = titleMatcher.group(2).trim();
                if (number.isEmpty()) number = titleMatcher.group(3).trim();
            } else {
                Matcher noNumMatcher = Pattern.compile("# \\[(.+?)\\]\\s*(.+)").matcher(firstLine);
                if (noNumMatcher.find()) {
                    difficulty = noNumMatcher.group(1).trim();
                    title = noNumMatcher.group(2).trim();
                }
            }

            title = title.replaceAll("^\\[|\\]$", "").trim();

            // 필수 필드 체크
            if (title.isEmpty() || number.isEmpty() || difficulty.isEmpty() || link.isEmpty()) {
                skipped++;
                continue;
            }

            // 난이도 정상 값 체크
            if (!difficulty.matches("(?i)(Bronze|Silver|Gold|Platinum|Diamond|Ruby|Unrated).*")) {
                skipped++;
                continue;
            }

            // 중복 체크
            if (seenNumbers.contains(number)) {
                duplicates++;
                continue;
            }
            seenNumbers.add(number);

            // 사이트 판별
            String site = "UNKNOWN";
            if (link.contains("acmicpc.net")) site = "BOJ";
            else if (link.contains("programmers.co.kr")) site = "PROGRAMMERS";
            else if (link.contains("swexpertacademy")) site = "SWEA";

            // 카테고리 추출
            Matcher categoryMatcher = categoryPattern.matcher(readme);
            String categoryStr = "";
            if (categoryMatcher.find()) {
                categoryStr = categoryMatcher.group(1).trim();
                categoryStr = categoryStr.replaceAll("<[^>]+>", "").trim();
                categoryStr = categoryStr.replaceAll("[\\[\\]]", "").trim();
                if (categoryStr.equals("문제 분류")) categoryStr = "";
            }

            List<String> categories = new ArrayList<>();
            if (!categoryStr.isEmpty()) {
                for (String cat : categoryStr.split(",")) {
                    categories.add(cat.trim());
                }
            }

            // 문제 본문 추출
            String description = "";
            int descStart = readme.indexOf("### 문제 설명");
            if (descStart != -1) {
                description = readme.substring(descStart).trim();
                description = description.replaceFirst("### 문제 설명\\s*", "").trim();

                // 이미지를 Base64로 변환
                Matcher imgMatcher = Pattern.compile("<img[^>]+src=\"(https?://[^\"]+)\"").matcher(description);
                StringBuilder sb = new StringBuilder();
                while (imgMatcher.find()) {
                    String imgUrl = imgMatcher.group(1);
                    try {
                        HttpRequest imgRequest = HttpRequest.newBuilder()
                            .uri(URI.create(imgUrl)).build();
                        HttpResponse<byte[]> imgResponse = client.send(imgRequest, HttpResponse.BodyHandlers.ofByteArray());
                        if (imgResponse.statusCode() == 200) {
                            String contentType = imgResponse.headers().firstValue("content-type").orElse("image/png");
                            String imgBase64 = Base64.getEncoder().encodeToString(imgResponse.body());
                            String dataUri = "data:" + contentType + ";base64," + imgBase64;
                            imgMatcher.appendReplacement(sb, Matcher.quoteReplacement(imgMatcher.group().replace(imgUrl, dataUri)));
                        } else {
                            imgMatcher.appendReplacement(sb, Matcher.quoteReplacement(imgMatcher.group()));
                        }
                    } catch (Exception e) {
                        imgMatcher.appendReplacement(sb, Matcher.quoteReplacement(imgMatcher.group()));
                    }
                }
                imgMatcher.appendTail(sb);
                description = sb.toString();
            }

            problems.add(new Problem(title, number, difficulty, site, link, description, categories));
            System.out.println("[" + problems.size() + "] " + difficulty + " | " + title + " (" + number + ")");

            Thread.sleep(1000); // API 제한 방지
        }

        System.out.println();
        System.out.println("=== 수집 완료 ===");
        System.out.println("검색: " + total + "개 / 건너뜀: " + skipped + "개 / 중복: " + duplicates + "개 / 수집: " + problems.size() + "개");

        // === 4단계: SQL 파일 생성 ===
        if (problems.isEmpty()) {
            System.out.println("수집된 문제가 없습니다.");
            return;
        }

        PrintWriter pw = new PrintWriter(new FileWriter("problems_data.sql", StandardCharsets.UTF_8));

        for (Problem p : problems) {
            String escapedTitle = p.title().replace("'", "''");
            String escapedDesc = p.description().replace("'", "''").replace("\\", "\\\\");

            // problems 테이블 INSERT (중복이면 무시)
            pw.println("INSERT IGNORE INTO problems (title, number, difficulty, site, link, description) VALUES ('"
                + escapedTitle + "', '"
                + p.number() + "', '"
                + p.difficulty() + "', '"
                + p.site() + "', '"
                + p.link() + "', '"
                + escapedDesc + "');");

            // problem_categories 테이블 INSERT
            for (String cat : p.categories()) {
                String escapedCat = cat.replace("'", "''");
                pw.println("INSERT IGNORE INTO problem_categories (problem_id, category_name) "
                    + "SELECT problem_id, '" + escapedCat + "' FROM problems WHERE number='" + p.number() + "' AND site='" + p.site() + "';");
            }

            pw.println();
        }

        pw.close();
        System.out.println("SQL 파일 생성 완료: problems_data.sql (" + problems.size() + "개 문제)");
    }
}
