package com.teligati.app.network;

import com.google.gson.Gson;
import com.teligati.app.data.SessionManager;
import com.teligati.app.network.model.*;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.*;

public class ReferenceRecommendationIntegrationTest {

    private MockWebServer server;
    private SessionManager sessionManager;
    private ReferenceApiService apiService;
    private Gson gson;
    private String baseUrl;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.setAccessToken("test-access-token-123");
        sessionManager.setRefreshToken("test-refresh-token-456");
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

        apiService = retrofit.create(ReferenceApiService.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void createReferenceRequest_sendsCorrectPayloadAndBearerToken() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Reference request submitted\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 101,\n" +
                "    \"studentRoll\": \"2307003\",\n" +
                "    \"targetInstitution\": \"Google Munich\",\n" +
                "    \"targetProgram\": \"Software Engineer\",\n" +
                "    \"status\": \"PENDING\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        ReferenceCreateRequest request = new ReferenceCreateRequest(
                2L, "JOB_REFERRAL", "Google Munich", "Software Engineer", null,
                "Requesting internal referral", "High CGPA"
        );

        Response<ApiResponse<ReferenceNetworkDto>> response = apiService.createRequest(request).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().isSuccess());
        assertEquals(Long.valueOf(101), response.body().getData().getId());
        assertEquals("Google Munich", response.body().getData().getTargetInstitution());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/api/v1/references", recorded.getPath());
        assertEquals("Bearer test-access-token-123", recorded.getHeader("Authorization"));
        assertTrue(recorded.getBody().readUtf8().contains("Google Munich"));
    }

    @Test
    public void getSentRequests_sendsGetAndParsesPagedResponse() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"content\": [\n" +
                "      {\n" +
                "        \"id\": 101,\n" +
                "        \"targetInstitution\": \"Google Munich\",\n" +
                "        \"status\": \"PENDING\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"totalElements\": 1,\n" +
                "    \"totalPages\": 1\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        Response<ApiResponse<PagedResponse<ReferenceNetworkDto>>> response = apiService.getSentRequests(0, 20).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        List<ReferenceNetworkDto> list = response.body().getData().getContent();
        assertEquals(1, list.size());
        assertEquals("Google Munich", list.get(0).getTargetInstitution());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals("/api/v1/references/sent?page=0&size=20", recorded.getPath());
        assertEquals("Bearer test-access-token-123", recorded.getHeader("Authorization"));
    }

    @Test
    public void getIncomingRequests_withStatusFilter() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"content\": [],\n" +
                "    \"totalElements\": 0,\n" +
                "    \"totalPages\": 0\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        Response<ApiResponse<PagedResponse<ReferenceNetworkDto>>> response =
                apiService.getIncomingRequests("PENDING", 0, 10).execute();

        assertTrue(response.isSuccessful());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals("/api/v1/references/incoming?status=PENDING&page=0&size=10", recorded.getPath());
    }

    @Test
    public void decideRequest_sendsPatchWithDecision() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 101,\n" +
                "    \"status\": \"ACCEPTED\",\n" +
                "    \"responseNotes\": \"Pleased to support\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        ReferenceDecisionNetworkRequest req = new ReferenceDecisionNetworkRequest("ACCEPTED", "Pleased to support");
        Response<ApiResponse<ReferenceNetworkDto>> response = apiService.decideRequest(101L, req).execute();

        assertTrue(response.isSuccessful());
        assertEquals("ACCEPTED", response.body().getData().getStatus());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("PATCH", recorded.getMethod());
        assertEquals("/api/v1/references/101/decision", recorded.getPath());
        assertTrue(recorded.getBody().readUtf8().contains("ACCEPTED"));
    }

    @Test
    public void completeRecommendation_sendsPatchWithRecommendationText() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 101,\n" +
                "    \"status\": \"COMPLETED\",\n" +
                "    \"recommendationText\": \"Top tier talent from KUET CSE.\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        RecommendationNetworkRequest req = new RecommendationNetworkRequest("Top tier talent from KUET CSE.", "Formal Letter");
        Response<ApiResponse<ReferenceNetworkDto>> response = apiService.completeRecommendation(101L, req).execute();

        assertTrue(response.isSuccessful());
        assertEquals("COMPLETED", response.body().getData().getStatus());
        assertEquals("Top tier talent from KUET CSE.", response.body().getData().getRecommendationText());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("PATCH", recorded.getMethod());
        assertEquals("/api/v1/references/101/recommendation", recorded.getPath());
        assertTrue(recorded.getBody().readUtf8().contains("Top tier talent"));
    }

    @Test
    public void cancelRequest_sendsPatchToCancel() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 101,\n" +
                "    \"status\": \"CANCELLED\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        Response<ApiResponse<ReferenceNetworkDto>> response = apiService.cancelRequest(101L).execute();

        assertTrue(response.isSuccessful());
        assertEquals("CANCELLED", response.body().getData().getStatus());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("PATCH", recorded.getMethod());
        assertEquals("/api/v1/references/101/cancel", recorded.getPath());
    }
}
