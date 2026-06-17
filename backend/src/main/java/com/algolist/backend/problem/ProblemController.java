package com.algolist.backend.problem;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.algolist.backend.auth.CustomUserDetails;
import com.algolist.backend.problem.dto.ProblemDto;
import com.algolist.backend.problem.dto.UserProblemDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

	private final ProblemService service;
	
	// 초기 목록 조회: GET /api/problems
	@GetMapping
	public ResponseEntity<?> selectAllByUserId(@AuthenticationPrincipal CustomUserDetails userDetails) {
		Long userId = userDetails.getUser().getUserId(); // 세션에서 userId 꺼내기
		
		List<UserProblemDto> list = service.selectAllByUserId(userId);
		
		return (list != null) ? ResponseEntity.status(HttpStatus.OK).body(list) :
								ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}
	
	
	// 문제 검색 GET /api/problems/search?query=...
	@GetMapping("/search")
	public ResponseEntity<?> searchProblem(@RequestParam String query) {
		List<ProblemDto> problems = service.searchProblem(query);
		
		return ResponseEntity.status(HttpStatus.OK).body(problems);
	}
	

	// 문제 추가: POST /api/problems
	@PostMapping
	public ResponseEntity<?> insertUserProblem(@AuthenticationPrincipal CustomUserDetails userDetails,
								 		  @RequestBody ProblemDto problemDto) {
		Long userId = userDetails.getUser().getUserId();
		
	    UserProblemDto result = service.insertUserProblem(userId, problemDto.getProblemId());
	    								
	    return (result != null) ? ResponseEntity.status(HttpStatus.CREATED).body(result) :
	    						  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}

	// 개별 삭제: DELETE /api/problems/{id}
	@DeleteMapping("/{problemId}")
	public ResponseEntity<?> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
								       @PathVariable Long problemId) {
		Long userId = userDetails.getUser().getUserId();
		
		int deleted = service.deleteUserProblem(userId, problemId);
		
		return (deleted > 0) ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() :
				  			   ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}
	
	// 문제 목록 페이지 전달 : /api/problems/browse/{params}
	@GetMapping("/browse/{site}")
	public ResponseEntity<?> selectPage(@PathVariable String site, @RequestParam int page, @RequestParam int size) {
		
		List<ProblemDto> problems = service.selectPage(site, page, size);
		
		return (problems != null) ? ResponseEntity.status(HttpStatus.OK).body(problems) :
									ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}
	
	// 문제 상세 조회
	@GetMapping("/detail/{problemId}")
	public ResponseEntity<?> selectDescription(@PathVariable Long problemId) {
		
		String description = service.selectDescription(problemId);
		
		return (description != null) ? ResponseEntity.status(HttpStatus.OK).body(description) :
									   ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}
	
	
}
