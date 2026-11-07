package com.teligati.backend.service;

import com.teligati.backend.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Test
    @DisplayName("checkHealth returns UP when database connection is valid")
    void checkHealthShouldReturnUpWhenConnectionIsValid() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        HealthService healthService = new HealthService(dataSource);
        healthService.init();

        HealthResponse response = healthService.checkHealth();
        assertEquals("UP", response.getStatus());
        assertEquals("UP", response.getDatabase());
    }

    @Test
    @DisplayName("checkHealth returns DEGRADED and database DOWN when connection throws SQLException")
    void checkHealthShouldReturnDegradedWhenConnectionFails() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Simulated connection timeout to PostgreSQL"));

        HealthService healthService = new HealthService(dataSource);
        healthService.init();

        HealthResponse response = healthService.checkHealth();
        assertEquals("DEGRADED", response.getStatus());
        assertEquals("DOWN", response.getDatabase());
    }
}
