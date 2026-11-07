package com.teligati.backend.controller;

import com.teligati.backend.dto.HealthResponse;
import com.teligati.backend.security.JwtAuthenticationFilter;
import com.teligati.backend.security.JwtService;
import com.teligati.backend.security.RestAccessDeniedHandler;
import com.teligati.backend.security.RestAuthenticationEntryPoint;
import com.teligati.backend.security.SecurityConfig;
import com.teligati.backend.service.HealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, JwtAuthenticationFilter.class})
class HealthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthService healthService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("GET /api/v1/health returns 200 OK when overall status is UP")
    void shouldReturn200WhenHealthy() throws Exception {
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .database("UP")
                .version("1.0.0-SNAPSHOT")
                .uptimeMs(1234L)
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("System is operational"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health returns 503 SERVICE_UNAVAILABLE when database is DOWN")
    void shouldReturn503WhenDatabaseDown() throws Exception {
        HealthResponse response = HealthResponse.builder()
                .status("DEGRADED")
                .database("DOWN")
                .version("1.0.0-SNAPSHOT")
                .uptimeMs(5678L)
                .build();

        when(healthService.checkHealth()).thenReturn(response);

        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("System is running in degraded state"))
                .andExpect(jsonPath("$.data.status").value("DEGRADED"))
                .andExpect(jsonPath("$.data.database").value("DOWN"));
    }
}
