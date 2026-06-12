package com.algolist.backend.reminder;

import java.time.LocalDate;

import com.algolist.backend.problem.UserProblemDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// UserProblemDto를 상속, 풀이 기한인 reviewDueDate값만 추가
public class ReminderProblemDto extends UserProblemDto {
	private LocalDate reviewDueDate;
}
