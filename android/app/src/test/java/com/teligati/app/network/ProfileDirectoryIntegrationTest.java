package com.teligati.app.network;

import com.google.gson.Gson;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.ProfileRepository;
import com.teligati.app.network.model.AlumniDirectoryResponse;
import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.PagedResponse;
import com.teligati.app.network.model.ProfileResponse;
import com.teligati.app.network.model.ProfileUpdateRequest;
import com.teligati.app.network.model.StudentDirectoryResponse;
import com.teligati.app.network.model.TeacherDirectoryResponse;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.*;

public class ProfileDirectoryIntegrationTest {
    private MockWebServer server;
    private SessionManager sessionManager;
    private ProfileApiService apiService;
    private ProfileRepository profileRepository;
    private Gson gson;
    private String baseUrl;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.setAccessToken("test-access-token-12345");
        sessionManager.setRefreshToken("test-refresh-token-67890");
        SessionManager.setInstanceForTesting(sessionManager);

        baseUrl = server.url("/").toString();
        gson = new Gson();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
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

        apiService = retrofit.create(ProfileApiService.class);
        profileRepository = new ProfileRepository(apiService, sessionManager, null, Runnable::run);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    // =========================================================================
    // 1. GET & UPDATE /ME TESTS
    // =========================================================================

    @Test
    public void fetchMyProfile_success_returnsProfileAndAttachesBearerToken() throws Exception {
        ProfileResponse mockProfile = new ProfileResponse();
        mockProfile.setUserId(1L);
        mockProfile.setKuetId("2307003");
        mockProfile.setName("Wafi");
        mockProfile.setRole("ROLE_STUDENT");
        mockProfile.setVerified(true);
        mockProfile.setDepartment("Computer Science and Engineering");
        mockProfile.setCgpa(3.92);
        mockProfile.setSkills("Java, Android, Spring Boot");

        ApiResponse<ProfileResponse> apiResp = ApiResponse.success(mockProfile, "Profile retrieved");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(apiResp)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ProfileResponse> resultRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        profileRepository.fetchMyProfile(new ProfileRepository.Callback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorRef.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(resultRef.get());
        assertEquals("2307003", resultRef.get().getKuetId());
        assertEquals("Wafi", resultRef.get().getName());
        assertEquals(Double.valueOf(3.92), resultRef.get().getCgpa());

        RecordedRequest req = server.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/api/v1/profiles/me", req.getPath());
        assertEquals("Bearer test-access-token-12345", req.getHeader("Authorization"));
    }

    @Test
    public void fetchMyProfile_unauthorized_invokesErrorCallback() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":false,\"message\":\"Full authentication is required\"}"));
        // TokenAuthenticator intercepts 401 and calls /api/v1/auth/refresh
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\":false,\"message\":\"Refresh token expired\"}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorRef = new AtomicReference<>();

        profileRepository.fetchMyProfile(new ProfileRepository.Callback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse result) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorRef.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
    }

    @Test
    public void updateMyProfile_success_sendsUpdateRequest() throws Exception {
        ProfileUpdateRequest updateReq = new ProfileUpdateRequest();
        updateReq.setName("Wafi Updated");
        updateReq.setSkills("Java 21, Kotlin, Docker");
        updateReq.setSpecialization("Distributed Systems");
        updateReq.setCgpa(3.95);

        ProfileResponse updatedProfile = new ProfileResponse();
        updatedProfile.setName("Wafi Updated");
        updatedProfile.setSkills("Java 21, Kotlin, Docker");
        updatedProfile.setCgpa(3.95);

        ApiResponse<ProfileResponse> apiResp = ApiResponse.success(updatedProfile, "Profile updated successfully");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(apiResp)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ProfileResponse> resultRef = new AtomicReference<>();

        profileRepository.updateMyProfile(updateReq, new ProfileRepository.Callback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals("Wafi Updated", resultRef.get().getName());
        assertEquals(Double.valueOf(3.95), resultRef.get().getCgpa());

        RecordedRequest req = server.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("PUT", req.getMethod());
        assertEquals("/api/v1/profiles/me", req.getPath());
        assertTrue(req.getBody().readUtf8().contains("Distributed Systems"));
    }

    // =========================================================================
    // 2. DIRECTORY APIS TESTS
    // =========================================================================

    @Test
    public void getStudentDirectory_success_passesQueryParamsAndReceivesPage() throws Exception {
        StudentDirectoryResponse student = new StudentDirectoryResponse();
        student.setUserId(1L);
        student.setRoll("2307003");
        student.setName("Wafi");
        student.setDepartment("CSE");
        student.setBatch("2023");
        student.setVerified(true);

        PagedResponse<StudentDirectoryResponse> page = new PagedResponse<>();
        page.setContent(Collections.singletonList(student));
        page.setPage(0);
        page.setSize(10);
        page.setTotalElements(1);
        page.setTotalPages(1);
        page.setLast(true);

        ApiResponse<PagedResponse<StudentDirectoryResponse>> resp = ApiResponse.success(page, "Directory retrieved");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(resp)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PagedResponse<StudentDirectoryResponse>> resultRef = new AtomicReference<>();

        profileRepository.getStudentDirectory("Wafi", "CSE", "2023", 0, 10,
                new ProfileRepository.Callback<PagedResponse<StudentDirectoryResponse>>() {
                    @Override
                    public void onSuccess(PagedResponse<StudentDirectoryResponse> result) {
                        resultRef.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals(1, resultRef.get().getContent().size());
        assertEquals("2307003", resultRef.get().getContent().get(0).getRoll());

        RecordedRequest req = server.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertTrue(req.getPath().contains("name=Wafi"));
        assertTrue(req.getPath().contains("department=CSE"));
        assertTrue(req.getPath().contains("batch=2023"));
    }

    @Test
    public void getAlumniDirectory_success_returnsAlumniWithMentorshipInfo() throws Exception {
        AlumniDirectoryResponse alumni = new AlumniDirectoryResponse();
        alumni.setUserId(2L);
        alumni.setName("Tanvir (Alumni)");
        alumni.setCurrentCompany("Tech Corp Munich");
        alumni.setCurrentPosition("Senior Software Engineer");
        alumni.setCountry("Germany");
        alumni.setAvailableForMentorship(true);
        alumni.setActivelyHiring(true);

        PagedResponse<AlumniDirectoryResponse> page = new PagedResponse<>();
        page.setContent(Collections.singletonList(alumni));
        page.setPage(0);
        page.setSize(20);
        page.setTotalElements(1);
        page.setTotalPages(1);

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(ApiResponse.success(page, "Alumni retrieved"))));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PagedResponse<AlumniDirectoryResponse>> resultRef = new AtomicReference<>();

        profileRepository.getAlumniDirectory(null, null, null, "Tech Corp", "Germany", null, 0, 20,
                new ProfileRepository.Callback<PagedResponse<AlumniDirectoryResponse>>() {
                    @Override
                    public void onSuccess(PagedResponse<AlumniDirectoryResponse> result) {
                        resultRef.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals("Tech Corp Munich", resultRef.get().getContent().get(0).getCurrentCompany());
        assertTrue(resultRef.get().getContent().get(0).isAvailableForMentorship());
        assertTrue(resultRef.get().getContent().get(0).isActivelyHiring());
    }

    @Test
    public void getTeacherDirectory_success_returnsFacultyWithResearchFocus() throws Exception {
        TeacherDirectoryResponse teacher = new TeacherDirectoryResponse();
        teacher.setUserId(3L);
        teacher.setName("Dr. Azharul");
        teacher.setDesignation("Professor & Head, CSE");
        teacher.setDepartment("CSE");
        teacher.setResearchFocus("Distributed AI");
        teacher.setLabName("Intelligent Systems Lab");

        PagedResponse<TeacherDirectoryResponse> page = new PagedResponse<>();
        page.setContent(Collections.singletonList(teacher));
        page.setPage(0);
        page.setSize(10);
        page.setTotalElements(1);

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(ApiResponse.success(page, "Teachers retrieved"))));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PagedResponse<TeacherDirectoryResponse>> resultRef = new AtomicReference<>();

        profileRepository.getTeacherDirectory(null, "CSE", "Professor", null, 0, 10,
                new ProfileRepository.Callback<PagedResponse<TeacherDirectoryResponse>>() {
                    @Override
                    public void onSuccess(PagedResponse<TeacherDirectoryResponse> result) {
                        resultRef.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals("Dr. Azharul", resultRef.get().getContent().get(0).getName());
        assertEquals("Distributed AI", resultRef.get().getContent().get(0).getResearchFocus());
    }

    @Test
    public void getProfileById_success_retrievesTargetProfile() throws Exception {
        ProfileResponse targetProfile = new ProfileResponse();
        targetProfile.setUserId(42L);
        targetProfile.setName("Jane Doe");
        targetProfile.setDepartment("ECE");
        targetProfile.setRole("ROLE_STUDENT");
        targetProfile.setCgpa(null); // Privacy: hidden for other users

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(ApiResponse.success(targetProfile, "Profile found"))));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ProfileResponse> resultRef = new AtomicReference<>();

        profileRepository.getProfileById(42L, new ProfileRepository.Callback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals(Long.valueOf(42), resultRef.get().getUserId());
        assertNull(resultRef.get().getCgpa());

        RecordedRequest req = server.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/api/v1/profiles/42", req.getPath());
    }

    // =========================================================================
    // 3. NULL SAFETY, ACCOUNT ISOLATION & RESILIENCE TESTS
    // =========================================================================

    @Test
    public void fetchMyProfile_handlesNullOptionalFieldsWithoutCrashing() throws Exception {
        ProfileResponse minimalProfile = new ProfileResponse();
        minimalProfile.setUserId(99L);
        minimalProfile.setKuetId("2307099");
        minimalProfile.setName("Minimal Student");
        minimalProfile.setRole("ROLE_STUDENT");
        // All other optional fields (bio, skills, projects, githubUrl, etc.) left null

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(ApiResponse.success(minimalProfile, "Profile retrieved"))));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ProfileResponse> resultRef = new AtomicReference<>();

        profileRepository.fetchMyProfile(new ProfileRepository.Callback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals("2307099", resultRef.get().getKuetId());
        assertNull(resultRef.get().getBio());
        assertNull(resultRef.get().getSkills());
        assertNull(resultRef.get().getGithubUrl());
    }

    @Test
    public void getAlumniDirectory_handlesNullFieldsInResponses() throws Exception {
        AlumniDirectoryResponse sparseAlumni = new AlumniDirectoryResponse();
        sparseAlumni.setUserId(50L);
        sparseAlumni.setName("Sparse Alumni");
        // Country, city, company, position, bio left null

        PagedResponse<AlumniDirectoryResponse> page = new PagedResponse<>();
        page.setContent(Collections.singletonList(sparseAlumni));
        page.setPage(0);
        page.setSize(10);
        page.setTotalElements(1);
        page.setTotalPages(1);

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(gson.toJson(ApiResponse.success(page, "Alumni page"))));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PagedResponse<AlumniDirectoryResponse>> resultRef = new AtomicReference<>();

        profileRepository.getAlumniDirectory(null, null, null, null, null, null, 0, 10,
                new ProfileRepository.Callback<PagedResponse<AlumniDirectoryResponse>>() {
                    @Override
                    public void onSuccess(PagedResponse<AlumniDirectoryResponse> result) {
                        resultRef.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals(1, resultRef.get().getContent().size());
        assertEquals("Sparse Alumni", resultRef.get().getContent().get(0).getName());
        assertNull(resultRef.get().getContent().get(0).getCurrentCompany());
    }

    @Test
    public void testAccountSwitchingSessionClearsPreviousTokensAndProfile() {
        // User A logs in
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.setAccessToken("token-user-a");
        sessionManager.setRefreshToken("refresh-user-a");
        assertEquals("2307003", sessionManager.getUserId());
        assertEquals("token-user-a", sessionManager.getAccessToken());

        // Logout
        sessionManager.clearSession();
        assertNull(sessionManager.getAccessToken());
        assertNull(sessionManager.getRefreshToken());

        // User B logs in
        sessionManager.setSession("ALUMNI", "tanvir.munich@kuet.ac.bd");
        sessionManager.setAccessToken("token-user-b");
        sessionManager.setRefreshToken("refresh-user-b");

        assertEquals("tanvir.munich@kuet.ac.bd", sessionManager.getUserId());
        assertEquals("ALUMNI", sessionManager.getUserRole());
        assertEquals("token-user-b", sessionManager.getAccessToken());
        assertNotEquals("token-user-a", sessionManager.getAccessToken());
    }

    @Test
    public void testDataMapperFromDirectoryResponseWithNullSkills() {
        AlumniDirectoryResponse resp = new AlumniDirectoryResponse();
        resp.setUserId(12L);
        resp.setName("Tanvir");
        resp.setSkills(null); // null skills test

        com.teligati.app.model.AlumniProfile profile = com.teligati.app.data.DataMapper.fromDirectoryResponse(resp);
        assertNotNull(profile);
        assertNotNull(profile.getSkills()); // Should be non-null empty list
        assertTrue(profile.getSkills().isEmpty());
    }
}
