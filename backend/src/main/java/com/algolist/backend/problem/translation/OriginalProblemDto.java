package com.algolist.backend.problem.translation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OriginalProblemDto {
	private Long problemId;
	private String originalTitle;
	private String originalDescription;
}
