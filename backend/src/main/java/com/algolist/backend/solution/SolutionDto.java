package com.algolist.backend.solution;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Solution 컬럼 값들을 가지고 있는 Dto
public class SolutionDto {
	private Long solutionId;
	private Long userProblemId;
	private String algorithm;
	private String type;
	private String language;
	// filename은 DB에 미포함, 확장자명을 통해 올바른 파일인지 검증하는 용도
	private String fileName;
	private String code;
	private LocalDateTime createdAt;
}
