package com.algolist.backend.problem;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProblemDto {
	private Long problemId;
	private String title;
	private String number;
	private String difficulty;
	private String site;
	private String link;
	private List<String> category;
}
