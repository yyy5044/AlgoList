package com.algolist.backend.problem.batch.common;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 수집 중 끝내 실패한 "페이지(offset 구간)"를 기록하는 매퍼.
 * 나중에 이 기록(status='PENDING')만 골라 재시도하는 잡을 붙일 수 있다.
 */
@Mapper
public interface FailedPageDao {

    int insertFailedPage(@Param("source") String source,
                         @Param("pageOffset") long pageOffset,
                         @Param("pageSize") int pageSize,
                         @Param("reason") String reason);
}
