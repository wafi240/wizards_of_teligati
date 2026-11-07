package com.teligati.app.network;

import com.google.gson.Gson;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.ResearchPaperEntity;
import com.teligati.app.network.model.*;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.*;

public class ResearchAcademicIntegrationTest {

    private MockWebServer server;
    private SessionManager sessionManager;
    private ResearchApiService apiService;
    private Gson gson;
    private String baseUrl;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("TEACHER", "teacher-cse-101");
        sessionManager.setAccessToken("test-access-token-research-999");
        sessionManager.setRefreshToken("test-refresh-token-research-888");
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

        apiService = retrofit.create(ResearchApiService.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void getOpportunities_attachesBearerTokenAndParsesResponse() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Research opportunities retrieved successfully\",\n" +
                "  \"data\": {\n" +
                "    \"content\": [\n" +
                "      {\n" +
                "        \"id\": 501,\n" +
                "        \"name\": \"Distributed Consensus in Edge AI\",\n" +
                "        \"title\": \"Distributed Consensus in Edge AI\",\n" +
                "        \"category\": \"Edge AI\",\n" +
                "        \"organization\": \"KUET Intelligent Systems Lab\",\n" +
                "        \"description\": \"Edge intelligence consensus protocol benchmark\",\n" +
                "        \"positionsCount\": 3,\n" +
                "        \"duration\": \"6 Months\",\n" +
                "        \"minCgpa\": 3.5,\n" +
                "        \"status\": \"OPEN\",\n" +
                "        \"postedByName\": \"Dr. Hasan\",\n" +
                "        \"postedByRole\": \"TEACHER\",\n" +
                "        \"isApplied\": false\n" +
                "      }\n" +
                "    ],\n" +
                "    \"page\": 0,\n" +
                "    \"size\": 20,\n" +
                "    \"totalElements\": 1,\n" +
                "    \"totalPages\": 1,\n" +
                "    \"last\": true\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        Response<ApiResponse<PagedResponse<ProjectNetworkDto>>> response =
                apiService.getOpportunities(null, "OPEN", null, 0, 20).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().isSuccess());

