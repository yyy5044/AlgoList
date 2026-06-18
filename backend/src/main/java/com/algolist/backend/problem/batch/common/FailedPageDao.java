package com.algolist.backend.problem.batch.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FailedPageDao {

    private final JdbcTemplate jdbcTemplate;

    public FailedPageDao(@Qualifier("batchSystemDataSource") javax.sql.DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public int insertFailedPage(String source, long pageOffset, int pageSize, String reason) {
        return jdbcTemplate.update(
                "INSERT INTO failed_pages (source, page_offset, page_size, reason) VALUES (?, ?, ?, ?)",
                source, pageOffset, pageSize, reason);
    }
}
