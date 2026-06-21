package com.algolist.backend.problem.batch;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algolist.backend.problem.batch.codeforces.CodeforcesIngestLauncher;
import com.algolist.backend.problem.batch.github.GitHubIngestLauncher;

import lombok.RequiredArgsConstructor;

/**
 * 관리자용 배치 트리거 API.
 *
 * 경로가 /api/admin/** 라 SecurityConfig 에서 ROLE_ADMIN 만 호출할 수 있다.
 * 컨트롤러는 얇게 두고, 실제 실행/중지는 각 배치의 Launcher 서비스에 위임한다.
 * 새 배치가 생기면 그 Launcher 를 주입해 엔드포인트만 한 쌍 더 추가하면 된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/batch")
public class AdminBatchController {

    private final CodeforcesIngestLauncher codeforcesLauncher;
    private final GitHubIngestLauncher githubIngestLauncher;

    // 코드포스 수집 실행 (이미 돌고 있으면 중복 실행하지 않음)
    @PostMapping("/codeforces")
    public ResponseEntity<Map<String, String>> runCodeforces() {
        boolean started = codeforcesLauncher.launch();
        if (started) {
            return ResponseEntity.ok(Map.of("status", "STARTED", "message", "코드포스 수집을 시작했습니다."));
        }
        return ResponseEntity.ok(Map.of("status", "ALREADY_RUNNING", "message", "이미 실행 중입니다."));
    }

    // 코드포스 수집 중지. 실제로 멈출 때까지 대기했다가 결과를 응답한다.
    @PostMapping("/codeforces/stop")
    public ResponseEntity<Map<String, String>> stopCodeforces() {
        boolean stopped = codeforcesLauncher.stopGracefully();
        if (stopped) {
            return ResponseEntity.ok(Map.of("status", "STOPPED", "message", "완전히 중지되었습니다."));
        }
        return ResponseEntity.ok(Map.of("status", "STOPPING",
                "message", "중지 요청됨 — 아직 정리 중입니다. 잠시 후 다시 시도/확인하세요."));
    }
    
    // 백준 문제 수집 실행
    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> runGitHub() {
    	boolean started = githubIngestLauncher.launch();
    	if (started) {
    		return ResponseEntity.ok(Map.of("status", "STARTED", "message", "백준 수집을 시작했습니다."));
    	}
    	return ResponseEntity.ok(Map.of("status", "ALREADY_RUNNING", "message", "이미 실행 중입니다."));
    }
    
    // 백준 수집 중지
    @PostMapping("/github/stop")
    public ResponseEntity<Map<String, String>> stopGitHub() {
        boolean stopped = githubIngestLauncher.stopGracefully();
        if (stopped) {
            return ResponseEntity.ok(Map.of("status", "STOPPED", "message", "완전히 중지되었습니다."));
        }
        return ResponseEntity.ok(Map.of("status", "STOPPING",
                "message", "중지 요청됨 — 아직 정리 중입니다. 잠시 후 다시 시도/확인하세요."));
    }
}
