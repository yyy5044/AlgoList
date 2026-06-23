package com.algolist.backend.solution;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.algolist.backend.user.dto.response.SolutionActivityDayDto;
import com.algolist.backend.user.dto.response.SolutionActivityResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SolutionActivityService {

	private static final long ACTIVITY_DAYS = 365;

	private final SolutionDao solutionDao;

	// 최근 1년간의 풀이 기록 수와 시작일, 종료일 반환
	public SolutionActivityResponseDto selectActivity(Long userId) {
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = endDate.minusDays(ACTIVITY_DAYS - 1);
		List<SolutionActivityDayDto> days = solutionDao.selectActivityDays(userId, startDate, endDate);

		SolutionActivityResponseDto response = new SolutionActivityResponseDto();
		response.setStartDate(startDate);
		response.setEndDate(endDate);
		response.setDays(days);
		return response;
	}
}
