package com.algolist.backend.problem.batch.github;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GitHubReadme {
    private String fileUrl;
    private String content;
}
