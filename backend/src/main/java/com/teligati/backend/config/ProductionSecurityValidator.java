package com.teligati.backend.config;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class ProductionSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);
    private static final String DEFAULT_DEV_JWT_SECRET = "d2l6YXJkcy1vZi10ZWxpZ2F0aS1zZWNyZXQta2V5LTIwMjYtc3ByaW5nLWJvb3Qtand0LTI1Ni1iaXQ=";

    private final Environment environment;

    @Value("${app.seeder.enabled:false}")
    private boolean seederEnabled;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecurityConstraints() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProduction = activeProfiles.contains("prod") || activeProfiles.contains("production");

        if (isProduction) {
            log.info("Production profile detected. Executing strict security validation...");

            // 1. Strict prohibition of demo accounts and seeders in production
            if (seederEnabled) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY CONFIGURATION ERROR: Demo data seeding (app.seeder.enabled=true) " +
                        "is strictly forbidden in production! Disable demo seeding immediately."
                );
            }

            // 2. Strict prohibition of missing or default JWT signing key
            if (jwtSecret == null || jwtSecret.isBlank() || DEFAULT_DEV_JWT_SECRET.equals(jwtSecret.trim())) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY CONFIGURATION ERROR: Production JWT signing key must be provided " +
                        "via the JWT_SECRET environment variable. Development default signing secret is strictly forbidden in production."
                );
            }

            // 3. Cryptographic key strength enforcement (minimum 256 bits = 32 bytes)
            byte[] keyBytes;
            try {
                keyBytes = Decoders.BASE64.decode(jwtSecret.trim());
                if (keyBytes.length < 32) {
                    keyBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                keyBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
            }

            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY CONFIGURATION ERROR: Production JWT secret key must be at least " +
                        "256 bits (32 bytes) long. Provided secret is only " + (keyBytes.length * 8) + " bits."
                );
            }

            log.info("Production security validation succeeded: seeder disabled, secure JWT key validated.");
        }
    }
}
