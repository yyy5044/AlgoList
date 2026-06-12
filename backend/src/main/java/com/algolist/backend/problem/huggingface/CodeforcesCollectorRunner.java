package com.algolist.backend.problem.huggingface;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeforcesCollectorRunner implements CommandLineRunner {

    private final CodeforcesCollector collector;

    @Override
    public void run(String... args) {
        log.info("=== HuggingFace Codeforces 데이터셋 미리보기 시작 ===");
        collector.수집(0, 3);
        log.info("=== 미리보기 종료 ===");
    }
}
