package com.algolist.backend.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
class DBConfigTest {

    // ── AlgoListDBConfig ─────────────────────────────────────────────────────
    @Autowired
    DataSource dataSource; // @Primary

    @Autowired
    PlatformTransactionManager algoListTransactionManager;

    // ── BatchSystemDBConfig ──────────────────────────────────────────────────
    @Autowired
    @Qualifier("batchSystemDataSource")
    DataSource batchSystemDataSource;

    @Autowired
    @Qualifier("batchSystemTransactionManager")
    PlatformTransactionManager batchSystemTransactionManager;

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AlgoList DataSource - 빈 등록 및 DB 연결 확인")
    void algoListDataSource_isConnected() throws Exception {
        assertThat(dataSource).isNotNull();
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.isValid(2)).isTrue();
            assertThat(conn.getCatalog()).isEqualTo("algolist");
        }
    }

    @Test
    @DisplayName("AlgoList TransactionManager - 빈 등록 확인")
    void algoListTransactionManager_isRegistered() {
        assertThat(algoListTransactionManager).isNotNull();
    }

    @Test
    @DisplayName("BatchSystem DataSource - 빈 등록 및 DB 연결 확인")
    void batchSystemDataSource_isConnected() throws Exception {
        assertThat(batchSystemDataSource).isNotNull();
        try (Connection conn = batchSystemDataSource.getConnection()) {
            assertThat(conn.isValid(2)).isTrue();
            assertThat(conn.getCatalog()).isEqualTo("batch_system");
        }
    }

    @Test
    @DisplayName("BatchSystem TransactionManager - 빈 등록 확인")
    void batchSystemTransactionManager_isRegistered() {
        assertThat(batchSystemTransactionManager).isNotNull();
    }
}
