package com.algolist.backend.user.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// startDate부터 endDate까지의 solution 풀이 수 조회를 위한 Dto
public class SolutionActivityResponseDto {
	private LocalDate startDate;
	private LocalDate endDate;
	private List<SolutionActivityDayDto> days;
}
