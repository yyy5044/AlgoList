package com.algolist.backend.reminder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

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

		// RED-YELLOW, 복습 날짜, problem_id 순으로 리스트를 다시 정렬 후 반환(RED = 0, YELLOW = 1)
		reminders.sort(Comparator
				.comparingInt((ReminderProblemDto item) -> ReminderSchedule.getGradePriority(item.getGrade()))
				.thenComparing(ReminderProblemDto::getReviewDueDate)
				.thenComparing(ReminderProblemDto::getUserProblemId));

		return reminders;
	}
}
