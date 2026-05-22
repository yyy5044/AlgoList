package com.algolist.backend.problem;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

	// 로그인 구현 전까지 사용할 임시 사용자 id.
	// 로그인 구현 후 @AuthenticationPrincipal 등으로 현재 사용자 id를 꺼내도록 교체할 것.
	private static final Long DEV_USER_ID = 1L;

	private final ProblemService service;

	// 초기 목록 조회: GET /api/problems
	@GetMapping
	public ResponseEntity<?> selectAll() {
		List<ProblemDto> list = service.selectAll(DEV_USER_ID);
		return ResponseEntity.ok().body(list);
	}
	
	// 문제 추가: POST /api/problems
	// 프론트의 selectSearchResult가 보낸 result 객체를 받아 저장하고, 저장된 문제를 반환한다.
	@PostMapping
	public ResponseEntity<?> add(@RequestBody ProblemDto problem) {
	    ProblemDto saved = service.addProblem(DEV_USER_ID, problem);
	    
	    URI location = UriComponentsBuilder
	            .fromUriString("/api/problems/{id}")   // 템플릿 (자리만 비워둠)
	            .buildAndExpand(saved.getId())          // {id} 자리에 값 채움
	            .toUri();
	    								
	    return ResponseEntity.created(location).body(saved);
	}

	// 개별 삭제: DELETE /api/problems/{id}
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id) {
		boolean deleted = service.deleteProblem(DEV_USER_ID, id);
		
		return deleted
				? ResponseEntity.noContent().build() // 204 No Content (성공)
				: ResponseEntity.notFound().build(); // 404 Not Found (실패)
	}
}
