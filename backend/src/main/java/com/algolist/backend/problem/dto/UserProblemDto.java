package com.algolist.backend.problem.dto;


import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProblemDto {
	private Long userProblemId;
	private Long userId;
	private String grade;
	private Integer solveCount;
	private LocalDate lastSolvedDate;
	private ProblemDto problem;
}
