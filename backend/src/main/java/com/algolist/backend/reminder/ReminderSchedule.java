package com.algolist.backend.reminder;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

public final class ReminderSchedule {

	// RED는 1일 뒤 복습, YELLOW는 7일 뒤 복습
	private static final Map<String, Integer> REVIEW_INTERVAL_DAYS = Map.of(
			"RED", 1,
			"YELLOW", 7);

	private ReminderSchedule() {
	}

	// 등급과 마지막에 풀었던 날짜를 기준으로 언제 다시 복습해야 하는지 LocalDate값을 반환
	public static LocalDate getReviewDueDate(String grade, LocalDate lastSolvedDate) {
		if (grade == null || lastSolvedDate == null) {
			return null;
		}

		Integer intervalDays = REVIEW_INTERVAL_DAYS.get(grade.toUpperCase(Locale.ROOT));
		if (intervalDays == null) {
			return null;
		}

		return lastSolvedDate.plusDays(intervalDays);
	}

	// reviewDueDate가 today거나 지나갔는지 확인
	public static boolean isDueTodayOrEarlier(LocalDate reviewDueDate, LocalDate today) {
		return reviewDueDate != null && today != null && !reviewDueDate.isAfter(today);
	}

	// grade 값에 따라 int값을 반환
	public static int getGradePriority(String grade) {
		if (grade == null) {
			return 2;
		}

		return switch (grade.toUpperCase(Locale.ROOT)) {
			case "RED" -> 0;
			case "YELLOW" -> 1;
			default -> 2;
		};
	}
}
