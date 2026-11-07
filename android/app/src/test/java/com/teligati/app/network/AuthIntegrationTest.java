package com.teligati.app.network;

import com.google.gson.Gson;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AuthRepository;
import com.teligati.app.network.model.AlumniRegisterRequest;
import com.teligati.app.network.model.FacultyRegisterRequest;
import com.teligati.app.network.model.RefreshTokenRequest;
import com.teligati.app.network.model.StudentRegisterRequest;
import com.teligati.app.network.model.TokenResponse;
import com.teligati.app.network.model.UserSummaryResponse;
import com.teligati.app.network.model.VerificationStatusResponse;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.*;

public class AuthIntegrationTest {
    private MockWebServer server;
    private SessionManager sessionManager;
    private AuthApiService apiService;
    private AuthRepository authRepository;
    private OkHttpClient okHttpClient;
    private Gson gson;
    private String baseUrl;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        SessionManager.setInstanceForTesting(sessionManager);

        baseUrl = server.url("/").toString();
        gson = new Gson();

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(sessionManager, baseUrl))
                .authenticator(new TokenAuthenticator(sessionManager, baseUrl))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(AuthApiService.class);
        authRepository = new AuthRepository(apiService, sessionManager, null, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testStudentRegisterSuccessAndSavesSession() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"accessToken\": \"mock-student-access-token\",\n" +
                "    \"refreshToken\": \"mock-student-refresh-token\",\n" +
                "    \"tokenType\": \"Bearer\",\n" +
                "    \"expiresIn\": 900,\n" +
                "    \"user\": {\n" +
                "      \"id\": 10,\n" +
                "      \"kuetId\": \"2307003\",\n" +
                "      \"email\": \"samun@stud.kuet.ac.bd\",\n" +
                "      \"name\": \"Samun Sadab Wafi\",\n" +
                "      \"role\": \"ROLE_STUDENT\",\n" +
                "      \"department\": \"CSE\",\n" +
                "      \"verified\": false\n" +
                "    }\n" +
                "  },\n" +
                "  \"message\": \"Student registered successfully\"\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TokenResponse> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        StudentRegisterRequest req = new StudentRegisterRequest(
                "2307003", "Samun Sadab Wafi", "samun@stud.kuet.ac.bd", "CSE", "pass123"
        );

        authRepository.registerStudent(req, new AuthRepository.Callback<TokenResponse>() {
            @Override
            public void onSuccess(TokenResponse result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorRef.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(resultRef.get());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("/api/v1/auth/register/student", recorded.getPath());
        assertEquals("POST", recorded.getMethod());

        assertEquals("mock-student-access-token", sessionManager.getAccessToken());
        assertEquals("mock-student-refresh-token", sessionManager.getRefreshToken());
        assertEquals("STUDENT", sessionManager.getUserRole());
        assertEquals("2307003", sessionManager.getUserId());
        assertEquals("Samun Sadab Wafi", sessionManager.getUserName());
        assertFalse(sessionManager.isVerified());
    }

    @Test
    public void testStudentLoginSuccessAndSavesSession() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"accessToken\": \"login-access-token\",\n" +
                "    \"refreshToken\": \"login-refresh-token\",\n" +
                "    \"tokenType\": \"Bearer\",\n" +
                "    \"expiresIn\": 900,\n" +
                "    \"user\": {\n" +
                "      \"id\": 1,\n" +
                "      \"kuetId\": \"2307003\",\n" +
                "      \"email\": \"samun@stud.kuet.ac.bd\",\n" +
                "      \"name\": \"Samun Sadab Wafi\",\n" +
                "      \"role\": \"ROLE_STUDENT\",\n" +
                "      \"department\": \"CSE\",\n" +
                "      \"verified\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"message\": \"Login successful\"\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TokenResponse> resultRef = new AtomicReference<>();

        authRepository.login("2307003", "demo", new AuthRepository.Callback<TokenResponse>() {
            @Override
            public void onSuccess(TokenResponse result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("/api/v1/auth/login", recorded.getPath());
        assertTrue(recorded.getBody().readUtf8().contains("2307003"));

        assertEquals("login-access-token", sessionManager.getAccessToken());
        assertEquals("login-refresh-token", sessionManager.getRefreshToken());
        assertEquals("STUDENT", sessionManager.getUserRole());
        assertTrue(sessionManager.isVerified());
    }

    @Test
    public void testAlumniRegisterWithInvitationToken() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"accessToken\": \"alumni-token\",\n" +
                "    \"refreshToken\": \"alumni-refresh\",\n" +
                "    \"tokenType\": \"Bearer\",\n" +
                "    \"expiresIn\": 900,\n" +
                "    \"user\": {\n" +
                "      \"id\": 2,\n" +
                "      \"kuetId\": null,\n" +
                "      \"email\": \"tanvir@kuet.ac.bd\",\n" +
                "      \"name\": \"Engr. Tanvir\",\n" +
                "      \"role\": \"ROLE_ALUMNI\",\n" +
                "      \"department\": \"CSE\",\n" +
                "      \"verified\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"message\": \"Alumni registered successfully\"\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        CountDownLatch latch = new CountDownLatch(1);
        AlumniRegisterRequest req = new AlumniRegisterRequest(
                "Engr. Tanvir", "tanvir@kuet.ac.bd", "CSE", "pass123", "DEMO-ALUMNI-INVITE-2026"
        );

        authRepository.registerAlumni(req, new AuthRepository.Callback<TokenResponse>() {
            @Override
            public void onSuccess(TokenResponse result) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        RecordedRequest recorded = server.takeRequest();
        assertEquals("/api/v1/auth/register/alumni", recorded.getPath());
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("DEMO-ALUMNI-INVITE-2026"));
        assertTrue(body.contains("tanvir@kuet.ac.bd"));

        assertEquals("ALUMNI", sessionManager.getUserRole());
        assertTrue(sessionManager.isVerified());
    }

    @Test
    public void testFacultyRegisterWithAuthorizationToken() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"accessToken\": \"faculty-token\",\n" +
                "    \"refreshToken\": \"faculty-refresh\",\n" +
                "    \"tokenType\": \"Bearer\",\n" +
                "    \"expiresIn\": 900,\n" +
                "    \"user\": {\n" +
                "      \"id\": 3,\n" +
                "      \"kuetId\": null,\n" +
                "      \"email\": \"azharul@cse.kuet.ac.bd\",\n" +
                "      \"name\": \"Prof. Azharul\",\n" +
                "      \"role\": \"ROLE_TEACHER\",\n" +
                "      \"department\": \"CSE\",\n" +
                "      \"verified\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"message\": \"Faculty registered successfully\"\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        CountDownLatch latch = new CountDownLatch(1);
        FacultyRegisterRequest req = new FacultyRegisterRequest(
                "Prof. Azharul", "azharul@cse.kuet.ac.bd", "CSE", "Professor", "pass123", "DEMO-FACULTY-AUTH-2026"
        );

        authRepository.registerFaculty(req, new AuthRepository.Callback<TokenResponse>() {
            @Override
            public void onSuccess(TokenResponse result) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        RecordedRequest recorded = server.takeRequest();
        assertEquals("/api/v1/auth/register/faculty", recorded.getPath());
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("DEMO-FACULTY-AUTH-2026"));
        assertTrue(body.contains("Professor"));

        assertEquals("TEACHER", sessionManager.getUserRole());
        assertTrue(sessionManager.isVerified());
    }

    @Test
    public void testVerificationStatusUnverifiedResponse() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"verified\": false,\n" +
                "    \"identifier\": \"2307003\",\n" +
                "    \"role\": \"ROLE_STUDENT\",\n" +
                "    \"status\": \"UNVERIFIED\",\n" +
                "    \"message\": \"Account is pending institutional verification.\",\n" +
                "    \"restrictedActions\": [\"APPLY_FOR_JOBS\", \"SUBMIT_RESEARCH_PROPOSALS\", \"REQUEST_ALUMNI_REFERENCES\"]\n" +
                "  },\n" +
                "  \"message\": \"Verification status retrieved successfully\"\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<VerificationStatusResponse> resultRef = new AtomicReference<>();

        authRepository.getVerificationStatus(new AuthRepository.Callback<VerificationStatusResponse>() {
            @Override
            public void onSuccess(VerificationStatusResponse result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        VerificationStatusResponse status = resultRef.get();

        assertFalse(status.isVerified());
        assertEquals("UNVERIFIED", status.getStatus());
        assertEquals(3, status.getRestrictedActions().size());
        assertTrue(status.getRestrictedActions().contains("APPLY_FOR_JOBS"));
        assertFalse(sessionManager.isVerified());
    }

    @Test
    public void testAuthInterceptorAttachesBearerToken() throws Exception {
        sessionManager.setAccessToken("active-session-jwt");

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        Request request = new Request.Builder()
                .url(server.url("/api/v1/auth/me"))
                .build();

        Response response = okHttpClient.newCall(request).execute();
        assertEquals(200, response.code());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("Bearer active-session-jwt", recorded.getHeader("Authorization"));
        assertEquals("application/json", recorded.getHeader("Accept"));
    }

    @Test
    public void testAuthInterceptorDoesNotSendTokenToExternalOrigin() throws Exception {
        sessionManager.setAccessToken("sensitive-backend-jwt");

        // Start a second server simulating an external third-party host
        MockWebServer externalServer = new MockWebServer();
        externalServer.start();
        externalServer.enqueue(new MockResponse().setResponseCode(200).setBody("external response"));

        try {
            Request externalRequest = new Request.Builder()
                    .url(externalServer.url("/external/api"))
                    .build();

            Response response = okHttpClient.newCall(externalRequest).execute();
            assertEquals(200, response.code());

            RecordedRequest recorded = externalServer.takeRequest();
            // Authorization header must NEVER be attached to external hosts
            assertNull("Bearer token must never be leaked to external hosts", recorded.getHeader("Authorization"));
        } finally {
            externalServer.shutdown();
        }
    }

    @Test
    public void testTokenAuthenticatorOn401RefreshesSuccessfully() throws Exception {
        sessionManager.setAccessToken("expired-jwt");
        sessionManager.setRefreshToken("valid-refresh-token");

        // 1. First call fails with 401 Unauthorized
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"message\":\"Token expired\"}"));

        // 2. Authenticator calls /refresh with refresh token -> returns 200 with rotated tokens
        String refreshJsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"accessToken\": \"refreshed-jwt-token\",\n" +
                "    \"refreshToken\": \"rotated-refresh-token\",\n" +
                "    \"tokenType\": \"Bearer\",\n" +
                "    \"expiresIn\": 900\n" +
                "  },\n" +
                "  \"message\": \"Token refreshed successfully\"\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(refreshJsonResponse));

        // 3. Authenticator retries the failed request with refreshed token -> returns 200 OK
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true,\"data\":\"Access Granted\"}"));

        Request request = new Request.Builder()
                .url(server.url("/api/v1/auth/student/restricted-feature"))
                .build();

        Response response = okHttpClient.newCall(request).execute();
        assertEquals(200, response.code());

        // Check the 3 recorded requests
        RecordedRequest initialReq = server.takeRequest();
        assertEquals("Bearer expired-jwt", initialReq.getHeader("Authorization"));

        RecordedRequest refreshReq = server.takeRequest();
        assertEquals("/api/v1/auth/refresh", refreshReq.getPath());
        assertTrue(refreshReq.getBody().readUtf8().contains("valid-refresh-token"));

        RecordedRequest retriedReq = server.takeRequest();
        assertEquals("Bearer refreshed-jwt-token", retriedReq.getHeader("Authorization"));

        assertEquals("refreshed-jwt-token", sessionManager.getAccessToken());
        assertEquals("rotated-refresh-token", sessionManager.getRefreshToken());
    }

    @Test
    public void testTokenAuthenticatorSingleRetryOnRefreshFailure() throws Exception {
        sessionManager.setAccessToken("expired-jwt");
        sessionManager.setRefreshToken("revoked-refresh-token");

        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"message\":\"Unauthorized\"}"));
        server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"message\":\"Invalid refresh token\"}"));

        Request request = new Request.Builder()
                .url(server.url("/api/v1/auth/student/restricted-feature"))
                .build();

        Response response = okHttpClient.newCall(request).execute();
        assertEquals(401, response.code());

        assertNull(sessionManager.getAccessToken());
        assertNull(sessionManager.getRefreshToken());
    }

    @Test
    public void testConcurrent401ResponsesExecuteSingleRefresh() throws Exception {
        sessionManager.setAccessToken("expired-jwt");
        sessionManager.setRefreshToken("valid-refresh-token");

        AtomicInteger refreshCallCount = new AtomicInteger(0);

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                String auth = request.getHeader("Authorization");

                if (path != null && path.contains("/auth/refresh")) {
                    refreshCallCount.incrementAndGet();
                    return new MockResponse().setResponseCode(200).setBody("{\n" +
                            "  \"success\": true,\n" +
                            "  \"data\": {\n" +
                            "    \"accessToken\": \"concurrent-refreshed-jwt\",\n" +
                            "    \"refreshToken\": \"concurrent-rotated-refresh\",\n" +
                            "    \"tokenType\": \"Bearer\",\n" +
                            "    \"expiresIn\": 900\n" +
                            "  }\n" +
                            "}");
                }

                if (auth != null && auth.contains("concurrent-refreshed-jwt")) {
                    return new MockResponse().setResponseCode(200).setBody("{\"success\":true,\"data\":\"OK\"}");
                }

                // Initial requests with expired token fail with 401
                return new MockResponse().setResponseCode(401).setBody("{\"message\":\"Expired token\"}");
            }
        });

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Response> responses = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    startLatch.await();
                    Request request = new Request.Builder()
                            .url(server.url("/api/v1/auth/me"))
                            .build();
                    Response resp = okHttpClient.newCall(request).execute();
                    responses.add(resp);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount, responses.size());
        for (Response resp : responses) {
            assertEquals(200, resp.code());
        }

        // Only 1 refresh request should have occurred despite concurrent 401s
        assertEquals(1, refreshCallCount.get());
        assertEquals("concurrent-refreshed-jwt", sessionManager.getAccessToken());
        assertEquals("concurrent-rotated-refresh", sessionManager.getRefreshToken());
    }

    @Test
    public void testLogoutRaceConditionPreventsSessionRevival() {
        sessionManager.setAccessToken("valid-access-token");
        sessionManager.setRefreshToken("valid-refresh-token");

        // Clear session (simulating logout while network call is in flight)
        sessionManager.clearSession();

        // Attempting to update tokens must return false and fail to revive
        boolean updated = sessionManager.updateTokens("new-access-token", "new-refresh-token");
        assertFalse(updated);
        assertNull(sessionManager.getAccessToken());
        assertNull(sessionManager.getRefreshToken());
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void testSessionBleedPreventionBetweenDifferentUsers() {
        // User A logs in as student
        TokenResponse tokenUserA = new TokenResponse();
        tokenUserA.setAccessToken("token-user-a");
        tokenUserA.setRefreshToken("refresh-user-a");
        UserSummaryResponse summaryA = new UserSummaryResponse();
        summaryA.setId(101L);
        summaryA.setKuetId("2307001");
        summaryA.setName("Student A");
        summaryA.setRole("ROLE_STUDENT");
        summaryA.setDepartment("CSE");
        summaryA.setVerified(false);
        tokenUserA.setUser(summaryA);

        sessionManager.saveAuthSession(tokenUserA);
        assertEquals("2307001", sessionManager.getUserId());
        assertFalse(sessionManager.isVerified());

        // User B logs in as verified Alumni on same device
        TokenResponse tokenUserB = new TokenResponse();
        tokenUserB.setAccessToken("token-user-b");
        tokenUserB.setRefreshToken("refresh-user-b");
        UserSummaryResponse summaryB = new UserSummaryResponse();
        summaryB.setId(202L);
        summaryB.setEmail("alumni.b@kuet.ac.bd");
        summaryB.setName("Alumni B");
        summaryB.setRole("ROLE_ALUMNI");
        summaryB.setDepartment("EEE");
        summaryB.setVerified(true);
        tokenUserB.setUser(summaryB);

        sessionManager.saveAuthSession(tokenUserB);

        // Assert all User A details are wiped and replaced by User B
        assertEquals("token-user-b", sessionManager.getAccessToken());
        assertEquals("refresh-user-b", sessionManager.getRefreshToken());
        assertEquals("ALUMNI", sessionManager.getUserRole());
        assertEquals("alumni.b@kuet.ac.bd", sessionManager.getUserId());
        assertEquals("Alumni B", sessionManager.getUserName());
        assertEquals("EEE", sessionManager.getUserDept());
        assertEquals(Long.valueOf(202L), sessionManager.getBackendId());
        assertTrue(sessionManager.isVerified());
    }

    @Test
    public void testErrorParsingWithValidationDetails() throws Exception {
        String validationErrorJson = "{\n" +
                "  \"success\": false,\n" +
                "  \"data\": {\n" +
                "    \"code\": \"VALIDATION_FAILED\",\n" +
                "    \"message\": \"Input validation failed\",\n" +
                "    \"details\": [\n" +
                "      \"email: must be a valid institutional KUET student email ending in @stud.kuet.ac.bd\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"message\": \"Invalid request payload\"\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(validationErrorJson));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorRef = new AtomicReference<>();

        StudentRegisterRequest req = new StudentRegisterRequest("2307003", "Test", "invalid@gmail.com", "CSE", "pass123");
        authRepository.registerStudent(req, new AuthRepository.Callback<TokenResponse>() {
            @Override
            public void onSuccess(TokenResponse result) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorRef.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("must be a valid institutional KUET student email"));
    }

    @Test
    public void testLogoutClearsSessionAndRevokes() throws Exception {
        sessionManager.setAccessToken("valid-access-token");
        sessionManager.setRefreshToken("valid-refresh-token");

        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true,\"message\":\"Logged out successfully\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        authRepository.logout(new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        RecordedRequest recorded = server.takeRequest();
        assertEquals("/api/v1/auth/logout", recorded.getPath());
        assertTrue(recorded.getBody().readUtf8().contains("valid-refresh-token"));

        assertNull(sessionManager.getAccessToken());
        assertNull(sessionManager.getRefreshToken());
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void testOfflineLogoutClearsSessionGracefully() throws Exception {
        sessionManager.setAccessToken("valid-access-token");
        sessionManager.setRefreshToken("valid-refresh-token");

        // Server responds with 500 error simulating network/server outage
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        CountDownLatch latch = new CountDownLatch(1);
        authRepository.logout(new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Even with network failure, local session must be cleared cleanly
        assertNull(sessionManager.getAccessToken());
        assertNull(sessionManager.getRefreshToken());
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void testRoleNormalizationMappingAllRoles() {
        assertEquals("STUDENT", SessionManager.normalizeRole("ROLE_STUDENT"));
        assertEquals("STUDENT", SessionManager.normalizeRole("STUDENT"));
        assertEquals("STUDENT", SessionManager.normalizeRole("student"));

        assertEquals("ALUMNI", SessionManager.normalizeRole("ROLE_ALUMNI"));
        assertEquals("ALUMNI", SessionManager.normalizeRole("ALUMNI"));

        assertEquals("TEACHER", SessionManager.normalizeRole("ROLE_TEACHER"));
        assertEquals("TEACHER", SessionManager.normalizeRole("TEACHER"));
        assertEquals("TEACHER", SessionManager.normalizeRole("ROLE_FACULTY"));
        assertEquals("TEACHER", SessionManager.normalizeRole("FACULTY"));
    }
}
