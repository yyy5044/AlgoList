package com.algolist.backend.problem.translation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranslatedProblemDto {
	private Long problemId;
	private String translatedTitle;
	private String translatedDescription;
}
