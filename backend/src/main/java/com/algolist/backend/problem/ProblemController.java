package com.algolist.backend.problem;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

	// 로그인 구현 전까지 사용할 임시 사용자 id.
	// 로그인 구현 후 @AuthenticationPrincipal 등으로 현재 사용자 id를 꺼내도록 교체할 것.
	private static final Long DEV_USER_ID = 1L;

	private final ProblemService service;

//	// 초기 목록 조회: GET /api/problems
//	@GetMapping
//	public ResponseEntity<?> list() {
//		problemService.findAllByUserId(DEV_USER_ID);
//		return null;
//	}

	// 문제 추가: POST /api/problems
	// 프론트의 selectSearchResult가 보낸 result 객체를 받아 저장하고, 저장된 문제를 반환한다.
	@PostMapping
	public ResponseEntity<?> add(@RequestBody ProblemDto problem) {
	    ProblemDto saved = service.addProblem(DEV_USER_ID, problem);
	    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

//	// 개별 삭제: DELETE /api/problems/{id}
//	@DeleteMapping("/{id}")
//	public ResponseEntity<?> delete(@PathVariable Long id) {
//		problemService.delete(DEV_USER_ID, id);
//		return null;
//	}
}
