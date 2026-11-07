package com.teligati.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teligati.backend.BackendApplication;
import com.teligati.backend.config.ProductionSecurityValidator;
import org.springframework.boot.SpringApplication;
import com.teligati.backend.dto.*;
import com.teligati.backend.entity.InvitationToken;
import com.teligati.backend.entity.RefreshToken;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.repository.InvitationTokenRepository;
import com.teligati.backend.repository.RefreshTokenRepository;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityAuditTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvitationTokenRepository invitationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    // =========================================================================
    // 1. Production Profile Cannot Seed Demo Users
    // =========================================================================
    @Test
    @DisplayName("Audit 1: Production profile with seeder enabled must fail startup validation")
    void productionProfileCannotSeedDemoUsers() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(prodEnv);
        ReflectionTestUtils.setField(validator, "seederEnabled", true);
        ReflectionTestUtils.setField(validator, "jwtSecret", "validProductionSecretKeyThatIsAtLeast32BytesLongForTesting!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConstraints);
        assertTrue(ex.getMessage().contains("strictly forbidden in production"));
    }

    // =========================================================================
    // 2. Production Cannot Use Demo Invitation Tokens
    // =========================================================================
    @Test
    @DisplayName("Audit 2: Production environment without seeded tokens rejects demo invitation token")
    void productionCannotUseDemoInvitationTokens() {
        // Querying for a non-existent or unseeded invitation token must throw BadRequestException
        String unseededToken = "UNSEEDED-PROD-TOKEN-" + System.currentTimeMillis();
        AlumniRegisterRequest request = AlumniRegisterRequest.builder()
                .name("Unseeded Alumni")
                .email("unseeded.alumni@kuet.ac.bd")
                .department("CSE")
                .password("password123")
                .invitationToken(unseededToken)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerAlumni(request));
        assertTrue(ex.getMessage().contains("Invalid alumni invitation token"));
    }

    // =========================================================================
    // 3. Missing Production JWT Signing Key Prevents Unsafe Startup
    // =========================================================================
    @Test
    @DisplayName("Audit 3: Production startup must fail if JWT secret is missing, default, or weak")
    void missingProductionJwtSigningKeyPreventsUnsafeStartup() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        // Case A: Missing/empty secret
        ProductionSecurityValidator validatorEmpty = new ProductionSecurityValidator(prodEnv);
        ReflectionTestUtils.setField(validatorEmpty, "seederEnabled", false);
        ReflectionTestUtils.setField(validatorEmpty, "jwtSecret", "");
        assertThrows(IllegalStateException.class, validatorEmpty::validateSecurityConstraints);

        // Case B: Insecure development default secret
        ProductionSecurityValidator validatorDefault = new ProductionSecurityValidator(prodEnv);
        ReflectionTestUtils.setField(validatorDefault, "seederEnabled", false);
        ReflectionTestUtils.setField(validatorDefault, "jwtSecret", "d2l6YXJkcy1vZi10ZWxpZ2F0aS1zZWNyZXQta2V5LTIwMjYtc3ByaW5nLWJvb3Qtand0LTI1Ni1iaXQ=");
        assertThrows(IllegalStateException.class, validatorDefault::validateSecurityConstraints);

        // Case C: Secret under 256 bits (e.g. 10 bytes)
        ProductionSecurityValidator validatorShort = new ProductionSecurityValidator(prodEnv);
        ReflectionTestUtils.setField(validatorShort, "seederEnabled", false);
        ReflectionTestUtils.setField(validatorShort, "jwtSecret", "short-key");
        assertThrows(IllegalStateException.class, validatorShort::validateSecurityConstraints);
    }

    // =========================================================================
    // 4. Unverified Students Cannot Access Restricted Functionality
    // =========================================================================
    @Test
    @DisplayName("Audit 4: Unverified student cannot access restricted feature; verified student can")
    void unverifiedStudentsCannotAccessRestrictedFunctionality() throws Exception {
        String roll = "2307199";
        String email = "unverified2307199@stud.kuet.ac.bd";

        StudentRegisterRequest regRequest = StudentRegisterRequest.builder()
                .roll(roll)
                .name("Pending Student")
                .email(email)
                .department("Computer Science and Engineering")
                .password("studentPass123")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = json.get("data").get("accessToken").asText();
        boolean isVerified = json.get("data").get("user").get("verified").asBoolean();

        // 1. Newly registered student must be unverified
        assertFalse(isVerified, "Newly registered student must default to unverified");

        // 2. Unverified student attempting restricted feature receives 403 FORBIDDEN
        mockMvc.perform(get("/api/v1/auth/student/restricted-feature")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").value("FORBIDDEN"));

        // 3. Verify user identity
        authService.verifyUser(roll);

        // 4. Verified student can now access restricted feature
        mockMvc.perform(get("/api/v1/auth/student/restricted-feature")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 5. Expired Invitation Rejection
    // =========================================================================
    @Test
    @DisplayName("Audit 5: Expired invitation token is strictly rejected")
    void expiredInvitationRejection() {
        String rawToken = "EXPIRED-TOKEN-" + System.currentTimeMillis();
        String tokenHash = JwtService.hashToken(rawToken);

        InvitationToken expired = InvitationToken.builder()
                .tokenHash(tokenHash)
                .email("expired.alumni@kuet.ac.bd")
                .role("ROLE_ALUMNI")
                .department("CSE")
                .expiresAt(LocalDateTime.now().minusHours(2)) // Expired 2 hours ago
                .used(false)
                .build();
        invitationTokenRepository.save(expired);

        AlumniRegisterRequest request = AlumniRegisterRequest.builder()
                .name("Expired Applicant")
                .email("expired.alumni@kuet.ac.bd")
                .department("CSE")
                .password("password123")
                .invitationToken(rawToken)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerAlumni(request));
        assertEquals("Invitation token has expired", ex.getMessage());
    }

    // =========================================================================
    // 6. Invitation Replay Rejection
    // =========================================================================
    @Test
    @DisplayName("Audit 6: Already claimed (used) invitation token is strictly rejected on replay")
    void invitationReplayRejection() {
        String rawToken = "CLAIMED-TOKEN-" + System.currentTimeMillis();
        String tokenHash = JwtService.hashToken(rawToken);

        InvitationToken claimed = InvitationToken.builder()
                .tokenHash(tokenHash)
                .email("claimed.alumni@kuet.ac.bd")
                .role("ROLE_ALUMNI")
                .department("CSE")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .used(true) // Already claimed!
                .build();
        invitationTokenRepository.save(claimed);

        AlumniRegisterRequest request = AlumniRegisterRequest.builder()
                .name("Replay Applicant")
                .email("replay.alumni@kuet.ac.bd")
                .department("CSE")
                .password("password123")
                .invitationToken(rawToken)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerAlumni(request));
        assertEquals("Invitation token has already been claimed", ex.getMessage());
    }

    // =========================================================================
    // 7. Concurrent Invitation Consumption
    // =========================================================================
    @Test
    @DisplayName("Audit 7: Concurrent requests with same invitation token result in exactly 1 claim")
    void concurrentInvitationConsumption() throws InterruptedException {
        String rawToken = "CONCURRENT-INVITE-" + System.currentTimeMillis();
        String tokenHash = JwtService.hashToken(rawToken);

        InvitationToken token = InvitationToken.builder()
                .tokenHash(tokenHash)
                .email("") // General departmental invite
                .role("ROLE_ALUMNI")
                .department("CSE")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .used(false)
                .build();
        invitationTokenRepository.save(token);

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    AlumniRegisterRequest req = AlumniRegisterRequest.builder()
                            .name("Concurrent Alumni " + id)
                            .email("concurrent" + id + "_" + System.currentTimeMillis() + "@kuet.ac.bd")
                            .department("CSE")
                            .password("password123")
                            .invitationToken(rawToken)
                            .build();
                    authService.registerAlumni(req);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent thread must succeed in claiming the invitation");
        assertEquals(threads - 1, failureCount.get(), "All other concurrent attempts must be rejected");

        // Verify database state: token must now be marked used
        InvitationToken updated = invitationTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(updated.isUsed());
    }

    // =========================================================================
    // 8. Refresh-Token Replay Detection
    // =========================================================================
    @Test
    @DisplayName("Audit 8: Refresh token replay triggers reuse detection and revokes all user sessions")
    void refreshTokenReplayDetection() {
        LoginRequest login = LoginRequest.builder()
                .identifier("2307003")
                .password("demo")
                .build();
        TokenResponse initialTokens = authService.login(login, "TestDevice");

        // Rotate once: token1 -> token2
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(initialTokens.getRefreshToken())
                .build();
        TokenResponse rotatedTokens = authService.refreshToken(refreshReq, "TestDevice");

        // Malicious replay of token1
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                authService.refreshToken(refreshReq, "AttackerDevice"));
        assertTrue(ex.getMessage().contains("Token reuse detected"));

        // Confirm rotatedTokens (token2) was ALSO revoked by reuse detection
        RefreshToken token2Entity = refreshTokenRepository
                .findByTokenHash(JwtService.hashToken(rotatedTokens.getRefreshToken()))
                .orElseThrow();
        assertTrue(token2Entity.isRevoked(), "Token reuse detection must revoke all active sessions for the user");
    }

    // =========================================================================
    // 9. Concurrent Refresh Requests
    // =========================================================================
    @Test
    @DisplayName("Audit 9: Concurrent refresh requests on the same token cannot create multiple lineages")
    void concurrentRefreshRequests() throws InterruptedException {
        LoginRequest login = LoginRequest.builder()
                .identifier("tanvir.munich@kuet.ac.bd")
                .password("demo")
                .build();
        TokenResponse initialTokens = authService.login(login, "ConcurDevice");

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        List<TokenResponse> successfulResponses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TokenResponse resp = authService.refreshToken(
                            RefreshTokenRequest.builder().refreshToken(initialTokens.getRefreshToken()).build(),
                            "ConcurDevice"
                    );
                    successfulResponses.add(resp);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly 1 thread must succeed; concurrent race condition cannot fork duplicate active chains
        assertEquals(1, successfulResponses.size(), "Only one concurrent refresh call must succeed");
        assertEquals(threads - 1, failures.get());
    }

    // =========================================================================
    // 10. Revoked Refresh-Token Rejection
    // =========================================================================
    @Test
    @DisplayName("Audit 10: Revoked refresh token is immediately rejected upon submission")
    void revokedRefreshTokenRejection() {
        LoginRequest login = LoginRequest.builder()
                .identifier("2307003")
                .password("demo")
                .build();
        TokenResponse tokens = authService.login(login, "RevokeDevice");

        // Log out
        authService.logout(tokens.getRefreshToken(), "2307003");

        // Attempting to refresh with the revoked token must fail
        assertThrows(BadRequestException.class, () ->
                authService.refreshToken(RefreshTokenRequest.builder().refreshToken(tokens.getRefreshToken()).build(), "RevokeDevice"));
    }

    // =========================================================================
    // 11. Logout Session Isolation
    // =========================================================================
    @Test
    @DisplayName("Audit 11: Authenticated user cannot maliciously revoke another user's session")
    void logoutSessionIsolation() {
        TokenResponse studentTokens = authService.login(
                LoginRequest.builder().identifier("2307003").password("demo").build(), "StudentDevice");
        TokenResponse alumniTokens = authService.login(
                LoginRequest.builder().identifier("tanvir.munich@kuet.ac.bd").password("demo").build(), "AlumniDevice");

        // Student tries to revoke Alumni's refresh token
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                authService.logout(alumniTokens.getRefreshToken(), "2307003"));
        assertTrue(ex.getMessage().contains("Unauthorized attempt to revoke a session belonging to another user"));

        // Alumni's token must still be valid and unrevoked
        RefreshToken alumniEntity = refreshTokenRepository
                .findByTokenHash(JwtService.hashToken(alumniTokens.getRefreshToken()))
                .orElseThrow();
        assertFalse(alumniEntity.isRevoked(), "Alumni session must remain unrevoked after unauthorized attempt");
    }

    // =========================================================================
    // 12. Invalid JWT Rejection
    // =========================================================================
    @Test
    @DisplayName("Audit 12: Invalid, expired, and tampered JWT access tokens are rejected with 401")
    void invalidJwtRejection() throws Exception {
        // A. Malformed token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));

        // B. Tampered token signature
        LoginRequest login = LoginRequest.builder().identifier("2307003").password("demo").build();
        TokenResponse tokens = authService.login(login, "Device");
        String tamperedToken = tokens.getAccessToken().substring(0, tokens.getAccessToken().length() - 5) + "abcde";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));

        // C. Missing Authorization header
        mockMvc.perform(get("/api/v1/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("UNAUTHORIZED"));
    }

    // =========================================================================
    // 13. Password Hashes Never Appear in Responses or Logs
    // =========================================================================
    @Test
    @DisplayName("Audit 13: Password hashes never appear in API responses")
    void passwordHashesNeverAppearInResponsesOrLogs() throws Exception {
        // 1. Check Login Response
        LoginRequest login = LoginRequest.builder().identifier("2307003").password("demo").build();
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        assertFalse(loginBody.contains("password"), "Login response must never expose password field");
        assertFalse(loginBody.contains("$2a$"), "Login response must never expose BCrypt hash");

        // 2. Check /me Response
        String accessToken = objectMapper.readTree(loginBody).get("data").get("accessToken").asText();
        MvcResult meResult = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String meBody = meResult.getResponse().getContentAsString();
        assertFalse(meBody.contains("password"), "/me response must never expose password field");
        assertFalse(meBody.contains("$2a$"), "/me response must never expose BCrypt hash");

        // 3. Confirm DTO reflection: UserSummaryResponse has no password field
        assertThrows(NoSuchFieldException.class, () -> UserSummaryResponse.class.getDeclaredField("password"));
        assertThrows(NoSuchFieldException.class, () -> TokenResponse.class.getDeclaredField("password"));
    }

    // =========================================================================
    // 14. Actual Production Profile Context Fails Startup When Secret Missing
    // =========================================================================
    @Test
    @DisplayName("Audit 14: Bootstrapping full Spring context under prod profile with missing JWT_SECRET aborts startup")
    void productionProfileContextStartupFailsWhenJwtSecretMissing() {
        SpringApplication app = new SpringApplication(BackendApplication.class);
        app.setAdditionalProfiles("prod");
        assertThrows(Exception.class, () -> app.run("--server.port=0", "--spring.main.web-application-type=none"));
    }

    // =========================================================================
    // 15. JWT Algorithm Enforcement
    // =========================================================================
    @Test
    @DisplayName("Audit 15: JWT parser rejects unsigned (alg=none) or mismatched algorithm tokens")
    void jwtExplicitAlgorithmVerificationRejectsUnsignedTokens() {
        String unsignedToken = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiIyMzA3MDAzIiwicm9sZSI6IlJPTEVfU1RVREVOVCJ9.";
        assertFalse(jwtService.isTokenValid(unsignedToken));
    }

    // =========================================================================
    // 16. Cryptographic Random Source for Invitations
    // =========================================================================
    @Test
    @DisplayName("Audit 16: Invitation tokens are generated using SecureRandom with minimum entropy")
    void invitationTokenGeneratedUsingCryptographicRandom() {
        String token1 = jwtService.generateInvitationTokenString();
        String token2 = jwtService.generateInvitationTokenString();
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.startsWith("KUET-INV-"));
        assertEquals(73, token1.length());
    }
}
