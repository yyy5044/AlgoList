package com.algolist.backend.problem;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.algolist.backend.auth.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

	private final ProblemService service;

	@GetMapping("/search")
	public ResponseEntity<?> search(@RequestParam String query) {
		return null;
	}
	
	// 초기 목록 조회: GET /api/problems
	@GetMapping
	@Operation(summary = "문제 전체 조회")
	public ResponseEntity<List<ProblemDto>> selectAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
		Long userId = userDetails.getUser().getUserId(); // 세션에서 userId 꺼내기
		
		List<ProblemDto> list = service.selectAll(userId);
		
		return ResponseEntity.ok().body(list);
	}
	
	// 문제 추가: POST /api/problems
	// 프론트의 selectSearchResult가 보낸 result 객체를 받아 저장하고, 저장된 문제를 반환한다.
	@PostMapping
	@Operation(summary = "문제 추가")
	public ResponseEntity<ProblemDto> add(@AuthenticationPrincipal CustomUserDetails userDetails,
								 		  @RequestBody ProblemDto problem) {
		Long userId = userDetails.getUser().getUserId();
		
	    ProblemDto saved = service.addProblem(userId, problem);
	    
	    URI location = UriComponentsBuilder
	            .fromUriString("/api/problems/{id}")   // 템플릿 (자리만 비워둠)
	            .buildAndExpand(saved.getId())          // {id} 자리에 값 채움
	            .toUri();
	    								
	    return ResponseEntity.created(location).body(saved);
	}

	// 개별 삭제: DELETE /api/problems/{id}
	@DeleteMapping("/{id}")
	@Operation(summary = "문제 삭제")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
								       @PathVariable Long id) {
		Long userId = userDetails.getUser().getUserId();
		
		boolean deleted = service.deleteProblem(userId, id);
		
		return deleted
				? ResponseEntity.noContent().build() // 204 No Content (성공)
				: ResponseEntity.notFound().build(); // 404 Not Found (실패)
	}
	
}
