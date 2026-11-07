package com.teligati.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teligati.backend.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================================================
    // 1. LOGIN TESTS
    // =========================================================================

    @Test
    @DisplayName("Login with valid student roll should succeed and return tokens")
    void loginWithStudentRollShouldSucceed() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("2307003")
                .password("demo")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.kuetId").value("2307003"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("Login with valid alumni institutional email should succeed")
    void loginWithAlumniEmailShouldSucceed() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("tanvir.munich@kuet.ac.bd")
                .password("demo")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("tanvir.munich@kuet.ac.bd"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ALUMNI"));
    }

    @Test
    @DisplayName("Login with valid faculty email should succeed")
    void loginWithFacultyEmailShouldSucceed() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("azharul@cse.kuet.ac.bd")
                .password("demo")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("azharul@cse.kuet.ac.bd"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_TEACHER"));
    }

    @Test
    @DisplayName("Login with incorrect password should return 401 Unauthorized")
    void loginWithWrongPasswordShouldFail() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("2307003")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Login with non-existent identifier should return 401 Unauthorized")
    void loginWithNonExistentIdentifierShouldFail() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("nonexistent@kuet.ac.bd")
                .password("somepassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    // =========================================================================
    // 2. REGISTRATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Student registration with valid 7-digit roll and institutional email should succeed")
    void studentRegistrationShouldSucceed() throws Exception {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .roll("2307088")
                .name("New Student")
                .email("student2307088@stud.kuet.ac.bd")
                .department("Computer Science and Engineering")
                .password("studentPass123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Student registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.kuetId").value("2307088"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("Student registration with duplicate roll should be rejected")
    void studentRegistrationDuplicateRollShouldFail() throws Exception {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .roll("2307003") // already registered in seeder
                .name("Imposter Student")
                .email("imposter@stud.kuet.ac.bd")
                .department("CSE")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Student registration with non-7-digit roll should be rejected by validation")
    void studentRegistrationInvalidRollShouldFail() throws Exception {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .roll("12345") // Only 5 digits
                .name("Invalid Roll Student")
                .email("invalid@stud.kuet.ac.bd")
                .department("CSE")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Student registration with non-KUET email domain should be rejected")
    void studentRegistrationNonKuetEmailShouldFail() throws Exception {
        StudentRegisterRequest request = StudentRegisterRequest.builder()
                .roll("2307090")
                .name("External Email Student")
                .email("external@gmail.com") // Non-KUET email
                .department("CSE")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Alumni registration without valid invitation token should be forbidden")
    void alumniRegistrationWithoutTokenShouldFail() throws Exception {
        AlumniRegisterRequest request = AlumniRegisterRequest.builder()
                .name("Self Assigned Alumni")
                .email("fake.alumni@kuet.ac.bd")
                .department("CSE")
                .password("password123")
                .invitationToken("INVALID-TOKEN-999")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/alumni")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Alumni registration with valid invitation token should succeed and mark token used")
    void alumniRegistrationWithValidTokenShouldSucceed() throws Exception {
        AlumniRegisterRequest request = AlumniRegisterRequest.builder()
                .name("Authorized Alumni")
                .email("verified.alumni@kuet.ac.bd")
                .department("Computer Science and Engineering")
                .password("alumniSecret123")
                .invitationToken("ALUMNI-KUET-2026-TOKEN")
                .build();

        // First attempt succeeds
        mockMvc.perform(post("/api/v1/auth/register/alumni")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ALUMNI"));

        // Second attempt with the SAME claimed token MUST fail
        AlumniRegisterRequest request2 = AlumniRegisterRequest.builder()
                .name("Another Alumni")
                .email("another.alumni@kuet.ac.bd")
                .department("CSE")
                .password("alumniSecret123")
                .invitationToken("ALUMNI-KUET-2026-TOKEN")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/alumni")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invitation token has already been claimed"));
    }

    @Test
    @DisplayName("Faculty registration with valid authorization token should succeed")
    void facultyRegistrationWithValidTokenShouldSucceed() throws Exception {
        FacultyRegisterRequest request = FacultyRegisterRequest.builder()
                .name("Authorized Faculty")
                .email("verified.faculty@cse.kuet.ac.bd")
                .department("Computer Science and Engineering")
                .designation("Associate Professor")
                .password("facultySecret123")
                .invitationToken("FACULTY-KUET-2026-TOKEN")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/faculty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_TEACHER"));
    }

    // =========================================================================
    // 3. TOKEN REFRESH, ROTATION & THEFT REUSE DETECTION
    // =========================================================================

    @Test
    @DisplayName("Refresh token should rotate token and return new access and refresh tokens")
    void tokenRefreshAndRotationShouldSucceed() throws Exception {
        // 1. Log in to obtain tokens
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("2307003")
                .password("demo")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String initialRefreshToken = loginJson.get("data").get("refreshToken").asText();
        assertNotNull(initialRefreshToken);

        // 2. Refresh with the initial refresh token
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(initialRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = refreshJson.get("data").get("refreshToken").asText();

        // 3. New refresh token must be different from initial one (token rotation)
        assertNotEquals(initialRefreshToken, rotatedRefreshToken);
    }

    @Test
    @DisplayName("Attempting to reuse an old revoked refresh token should trigger theft detection and revoke all sessions")
    void tokenReuseDetectionShouldRevokeAllSessions() throws Exception {
        // 1. Log in
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("2307003")
                .password("demo")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token1 = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // 2. Refresh once (token1 becomes revoked, replaced by token2)
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(token1)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token2 = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // 3. MALICIOUS ACTOR ATTEMPTS TO REPLAY token1 (Token Reuse!)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(token1).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token reuse detected. All active sessions have been revoked for your security."));

        // 4. Because theft was detected, token2 must ALSO now be revoked (all sessions killed)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(token2).build())))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 4. LOGOUT & CURRENT USER (/me) TESTS
    // =========================================================================

    @Test
    @DisplayName("Logout should revoke session and prevent subsequent refresh")
    void logoutShouldRevokeSession() throws Exception {
        // Log in
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("2307003")
                .password("demo")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("refreshToken").asText();

        // Call Logout with Bearer token & refresh token body
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(refreshToken).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Refresh attempt should now fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(refreshToken).build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid Bearer token should return user profile")
    void getCurrentUserWithValidBearerTokenShouldSucceed() throws Exception {
        // Log in
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("2307003")
                .password("demo")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // Call /me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kuetId").value("2307003"))
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without Bearer token should return 401 Unauthorized")
    void getCurrentUserWithoutTokenShouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }
}
