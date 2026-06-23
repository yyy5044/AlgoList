package com.algolist.backend.reminder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

	private static final Set<String> ALLOWED_GRADES = Set.of("RED", "YELLOW", "GREEN");

	private final ReminderDao reminderDao;

	@Override
	public List<ReminderProblemDto> selectTodayReminders(Long userId) {
		LocalDate today = LocalDate.now();
		List<ReminderProblemDto> reminders = new ArrayList<>();

		// userId 기반으로 ReminderProblemDto 목록을 반환
		for (ReminderProblemDto item : reminderDao.selectReviewCandidates(userId)) {
			// 객체의 grade와 last_solved_date를 이용해 다시 문제를 풀어야 하는 날짜가 언제인지 계산
			LocalDate reviewDueDate = ReminderSchedule.getReviewDueDate(item.getGrade(), item.getLastSolvedDate());
			item.setReviewDueDate(reviewDueDate);

			// 복습 날짜가 오늘이거나 이미 지나갔다면 다시 풀어야하는 문제 리스트에 문제를 추가
			if (ReminderSchedule.isDueTodayOrEarlier(reviewDueDate, today)) {
				reminders.add(item);
			}
		}

		// 오늘 복습해야 하는 문제를 가장 위로 올리고, 이후 RED-YELLOW, 복습 날짜, problem_id 순으로 정렬
		reminders.sort(Comparator
				.comparingInt((ReminderProblemDto item) -> getDueDatePriority(item, today))
				.thenComparingInt(item -> ReminderSchedule.getGradePriority(item.getGrade()))
				.thenComparing(ReminderProblemDto::getReviewDueDate)
				.thenComparing(ReminderProblemDto::getUserProblemId));

		return reminders;
	}

	// 오늘 복습해야 하는 문제인지 확인
	private int getDueDatePriority(ReminderProblemDto item, LocalDate today) {
		return today.equals(item.getReviewDueDate()) ? 0 : 1;
	}

	@Override
	@Transactional
	// Grade 값 갱신 후 바뀐 문제 Dto 다시 가져오기
	public ReminderProblemDto updateGrade(Long userId, Long userProblemId, UpdateGradeRequestDto request) {
		String grade = validateGrade(request);

		int updated = reminderDao.updateGrade(userId, userProblemId, grade);
		if (updated != 1) {
			return null;
		}

		ReminderProblemDto updatedProblem = reminderDao.selectOne(userId, userProblemId);
		if (updatedProblem == null) {
			return null;
		}

		// 바뀐 등급 기준으로 복습 일자 다시 설정
		updatedProblem.setReviewDueDate(
				ReminderSchedule.getReviewDueDate(updatedProblem.getGrade(), updatedProblem.getLastSolvedDate()));

		return updatedProblem;
	}

	// 설정된 등급이 RED, YELLOW, GREEN이 아니면 가져오지 못하도록 설정
	private String validateGrade(UpdateGradeRequestDto request) {
		if (request == null || request.getGrade() == null || request.getGrade().isBlank()) {
			throw new IllegalArgumentException("문제 등급을 선택해주세요.");
		}

		String grade = request.getGrade().trim().toUpperCase(Locale.ROOT);
		if (!ALLOWED_GRADES.contains(grade)) {
			throw new IllegalArgumentException("지원하지 않는 문제 등급입니다.");
		}

		return grade;
	}
}
