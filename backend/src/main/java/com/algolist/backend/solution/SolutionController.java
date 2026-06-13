package com.algolist.backend.solution;

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
import org.springframework.web.bind.annotation.RestController;

import com.algolist.backend.auth.CustomUserDetails;
import com.algolist.backend.problem.dto.UserProblemDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/solutions")
@RequiredArgsConstructor
public class SolutionController {

	private final SolutionService solutionService;

	@GetMapping("/{userProblemId}")
	// userProblemId를 통해 Solution 목록을 가져오기(userId로 검증)
	public ResponseEntity<?> selectSolutions(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long userProblemId) {
		Long userId = userDetails.getUser().getUserId();
		List<SolutionDto> solutions = solutionService.selectSolutions(userId, userProblemId);

		if (solutions != null) {
			return ResponseEntity.status(HttpStatus.OK).body(solutions);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping("/detail/{solutionId}")
	// solutionId를 통해 Solution 상세 정보 가져오기(userId로 검증)
	public ResponseEntity<?> selectSolution(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long solutionId) {
		Long userId = userDetails.getUser().getUserId();
		SolutionDto solution = solutionService.selectSolution(userId, solutionId);

		if (solution != null) {
			return ResponseEntity.status(HttpStatus.OK).body(solution);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PostMapping
	// Solution 추가하기
	public ResponseEntity<?> insertSolution(@AuthenticationPrincipal CustomUserDetails userDetails,
			@RequestBody SolutionDto solution) {
		Long userId = userDetails.getUser().getUserId();
		// 풀이를 추가한 뒤, 갱신된 문제 데이터를 다시 가져옴(풀이 횟수 증가, 최근에 푼 날짜 갱신)
		UserProblemDto updatedUserProblem = solutionService.insertSolution(userId, solution);

		if (updatedUserProblem != null) {
			return ResponseEntity.status(HttpStatus.CREATED).body(updatedUserProblem);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@DeleteMapping("/{solutionId}")
	// Solution 삭제하기
	public ResponseEntity<?> deleteSolution(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long solutionId) {
		Long userId = userDetails.getUser().getUserId();
		boolean success = solutionService.deleteSolution(userId, solutionId);

		if (success) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}
}
