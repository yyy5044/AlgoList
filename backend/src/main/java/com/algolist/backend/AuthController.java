package com.algolist.backend;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

	@Autowired
	private UserMapper userMapper;

    // 로그인
    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData, HttpSession session) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        // DB에서 사용자 조회
        Map<String, Object> user = userMapper.findByUsername(username);

        if (user == null || !user.get("password").equals(password)) {
            return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 틀렸습니다."));
        }

        // 세션에 사용자 정보 저장
        session.setAttribute("userId", user.get("id"));
        session.setAttribute("username", user.get("username"));
        
        System.out.println(username+"님이 로그인했습니다.");
        return ResponseEntity.ok(Map.of("username", user.get("username")));
    }
    
    // 로그아웃
    @PostMapping("/api/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        session.invalidate();
        System.out.println(username + "님이 로그아웃 했습니다.");
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    // 로그인 상태 확인
    @GetMapping("/api/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "username", session.getAttribute("username")
        ));
    }
}