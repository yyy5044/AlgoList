package com.algolist.backend.db;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class BatchSystemDBConfig {

    @Bean
    @BatchDataSource
    @ConfigurationProperties(prefix = "spring.datasource.batch-system")
    DataSource batchSystemDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    PlatformTransactionManager batchSystemTransactionManager(@Qualifier("batchSystemDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
