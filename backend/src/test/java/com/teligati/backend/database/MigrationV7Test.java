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
class MigrationV7Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Migration V7: Verify reference_requests table schema integrity")
    void verifyReferenceRequestsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'reference_requests'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "reference_requests must contain id");
        assertTrue(columnNames.contains("student_id"), "reference_requests must contain student_id");
        assertTrue(columnNames.contains("recipient_id"), "reference_requests must contain recipient_id");
        assertTrue(columnNames.contains("recipient_role"), "reference_requests must contain recipient_role");
        assertTrue(columnNames.contains("purpose"), "reference_requests must contain purpose");
        assertTrue(columnNames.contains("target_institution"), "reference_requests must contain target_institution");
        assertTrue(columnNames.contains("target_program"), "reference_requests must contain target_program");
        assertTrue(columnNames.contains("deadline"), "reference_requests must contain deadline");
        assertTrue(columnNames.contains("personal_message"), "reference_requests must contain personal_message");
        assertTrue(columnNames.contains("supporting_info"), "reference_requests must contain supporting_info");
        assertTrue(columnNames.contains("status"), "reference_requests must contain status");
        assertTrue(columnNames.contains("recommendation_text"), "reference_requests must contain recommendation_text");
        assertTrue(columnNames.contains("response_notes"), "reference_requests must contain response_notes");
        assertTrue(columnNames.contains("created_at"), "reference_requests must contain created_at");
        assertTrue(columnNames.contains("updated_at"), "reference_requests must contain updated_at");
    }
}
