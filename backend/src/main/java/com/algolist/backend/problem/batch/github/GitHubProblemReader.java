package com.algolist.backend.problem.batch.github;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.stereotype.Component;

import com.algolist.backend.problem.batch.common.FailedPageRecorder;
import com.algolist.backend.problem.github.GitHubClient;

import lombok.RequiredArgsConstructor;

@Component
@StepScope
@RequiredArgsConstructor
public class GitHubProblemReader implements ItemStreamReader<String>{
	private final GitHubClient client;
	private final FailedPageRecorder failedPageRecorder;
 
	private List<String> queries;
	private int queryIndex = 0;
	
	private int page = 1;
	
	private List<String> fileUrls;
	private int fileIndex = 0;

	@Override
	public @Nullable String read() throws Exception {
	    while (queryIndex < queries.size()) {
	        // URL 목록이 비었으면 채우기
	        if (fileUrls == null || fileIndex >= fileUrls.size()) {
	            fileUrls = client.searchCode(queries.get(queryIndex), 100, page);
	            fileIndex = 0;

	            if (fileUrls.isEmpty()) {
	                // 이 검색어는 끝 → 다음 검색어로
	                queryIndex++;
	                page = 1;
	                continue;
	            }
	            page++;
	        }

	        // URL 하나 꺼내서 README 가져오기
	        String readme = client.fetchFileContent(fileUrls.get(fileIndex));
	        fileIndex++;
	        return readme;
	    }
	    return null;  // 검색어 다 썼으면 끝
	}
    
	@Override
	public void open(ExecutionContext ctx) {
	    // 1. 검색어 목록 세팅
		queries = Arrays.stream(BOJDifficulty.values())
                .map(d -> d.getSearchName() + " \"### 문제 설명\" filename:README.md")
                .toList();

	    // 2. 재시작이면 저장된 위치에서 이어서
	    if (ctx.containsKey("github.queryIndex")) {
	        queryIndex = ctx.getInt("github.queryIndex");
	        page = ctx.getInt("github.page");
	    }
	}
	
	@Override
	public void update(ExecutionContext ctx) {
	    // 청크 커밋될 때마다 지금 위치 저장
	    ctx.putInt("github.queryIndex", queryIndex);
	    ctx.putInt("github.page", page);
	}
}
