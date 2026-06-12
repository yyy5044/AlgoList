package com.algolist.backend.reminder;

import java.util.List;

public interface ReminderService {
	List<ReminderProblemDto> selectTodayReminders(Long userId);
}
