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
class MigrationV6Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Migration V6: Verify extended columns in projects table")
    void verifyProjectsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'projects'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("positions_count"), "projects must contain positions_count");
        assertTrue(columnNames.contains("deadline"), "projects must contain deadline");
        assertTrue(columnNames.contains("preferred_background"), "projects must contain preferred_background");
        assertTrue(columnNames.contains("eligibility_criteria"), "projects must contain eligibility_criteria");
        assertTrue(columnNames.contains("duration"), "projects must contain duration");
        assertTrue(columnNames.contains("skills_required"), "projects must contain skills_required");
        assertTrue(columnNames.contains("category"), "projects must contain category");
        assertTrue(columnNames.contains("min_cgpa"), "projects must contain min_cgpa");
    }

    @Test
    @DisplayName("Migration V6: Verify columns in research_papers table")
    void verifyResearchPapersTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'research_papers'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "research_papers must contain id");
        assertTrue(columnNames.contains("title"), "research_papers must contain title");
        assertTrue(columnNames.contains("abstract_text"), "research_papers must contain abstract_text");
        assertTrue(columnNames.contains("research_field"), "research_papers must contain research_field");
        assertTrue(columnNames.contains("authors"), "research_papers must contain authors");
        assertTrue(columnNames.contains("doi"), "research_papers must contain doi");
        assertTrue(columnNames.contains("teacher_id"), "research_papers must contain teacher_id");
        assertTrue(columnNames.contains("created_at"), "research_papers must contain created_at");
        assertTrue(columnNames.contains("updated_at"), "research_papers must contain updated_at");
    }

    @Test
    @DisplayName("Migration V6: Verify extended columns in applications table")
    void verifyApplicationsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'applications'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("feedback"), "applications must contain feedback");
        assertTrue(columnNames.contains("review_notes"), "applications must contain review_notes");
    }

    @Test
    @DisplayName("Migration V6: Verify columns in notifications table")
    void verifyNotificationsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'notifications'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "notifications must contain id");
        assertTrue(columnNames.contains("user_id"), "notifications must contain user_id");
        assertTrue(columnNames.contains("title"), "notifications must contain title");
        assertTrue(columnNames.contains("message"), "notifications must contain message");
        assertTrue(columnNames.contains("is_read"), "notifications must contain is_read");
        assertTrue(columnNames.contains("created_at"), "notifications must contain created_at");
    }
}
