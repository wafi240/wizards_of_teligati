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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class MigrationV4Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Migration V4: Verify columns in jobs table")
    void verifyJobsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'jobs'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "jobs must contain id");
        assertTrue(columnNames.contains("title"), "jobs must contain title");
        assertTrue(columnNames.contains("company"), "jobs must contain company");
        assertTrue(columnNames.contains("category"), "jobs must contain category");
        assertTrue(columnNames.contains("organization"), "jobs must contain organization");
        assertTrue(columnNames.contains("duration"), "jobs must contain duration");
        assertTrue(columnNames.contains("skills_required"), "jobs must contain skills_required");
        assertTrue(columnNames.contains("min_cgpa"), "jobs must contain min_cgpa");
        assertTrue(columnNames.contains("status"), "jobs must contain status");
        assertTrue(columnNames.contains("created_at"), "jobs must contain created_at");
        assertTrue(columnNames.contains("updated_at"), "jobs must contain updated_at");
    }

    @Test
    @DisplayName("Migration V4: Verify columns in projects table")
    void verifyProjectsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'projects'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "projects must contain id");
        assertTrue(columnNames.contains("name"), "projects must contain name");
        assertTrue(columnNames.contains("type"), "projects must contain type");
        assertTrue(columnNames.contains("owner_dept"), "projects must contain owner_dept");
        assertTrue(columnNames.contains("contributors_count"), "projects must contain contributors_count");
        assertTrue(columnNames.contains("paper_doi"), "projects must contain paper_doi");
        assertTrue(columnNames.contains("status"), "projects must contain status");
        assertTrue(columnNames.contains("created_at"), "projects must contain created_at");
        assertTrue(columnNames.contains("updated_at"), "projects must contain updated_at");
    }

    @Test
    @DisplayName("Migration V4: Verify research_proposals and applications tables exist")
    void verifyNewDomainTablesIntegrity() {
        List<Map<String, Object>> proposalColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'research_proposals'"
        );
        assertFalse(proposalColumns.isEmpty(), "research_proposals table must exist");

        List<Map<String, Object>> appColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'applications'"
        );
        assertFalse(appColumns.isEmpty(), "applications table must exist");
    }
}
