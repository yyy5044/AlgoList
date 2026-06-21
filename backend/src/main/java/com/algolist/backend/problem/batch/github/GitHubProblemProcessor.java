package com.algolist.backend.problem.batch.github;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.algolist.backend.problem.batch.common.FailedProblemRecorder;
import com.algolist.backend.problem.dto.ProblemDto;
import com.algolist.backend.problem.github.ImageConverter;
import com.algolist.backend.problem.github.ImageProcessResult;
import com.algolist.backend.problem.github.ReadmeParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubProblemProcessor implements ItemProcessor<GitHubReadme, ProblemDto> {
	private final ReadmeParser parser;
	private final ImageConverter imageConverter;
	private final FailedProblemRecorder failedProblemRecorder;

	@Override
	public @Nullable ProblemDto process(GitHubReadme item) throws Exception {
		// 1. 문제 파싱
		ProblemDto problem = parser.parse(item.getContent());
		if (problem == null) {
			log.info("[배치] 파싱 실패 → 스킵: {}", item.getFileUrl());
			return null;
		}

		// 2. 이미지 변환
		log.info("[배치] 이미지 변환 시작: {} {}", problem.getSite(), problem.getNumber());
		ImageProcessResult imageProcessResult = imageConverter.processImages(problem.getDescription());

		if (!imageProcessResult.isSuccess()) {
			log.warn("[배치] 이미지 다운로드 실패 → 스킵: {} {}", problem.getSite(), problem.getNumber());
			failedProblemRecorder.record(problem.getSite(), problem.getNumber(), item.getFileUrl(), "[Failed Reason] 이미지 다운로드 실패");
			return null;
		}

		log.info("[배치] 처리 완료: {} {}", problem.getSite(), problem.getNumber());
		problem.setDescription(imageProcessResult.getHtml());
		return problem;
	}
	
}
