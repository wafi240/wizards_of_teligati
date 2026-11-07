package com.teligati.backend.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class MigrationV5Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Migration V5: Verify columns in wallets table")
    void verifyWalletsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'wallets'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "wallets must contain id");
        assertTrue(columnNames.contains("user_id"), "wallets must contain user_id");
        assertTrue(columnNames.contains("available_balance"), "wallets must contain available_balance");
        assertTrue(columnNames.contains("escrow_balance"), "wallets must contain escrow_balance");
        assertTrue(columnNames.contains("currency"), "wallets must contain currency");
        assertTrue(columnNames.contains("version"), "wallets must contain version");
        assertTrue(columnNames.contains("created_at"), "wallets must contain created_at");
        assertTrue(columnNames.contains("updated_at"), "wallets must contain updated_at");
    }

    @Test
    @DisplayName("Migration V5: Verify columns in escrows table")
    void verifyEscrowsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'escrows'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "escrows must contain id");
        assertTrue(columnNames.contains("escrow_reference"), "escrows must contain escrow_reference");
        assertTrue(columnNames.contains("title"), "escrows must contain title");
        assertTrue(columnNames.contains("amount"), "escrows must contain amount");
        assertTrue(columnNames.contains("status"), "escrows must contain status");
        assertTrue(columnNames.contains("payer_id"), "escrows must contain payer_id");
        assertTrue(columnNames.contains("payee_id"), "escrows must contain payee_id");
        assertTrue(columnNames.contains("created_at"), "escrows must contain created_at");
        assertTrue(columnNames.contains("updated_at"), "escrows must contain updated_at");
    }

    @Test
    @DisplayName("Migration V5: Verify extended columns in transactions table")
    void verifyTransactionsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'transactions'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "transactions must contain id");
        assertTrue(columnNames.contains("transaction_id"), "transactions must contain transaction_id");
        assertTrue(columnNames.contains("title"), "transactions must contain title");
        assertTrue(columnNames.contains("category"), "transactions must contain category");
        assertTrue(columnNames.contains("escrow_id"), "transactions must contain escrow_id");
        assertTrue(columnNames.contains("completed_at"), "transactions must contain completed_at");
    }
}
