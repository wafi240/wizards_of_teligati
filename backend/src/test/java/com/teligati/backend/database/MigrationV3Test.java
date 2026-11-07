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
class MigrationV3Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Migration V1..V3: Verify all expected columns in profiles table exist")
    void verifyProfilesTableSchemaIntegrity() {
        // Query column names from database metadata
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type FROM information_schema.columns WHERE LOWER(table_name) = 'profiles'"
        );

        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        // Baseline V1 columns
        assertTrue(columnNames.contains("id"), "profiles must contain id");
        assertTrue(columnNames.contains("name"), "profiles must contain name");
        assertTrue(columnNames.contains("department"), "profiles must contain department");
        assertTrue(columnNames.contains("batch"), "profiles must contain batch");
        assertTrue(columnNames.contains("current_company"), "profiles must contain current_company");
        assertTrue(columnNames.contains("user_id"), "profiles must contain user_id");

        // V3 expansion columns
        assertTrue(columnNames.contains("bio"), "profiles must contain bio");
        assertTrue(columnNames.contains("skills"), "profiles must contain skills");
        assertTrue(columnNames.contains("projects"), "profiles must contain projects");
        assertTrue(columnNames.contains("github_url"), "profiles must contain github_url");
        assertTrue(columnNames.contains("linkedin_url"), "profiles must contain linkedin_url");
        assertTrue(columnNames.contains("cgpa"), "profiles must contain cgpa");
        assertTrue(columnNames.contains("specialization"), "profiles must contain specialization");
        assertTrue(columnNames.contains("clubs"), "profiles must contain clubs");
        assertTrue(columnNames.contains("designation"), "profiles must contain designation");
        assertTrue(columnNames.contains("research_focus"), "profiles must contain research_focus");
        assertTrue(columnNames.contains("lab_name"), "profiles must contain lab_name");
        assertTrue(columnNames.contains("publications_count"), "profiles must contain publications_count");
        assertTrue(columnNames.contains("is_actively_hiring"), "profiles must contain is_actively_hiring");
        assertTrue(columnNames.contains("contact_email"), "profiles must contain contact_email");
    }

    @Test
    @DisplayName("Migration V1..V3: Foreign key constraint between profiles and users is preserved")
    void verifyUserForeignKeyIntegrity() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM profiles p JOIN users u ON p.user_id = u.id",
                Integer.class
        );
        assertNotNull(count);
        assertTrue(count >= 3, "At least 3 seeded profiles should be correctly linked to users");
    }
}
