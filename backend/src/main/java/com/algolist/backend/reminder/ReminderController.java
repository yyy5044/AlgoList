package com.algolist.backend.reminder;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algolist.backend.auth.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

	private final ReminderService reminderService;

	@GetMapping("/today")
	// 오늘 날짜를 기준으로 last_solved_date + 문제 등급에 따른 복습 주기가 지난 문제들 표시
	public ResponseEntity<List<ReminderProblemDto>> selectTodayReminders(
			@AuthenticationPrincipal CustomUserDetails userDetails) {
		Long userId = userDetails.getUser().getUserId();

		return ResponseEntity.status(HttpStatus.OK).body(reminderService.selectTodayReminders(userId));
	}

	@PatchMapping("/{userProblemId}/grade")
	// Grade 값을 수정하고 수정한 Dto를 body에 실어서 보냄
	public ResponseEntity<ReminderProblemDto> updateGrade(@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long userProblemId, @RequestBody UpdateGradeRequestDto request) {
		Long userId = userDetails.getUser().getUserId();
		ReminderProblemDto updatedProblem = reminderService.updateGrade(userId, userProblemId, request);

		if (updatedProblem == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		return ResponseEntity.status(HttpStatus.OK).body(updatedProblem);
	}
}
