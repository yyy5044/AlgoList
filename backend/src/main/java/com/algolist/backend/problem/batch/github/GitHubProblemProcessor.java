package com.algolist.backend.problem.batch.github;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.algolist.backend.problem.dto.ProblemDto;
import com.algolist.backend.problem.github.ImageConverter;
import com.algolist.backend.problem.github.ReadmeParser;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GitHubProblemProcessor implements ItemProcessor<String, ProblemDto> {
	private final ReadmeParser parser;
	private final ImageConverter imageConverter;
	
	@Override
	public @Nullable ProblemDto process(String readme) throws Exception {
		// 1. 문제 파싱
		ProblemDto problem = parser.parse(readme);
		if(problem == null) return null;
		
		// 2. 이미지 변환
		problem.setDescription(imageConverter.processImages(problem.getDescription()));
		return problem;
	}
	
}
