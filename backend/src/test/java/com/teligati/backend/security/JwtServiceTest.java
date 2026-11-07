package com.teligati.backend.security;

import com.teligati.backend.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "d2l6YXJkcy1vZi10ZWxpZ2F0aS1zZWNyZXQta2V5LTIwMjYtc3ByaW5nLWJvb3Qtand0LTI1Ni1iaXQ=");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 2592000000L); // 30 days
    }

    @Test
    @DisplayName("Should generate valid JWT access token with correct claims")
    void shouldGenerateValidAccessToken() {
        User user = User.builder()
                .id(42L)
                .kuetId("2307003")
                .email("wafi2307003@stud.kuet.ac.bd")
                .name("Wafi")
                .department("CSE")
                .role("ROLE_STUDENT")
                .build();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("wafi2307003@stud.kuet.ac.bd", jwtService.extractSubject(token));
        assertEquals("ROLE_STUDENT", jwtService.extractRole(token));
        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Should extract subject when user has only kuetId and no email")
    void shouldExtractKuetIdWhenEmailIsNull() {
        User user = User.builder()
                .id(10L)
                .kuetId("2307099")
                .email(null)
                .name("Roll Only Student")
                .department("CSE")
                .role("ROLE_STUDENT")
                .build();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertEquals("2307099", jwtService.extractSubject(token));
    }

    @Test
    @DisplayName("Should reject invalid or malformed tokens")
    void shouldRejectMalformedToken() {
        assertFalse(jwtService.isTokenValid("not.a.valid.jwt.token"));
        assertFalse(jwtService.isTokenValid(""));
    }

    @Test
    @DisplayName("Should generate random refresh tokens and correct SHA-256 hashes")
    void shouldGenerateRefreshTokenAndConsistentHash() {
        String token1 = jwtService.generateRefreshTokenString();
        String token2 = jwtService.generateRefreshTokenString();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);

        String hash1 = JwtService.hashToken(token1);
        String hash2 = JwtService.hashToken(token1);
        assertEquals(hash1, hash2, "Hashing same token must produce identical SHA-256 hash");
        assertEquals(64, hash1.length(), "SHA-256 hex string must be 64 characters");
    }

    @Test
    @DisplayName("Expired token should be recognized as invalid")
    void expiredTokenShouldBeInvalid() {
        // Set expiration to 1 ms in the past
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);

        User user = User.builder()
                .id(1L)
                .kuetId("2307001")
                .email("test@stud.kuet.ac.bd")
                .role("ROLE_STUDENT")
                .build();

        String expiredToken = jwtService.generateAccessToken(user);
        assertFalse(jwtService.isTokenValid(expiredToken));
    }
}
