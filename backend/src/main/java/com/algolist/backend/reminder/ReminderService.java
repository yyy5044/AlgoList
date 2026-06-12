package com.algolist.backend.reminder;

import java.util.List;

public interface ReminderService {
	List<ReminderProblemDto> selectTodayReminders(Long userId);

	ReminderProblemDto updateGrade(Long userId, Long userProblemId, UpdateGradeRequestDto request);
}
