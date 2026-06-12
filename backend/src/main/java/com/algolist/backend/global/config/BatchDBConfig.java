package com.algolist.backend.global.config;

import javax.sql.DataSource;

import org.springframework.boot.batch.jdbc.autoconfigure.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchDBConfig {

    @Bean(defaultCandidate = false)
    @BatchDataSource
    @ConfigurationProperties(prefix = "spring.datasource.batch-system")
    DataSource batchDataSource() {
        return DataSourceBuilder.create().build();
    }
}
