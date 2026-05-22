package com.algolist.backend.problem;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class ProblemDto {

	private Long id;
	private String title;
	private String number;
	private String difficulty;
	private String site;
	private String link;
	private String grade;

	// DB: solve_count
	private Integer solveCount;

	// DB: last_solved_date
	private LocalDate lastSolvedDate;

	// 소유 사용자 (DB: user_id) - 응답에는 보통 불필요하지만 매핑/저장에 사용
	private Long userId;
	
	// problem_categories 테이블과 1:N. 매퍼 XML에서 collection으로 채운다.
	private List<String> category;
}
