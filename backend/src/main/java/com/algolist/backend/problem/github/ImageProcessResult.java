package com.algolist.backend.problem.github;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageProcessResult {
    private String html;           // 변환된 본문
    private boolean success;       // 전부 성공했는지
    private List<String> failedUrls; // 실패한 이미지 URL들
}
