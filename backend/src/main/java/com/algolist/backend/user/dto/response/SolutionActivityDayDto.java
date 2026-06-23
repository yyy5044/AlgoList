package com.algolist.backend.user.dto.response;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 한 날짜의 solution 풀이 수 조회를 위해 사용하는 Dto
public class SolutionActivityDayDto {
	private LocalDate date;
	private int count;
}
