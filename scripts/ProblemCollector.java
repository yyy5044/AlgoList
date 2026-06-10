import java.util.List;

/**
 * 외부 소스(GitHub 등)에서 코딩 문제를 검색, 수집, 파싱하는 인터페이스.
 *
 * 사용처:
 * 1. DataCollector (스크립트) — 대량 수집 시 구현체 호출
 * 2. ProblemService (백엔드) — DB 검색 miss 시 실시간 수집
 */
public interface ProblemCollector {

    // ── 파싱 결과 DTO ──
    record ParsedProblem(
        String title,
        String number,
        String difficulty,
        String site,         // "BOJ", "PROGRAMMERS" 등
        String link,
        String description,
        List<String> categories
    ) {}

    // ── GitHub 검색 ──

    /** GitHub 코드 검색 API 호출 → 매칭된 파일 URL 목록 반환 */
    List<String> searchGitHub(String query, int page, int perPage);

    /** GitHub Contents API로 파일의 raw 내용을 가져와 디코딩 후 반환 */
    String fetchReadmeContent(String fileUrl);

    // ── README 파싱 ──

    /** README 전체 텍스트를 파싱하여 문제 데이터로 변환. 파싱 실패 시 null 반환 */
    ParsedProblem parseReadme(String readmeContent);

    // ── 이미지 처리 ──

    /** 외부 이미지 URL을 Base64 data URI로 변환. 실패 시 null 반환 */
    String convertImageToBase64(String imageUrl);

    /** description 내의 모든 외부 이미지를 Base64로 변환하거나 대체 텍스트로 교체 */
    String processDescriptionImages(String description);
}
