package com.algolist.backend.problem.batch.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FailedProblemDao {
    private final JdbcTemplate jdbcTemplate;

    public FailedProblemDao(@Qualifier("batchSystemDataSource") javax.sql.DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public int insertFailedProblem(String site, String number, String fileUrl, String reason) {
        return jdbcTemplate.update(
                "INSERT INTO failed_problems (site, number, file_url, reason) VALUES (?, ?, ?, ?)",
                site, number, fileUrl, reason);
    }
}
