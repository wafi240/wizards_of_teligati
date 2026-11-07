package com.teligati.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring Boot application context bootstraps successfully,
        // Flyway migration executes against the test database, and all beans initialize.
    }
}
