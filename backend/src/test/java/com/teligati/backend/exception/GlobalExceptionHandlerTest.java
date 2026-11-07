package com.teligati.backend.exception;

import com.teligati.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Bypass security filters to focus test on exception handler
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/validate")
        public ApiResponse<String> testValidation(@Valid @RequestBody SampleRequest request) {
            return ApiResponse.success(request.getName());
        }

        @GetMapping("/not-found")
        public ApiResponse<String> testNotFound() {
            throw new ResourceNotFoundException("TestItem", "id", 999);
        }
    }

    static class SampleRequest {
        @NotBlank(message = "Name cannot be blank")
        private String name;

        public SampleRequest() {}

        public SampleRequest(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    @DisplayName("MethodArgumentNotValidException should return 400 Bad Request with field error details")
    void validationFailureShouldReturn400() throws Exception {
        String invalidJson = "{\"name\":\"\"}";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid request payload"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.data.details[0]").value("name: Name cannot be blank"));
    }

    @Test
    @DisplayName("Malformed JSON should return 400 Bad Request with MALFORMED_JSON code")
    void malformedJsonShouldReturn400() throws Exception {
        String malformedJson = "{\"name\": ";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("MALFORMED_JSON"));
    }

    @Test
    @DisplayName("ResourceNotFoundException should return 404 Not Found with descriptive message")
    void resourceNotFoundShouldReturn404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("TestItem not found with id: '999'"));
    }
}