        PagedResponse<ProjectNetworkDto> paged = response.body().getData();
        assertNotNull(paged);
        assertEquals(1, paged.getContent().size());
        assertEquals("Distributed Consensus in Edge AI", paged.getContent().get(0).getName());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertTrue(recorded.getPath().contains("/api/v1/research/opportunities"));
        assertEquals("Bearer test-access-token-research-999", recorded.getHeader("Authorization"));
    }

    @Test
    public void createOpportunity_sendsCorrectPayload() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Research opportunity created successfully\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 601,\n" +
                "    \"name\": \"Deep Learning for Medical Imaging\",\n" +
                "    \"type\": \"Research\",\n" +
                "    \"status\": \"OPEN\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        ProjectCreateRequest req = new ProjectCreateRequest(
                "Deep Learning for Medical Imaging",
                "Development of segmentation architectures",
                "PyTorch, OpenCV",
                "https://github.com/kuet-biomed",
                true,
                "Research",
                "CSE",
                "10.1145/kuet.dl.2026"
        );

        Response<ApiResponse<ProjectNetworkDto>> response = apiService.createOpportunity(req).execute();
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(Long.valueOf(601), response.body().getData().getId());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/api/v1/research/opportunities", recorded.getPath());
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("Medical Imaging"));
    }

    @Test
    public void closeOpportunity_patchesCloseEndpoint() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Research opportunity closed\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 501,\n" +
                "    \"title\": \"Closed Lab Call\",\n" +
                "    \"status\": \"CLOSED\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        Response<ApiResponse<ProjectNetworkDto>> response = apiService.closeOpportunity(501L).execute();
        assertTrue(response.isSuccessful());
        assertEquals("CLOSED", response.body().getData().getStatus());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("PATCH", recorded.getMethod());
        assertEquals("/api/v1/research/opportunities/501/close", recorded.getPath());
    }

    @Test
    public void submitApplication_and_reviewApplication() throws Exception {
        // 1. Submit Application
        String submitJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Application submitted\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 88,\n" +
                "    \"opportunityId\": 501,\n" +
                "    \"opportunityTitle\": \"Distributed Consensus in Edge AI\",\n" +
                "    \"applicantName\": \"Rahim\",\n" +
                "    \"status\": \"PENDING\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(submitJson));

        ApplicationCreateRequest submitReq = new ApplicationCreateRequest("PROJECT", null, 501L, "I want to join this lab.");
        Response<ApiResponse<ApplicationNetworkDto>> submitResp = apiService.submitApplication(501L, submitReq).execute();
        assertTrue(submitResp.isSuccessful());
        assertEquals("PENDING", submitResp.body().getData().getStatus());

        RecordedRequest recSubmit = server.takeRequest();
        assertEquals("POST", recSubmit.getMethod());
        assertEquals("/api/v1/research/opportunities/501/applications", recSubmit.getPath());

        // 2. Review Application: REVISION_REQUESTED
        String reviewJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Application reviewed\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 88,\n" +
                "    \"status\": \"REVISION_REQUESTED\",\n" +
                "    \"feedback\": \"Please detail prior experience with Raft algorithm.\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(reviewJson));

        ApplicationStatusRequest reviewReq = new ApplicationStatusRequest("REVISION_REQUESTED", "Please detail prior experience with Raft algorithm.");
        Response<ApiResponse<ApplicationNetworkDto>> reviewResp = apiService.reviewApplication(88L, reviewReq).execute();
        assertTrue(reviewResp.isSuccessful());
        assertEquals("REVISION_REQUESTED", reviewResp.body().getData().getStatus());

        RecordedRequest recReview = server.takeRequest();
        assertEquals("PATCH", recReview.getMethod());
        assertEquals("/api/v1/research/applications/88/review", recReview.getPath());
        String reviewBody = recReview.getBody().readUtf8();
        assertTrue(reviewBody.contains("Raft algorithm"));
    }

    @Test
    public void resubmitAndWithdrawApplication_flow() throws Exception {
        // 1. Resubmit
        String resubmitJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Application resubmitted\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 88,\n" +
                "    \"status\": \"PENDING\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(resubmitJson));

        ApplicationCreateRequest resubmitReq = new ApplicationCreateRequest("PROJECT", null, 501L, "Updated statement with Raft protocol benchmarks.");
        Response<ApiResponse<ApplicationNetworkDto>> resubmitResp = apiService.resubmitApplication(88L, resubmitReq).execute();
        assertTrue(resubmitResp.isSuccessful());
        assertEquals("PENDING", resubmitResp.body().getData().getStatus());

        RecordedRequest recResubmit = server.takeRequest();
        assertEquals("PATCH", recResubmit.getMethod());
        assertEquals("/api/v1/research/applications/88/resubmit", recResubmit.getPath());

        // 2. Withdraw
        String withdrawJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Application withdrawn\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 88,\n" +
                "    \"status\": \"WITHDRAWN\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(withdrawJson));

        Response<ApiResponse<ApplicationNetworkDto>> withdrawResp = apiService.withdrawApplication(88L).execute();
        assertTrue(withdrawResp.isSuccessful());
        assertEquals("WITHDRAWN", withdrawResp.body().getData().getStatus());

        RecordedRequest recWithdraw = server.takeRequest();
        assertEquals("PATCH", recWithdraw.getMethod());
        assertEquals("/api/v1/research/applications/88/withdraw", recWithdraw.getPath());
    }

    @Test
    public void researchPapers_publishAndGetList() throws Exception {
        // 1. Publish Paper
        String publishJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Research paper published\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 77,\n" +
                "    \"title\": \"Quantum Key Distribution in Campus Networks\",\n" +
                "    \"abstractText\": \"Experimental verification of BB84 protocol on optical fiber\",\n" +
                "    \"researchField\": \"Quantum Computing\",\n" +
                "    \"authors\": \"Dr. Hasan, Rahim (Student)\",\n" +
                "    \"doi\": \"10.1109/KUET.2026.001\",\n" +
                "    \"referenceUrl\": \"https://kuet.ac.bd/research/quantum\",\n" +
                "    \"teacherId\": 101,\n" +
                "    \"teacherName\": \"Dr. Hasan\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(publishJson));

        ResearchPaperCreateRequest createReq = new ResearchPaperCreateRequest(
                "Quantum Key Distribution in Campus Networks",
                "Experimental verification of BB84 protocol on optical fiber",
                "Quantum Computing",
                "Dr. Hasan, Rahim (Student)",
                "2026-05-15",
                "10.1109/KUET.2026.001",
                "https://kuet.ac.bd/research/quantum",
                "https://kuet.ac.bd/docs/bb84.pdf"
        );

        Response<ApiResponse<ResearchPaperNetworkDto>> publishResp = apiService.publishPaper(createReq).execute();
        assertTrue(publishResp.isSuccessful());
        assertNotNull(publishResp.body());
        assertEquals(Long.valueOf(77), publishResp.body().getData().getId());
        assertEquals("Quantum Key Distribution in Campus Networks", publishResp.body().getData().getTitle());

        RecordedRequest recPublish = server.takeRequest();
        assertEquals("POST", recPublish.getMethod());
        assertEquals("/api/v1/research/papers", recPublish.getPath());

        // Test DataMapper mapping to entity
        ResearchPaperEntity entity = DataMapper.toPaperEntity(publishResp.body().getData());
        assertNotNull(entity);
        assertEquals(77L, entity.getId());
        assertEquals("Quantum Key Distribution in Campus Networks", entity.getTitle());
        assertEquals("Quantum Computing", entity.getTechnologies());
    }
}
