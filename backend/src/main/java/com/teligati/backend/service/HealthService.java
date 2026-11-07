package com.teligati.backend.service;

import com.teligati.backend.dto.HealthResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.lang.management.ManagementFactory;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final DataSource dataSource;

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String appVersion;

    private long startTime;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        this.startTime = System.currentTimeMillis();
    }

    public HealthResponse checkHealth() {
        String dbStatus = checkDatabaseConnection();
        String overallStatus = "UP".equals(dbStatus) ? "UP" : "DEGRADED";
        long uptime = System.currentTimeMillis() - startTime;

        return HealthResponse.builder()
                .status(overallStatus)
                .database(dbStatus)
                .version(appVersion)
                .uptimeMs(uptime)
                .build();
    }

    private String checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            log.warn("Database health probe failed: {}", ex.getMessage());
            return "DOWN";
        }
    }
}
