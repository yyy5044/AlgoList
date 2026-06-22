package com.algolist.backend.problem.translation;

public interface TranslationService {
	TranslatedProblemDto getTranslatedProblem(Long problemId);
}
