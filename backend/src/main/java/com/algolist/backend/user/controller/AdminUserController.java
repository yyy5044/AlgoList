package com.algolist.backend.user.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algolist.backend.auth.CustomUserDetails;
import com.algolist.backend.problem.dto.UserProblemDto;
import com.algolist.backend.solution.SolutionDto;
import com.algolist.backend.user.dto.request.ReleaseSuspensionRequestDto;
import com.algolist.backend.user.dto.request.SuspendUserRequestDto;
import com.algolist.backend.user.dto.request.UpdateRoleRequestDto;
import com.algolist.backend.user.dto.response.SolutionActivityResponseDto;
import com.algolist.backend.user.dto.response.UserDetailDto;
import com.algolist.backend.user.dto.response.UserPageResponseDto;
import com.algolist.backend.user.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final AdminUserService adminUserService;

	@GetMapping
	// 전체 회원 목록 조회 요청(페이징)
	public ResponseEntity<UserPageResponseDto> selectAllUsers(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String searchType,
			@RequestParam(required = false) String keyword) {
		UserPageResponseDto users = adminUserService.selectUsers(page, size, status, searchType, keyword);

		return ResponseEntity.status(HttpStatus.OK).body(users);
	}

	@GetMapping("/{username}")
	// 특정 유저 상세정보 조회 요청
	public ResponseEntity<UserDetailDto> selectUser(@PathVariable String username) {
		UserDetailDto user = adminUserService.selectUser(username);

		if (user != null) {
			return ResponseEntity.status(HttpStatus.OK).body(user);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 조회된 정보가 없으므로 NOT_FOUND(404) 에러
		}
	}

	@GetMapping("/{username}/activity")
	// 특정 유저의 최근 1년간 풀이 기록 수 조회
	public ResponseEntity<SolutionActivityResponseDto> selectSolutionActivity(@PathVariable String username) {
		SolutionActivityResponseDto activity = adminUserService.selectSolutionActivity(username);

		if (activity != null) {
			return ResponseEntity.status(HttpStatus.OK).body(activity);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping("/{username}/problems")
	// 특정 유저 문제 목록 조회 요청
	public ResponseEntity<List<UserProblemDto>> selectUserProblems(@PathVariable String username) {
		List<UserProblemDto> problems = adminUserService.selectUserProblems(username);

		if (problems != null) {
			return ResponseEntity.status(HttpStatus.OK).body(problems);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping("/{username}/problems/{userProblemId}/solutions")
	// 유저의 특정 문제 풀이 목록 조회 요청
	public ResponseEntity<List<SolutionDto>> selectUserProblemSolutions(@PathVariable String username,
			@PathVariable Long userProblemId) {
		List<SolutionDto> solutions = adminUserService.selectUserProblemSolutions(username, userProblemId);

		if (solutions != null) {
			return ResponseEntity.status(HttpStatus.OK).body(solutions);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@GetMapping("/{username}/problems/{userProblemId}/solutions/{solutionId}")
	// 유저의 풀이 코드 조회 요청
	public ResponseEntity<SolutionDto> selectUserProblemSolution(@PathVariable String username,
			@PathVariable Long userProblemId, @PathVariable Long solutionId) {
		SolutionDto solution = adminUserService.selectUserProblemSolution(username, userProblemId, solutionId);

		if (solution != null) {
			return ResponseEntity.status(HttpStatus.OK).body(solution);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@DeleteMapping("/{username}")
	// 유저 삭제 요청
	public ResponseEntity<?> deleteUser(@PathVariable String username) {
		boolean success = adminUserService.deleteUser(username);

		if (success) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 삭제 완료했으므로 NO_CONTENT(204) 반환
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 조회된 정보가 없으므로 NOT_FOUND(404) 에러
		}
	}

	@PostMapping("/{username}/suspensions")
	// 유저 정지 요청
	public ResponseEntity<?> suspendUser(@PathVariable String username, @RequestBody SuspendUserRequestDto request,
			Authentication authentication) {
		boolean success = adminUserService.suspendUser(username, request, getCurrentUserId(authentication));

		if (success) {
			return ResponseEntity.status(HttpStatus.OK).build();
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PatchMapping("/{username}/suspensions/release")
	// 유저 정지 해제 요청
	public ResponseEntity<?> releaseUserSuspension(@PathVariable String username,
			@RequestBody ReleaseSuspensionRequestDto request, Authentication authentication) {
		boolean success = adminUserService.releaseUserSuspension(username, request, getCurrentUserId(authentication));

		if (success) {
			return ResponseEntity.status(HttpStatus.OK).build();
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PatchMapping("/{username}/role")
	// 유저 권한 변경 요청
	public ResponseEntity<?> updateUserRole(@PathVariable String username, @RequestBody UpdateRoleRequestDto request,
			Authentication authentication) {
		boolean success = adminUserService.updateUserRole(username, request, getCurrentUserId(authentication));

		if (success) {
			return ResponseEntity.status(HttpStatus.OK).build();
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	private Long getCurrentUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
			return userDetails.getUser().getUserId();
		}

		return null;
	}

}
