package com.algolist.backend.db;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class AlgoListDBConfig {
	@Bean
	@Primary
	@ConfigurationProperties(prefix = "spring.datasource")
	DataSource algoListDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

    @Bean(name = "algoListTransactionManager")
    @Primary
    PlatformTransactionManager algoListTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
