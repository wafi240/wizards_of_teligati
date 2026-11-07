package com.teligati.app.network;

import com.google.gson.Gson;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.*;
import com.teligati.app.data.repository.OpportunityRepository;
import com.teligati.app.network.model.*;
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

public class OpportunityIntegrationTest {

    private MockWebServer server;
    private SessionManager sessionManager;
    private OpportunityApiService apiService;
    private Gson gson;
    private String baseUrl;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.setAccessToken("test-access-token-opp-12345");
        sessionManager.setRefreshToken("test-refresh-token-opp-67890");
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

        apiService = retrofit.create(OpportunityApiService.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void getJobs_attachesBearerTokenAndParsesResponse() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Jobs retrieved successfully\",\n" +
                "  \"data\": {\n" +
                "    \"content\": [\n" +
                "      {\n" +
                "        \"id\": 101,\n" +
                "        \"title\": \"Python Script Automation Assistant\",\n" +
                "        \"category\": \"ASSISTANT_WORK\",\n" +
                "        \"organization\": \"KUET Alumni Tech Venture\",\n" +
                "        \"location\": \"Remote\",\n" +
                "        \"compensation\": \"৳15,000 / month\",\n" +
                "        \"duration\": \"10 hrs/week\",\n" +
                "        \"description\": \"Assist with automation pipelines\",\n" +
                "        \"skillsRequired\": \"Python, SQL\",\n" +
                "        \"minCgpa\": 3.0,\n" +
                "        \"status\": \"OPEN\",\n" +
                "        \"postedByUserId\": 2,\n" +
                "        \"postedByName\": \"Tanvir (Alumni)\",\n" +
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

        retrofit2.Response<ApiResponse<PagedResponse<JobNetworkDto>>> response =
                apiService.getJobs("ASSISTANT_WORK", "OPEN", null, 0, 20).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().isSuccess());

        PagedResponse<JobNetworkDto> paged = response.body().getData();
        assertNotNull(paged);
        assertEquals(1, paged.getContent().size());
        assertEquals("Python Script Automation Assistant", paged.getContent().get(0).getTitle());
        assertEquals("ASSISTANT_WORK", paged.getContent().get(0).getCategory());

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("category=ASSISTANT_WORK"));
        assertEquals("Bearer test-access-token-opp-12345", recordedRequest.getHeader("Authorization"));
    }

    @Test
    public void createAssistantJob_sendsValidPayload() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Assistant job created successfully\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 102,\n" +
                "    \"title\": \"Data Scraping Helper\",\n" +
                "    \"category\": \"ASSISTANT_WORK\",\n" +
                "    \"status\": \"OPEN\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        JobCreateRequest req = new JobCreateRequest(
                "Data Scraping Helper", "Alumni Venture", "Alumni Venture", "Remote",
                "Contract", "Scraping tasks", "৳10,000 / month", "ASSISTANT_WORK",
                "8 hrs/wk", "Python, Beautifulsoup", 3.0
        );

        retrofit2.Response<ApiResponse<JobNetworkDto>> response =
                apiService.createAssistantJob(req).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(Long.valueOf(102L), response.body().getData().getId());

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/assistant-jobs", recordedRequest.getPath());
        assertTrue(recordedRequest.getBody().readUtf8().contains("Data Scraping Helper"));
    }

    @Test
    public void submitApplication_sendsCorrectRequest() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Application submitted successfully\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 501,\n" +
                "    \"opportunityType\": \"JOB\",\n" +
                "    \"jobId\": 101,\n" +
                "    \"jobTitle\": \"Python Script Automation Assistant\",\n" +
                "    \"applicantRoll\": \"2307003\",\n" +
                "    \"status\": \"APPLIED\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        ApplicationCreateRequest appReq = new ApplicationCreateRequest("JOB", 101L, null, "Experienced applicant.");
        retrofit2.Response<ApiResponse<ApplicationNetworkDto>> response =
                apiService.submitApplication(appReq).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(Long.valueOf(501L), response.body().getData().getId());
        assertEquals("APPLIED", response.body().getData().getStatus());

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/applications", recordedRequest.getPath());
        assertTrue(recordedRequest.getBody().readUtf8().contains("\"jobId\":101"));
    }

    @Test
    public void updateApplicationStatus_sendsPatchRequest() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Application status updated successfully\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 501,\n" +
                "    \"status\": \"SHORTLISTED\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        retrofit2.Response<ApiResponse<ApplicationNetworkDto>> response =
                apiService.updateApplicationStatus(501L, new ApplicationStatusRequest("SHORTLISTED")).execute();

        assertTrue(response.isSuccessful());
        assertEquals("SHORTLISTED", response.body().getData().getStatus());

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("PATCH", recordedRequest.getMethod());
        assertEquals("/api/v1/applications/501/status", recordedRequest.getPath());
        assertTrue(recordedRequest.getBody().readUtf8().contains("SHORTLISTED"));
    }

    @Test
    public void submitResearchProposal_sendsCorrectPayload() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Research proposal submitted successfully\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 301,\n" +
                "    \"topic\": \"Decentralized Edge Storage\",\n" +
                "    \"proposalAbstract\": \"Exploring IPFS and gossip protocols\",\n" +
                "    \"degreeType\": \"Undergrad Thesis\",\n" +
                "    \"status\": \"PENDING\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        ResearchProposalCreateRequest req = new ResearchProposalCreateRequest(
                "Decentralized Edge Storage",
                "Exploring IPFS and gossip protocols",
                "Undergrad Thesis",
                3L
        );

        retrofit2.Response<ApiResponse<ResearchProposalNetworkDto>> response =
                apiService.submitProposal(req).execute();

        assertTrue(response.isSuccessful());
        assertEquals("Decentralized Edge Storage", response.body().getData().getTopic());
        assertEquals("PENDING", response.body().getData().getStatus());

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/research-opportunities/proposals", recordedRequest.getPath());
    }

    @Test
    public void decideResearchProposal_sendsFeedbackPatch() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Proposal decision recorded successfully\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 301,\n" +
                "    \"status\": \"ACCEPTED\",\n" +
                "    \"feedback\": \"Approved for supervision\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseJson));

        retrofit2.Response<ApiResponse<ResearchProposalNetworkDto>> response =
                apiService.decideProposal(301L, new ProposalDecisionRequest("ACCEPTED", "Approved for supervision")).execute();

        assertTrue(response.isSuccessful());
        assertEquals("ACCEPTED", response.body().getData().getStatus());
        assertEquals("Approved for supervision", response.body().getData().getFeedback());

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("PATCH", recordedRequest.getMethod());
        assertEquals("/api/v1/research-opportunities/proposals/301/decision", recordedRequest.getPath());
    }

    @Test
    public void getMyProposals_retrievesStudentProposals() throws Exception {
        String mockResponseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Proposals retrieved successfully\",\n" +
                "  \"data\": {\n" +
                "    \"content\": [\n" +
                "      {\n" +
                "        \"id\": 401,\n" +
                "        \"topic\": \"Edge AI Model Compression\",\n" +
                "        \"proposalAbstract\": \"Lightweight quantization for embedded systems\",\n" +
                "        \"degreeType\": \"Undergrad Thesis\",\n" +
                "        \"status\": \"Accepted\",\n" +
                "        \"feedback\": \"Approved by Dr. Azharul\",\n" +
                "        \"studentId\": 1,\n" +
                "        \"studentName\": \"Samun Sadab Wafi\",\n" +
                "        \"studentRoll\": \"2307003\",\n" +
                "        \"facultyId\": 3,\n" +
                "        \"facultyName\": \"Dr. Azharul (Faculty)\",\n" +
                "        \"createdAt\": \"2026-11-07T12:00:00\"\n" +
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

        retrofit2.Response<ApiResponse<PagedResponse<ResearchProposalNetworkDto>>> response =
                apiService.getMyProposals(0, 20).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().isSuccess());
        assertEquals(1, response.body().getData().getContent().size());

        ResearchProposalNetworkDto dto = response.body().getData().getContent().get(0);
        assertEquals(Long.valueOf(401L), dto.getId());
        assertEquals("Edge AI Model Compression", dto.getTopic());
        assertEquals("Accepted", dto.getStatus());
        assertEquals("2307003", dto.getStudentRoll());

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().startsWith("/api/v1/research-opportunities/proposals/me"));
    }

    @Test
    public void dataMapper_convertsNetworkDtosToRoomEntities() {
        JobNetworkDto jobDto = gson.fromJson(
                "{\"id\":10,\"title\":\"Robotics Lead\",\"category\":\"RESEARCH_COLLABORATION\",\"organization\":\"Robotics Lab\",\"minCgpa\":3.5}",
                JobNetworkDto.class
        );
        JobPostingEntity jobEntity = DataMapper.toJobEntity(jobDto);
        assertNotNull(jobEntity);
        assertEquals(10L, jobEntity.getId());
        assertEquals("Robotics Lead", jobEntity.getTitle());
        assertEquals("RESEARCH_COLLABORATION", jobEntity.getCategory());
        assertEquals(Double.valueOf(3.5), jobEntity.getMinCgpa());

        ProjectNetworkDto projectDto = gson.fromJson(
                "{\"id\":20,\"name\":\"Drone Swarm AI\",\"type\":\"Research Paper\",\"contributorsCount\":4}",
                ProjectNetworkDto.class
        );
        ResearchPaperEntity paperEntity = DataMapper.toPaperEntity(projectDto);
        assertNotNull(paperEntity);
        assertEquals(20L, paperEntity.getId());
        assertEquals("Drone Swarm AI", paperEntity.getTitle());
        assertEquals(4, paperEntity.getContributorsCount());

        ApplicationNetworkDto appDto = gson.fromJson(
                "{\"id\":30,\"jobId\":10,\"applicantRoll\":\"2307003\",\"applicantName\":\"Wafi\",\"status\":\"ACCEPTED\"}",
                ApplicationNetworkDto.class
        );
        JobApplicationEntity appEntity = DataMapper.toJobApplicationEntity(appDto);
        assertNotNull(appEntity);
        assertEquals(30L, appEntity.getId());
        assertEquals(10L, appEntity.getJobId());
        assertEquals("2307003", appEntity.getApplicantRoll());
        assertEquals("ACCEPTED", appEntity.getStatus());

        ResearchProposalNetworkDto propDto = gson.fromJson(
                "{\"id\":50,\"topic\":\"Quantum Cryptography\",\"proposalAbstract\":\"Lattice based crypto\",\"degreeType\":\"Master Thesis\",\"status\":\"Pending\",\"facultyId\":3,\"studentRoll\":\"2307003\",\"feedback\":\"Under review\"}",
                ResearchProposalNetworkDto.class
        );
        ResearchProposalEntity propEntity = DataMapper.toProposalEntity(propDto);
        assertNotNull(propEntity);
        assertEquals(50L, propEntity.getId());
        assertEquals("Quantum Cryptography", propEntity.getTopic());
        assertEquals("Master Thesis", propEntity.getDegreeType());
        assertEquals("Pending", propEntity.getStatus());
        assertEquals("3", propEntity.getTeacherId());

        com.teligati.app.model.ResearchProposal model = DataMapper.toResearchProposal(propEntity);
        assertNotNull(model);
        assertEquals(50L, model.getId());
        assertEquals("Quantum Cryptography", model.getTopic());
        assertEquals("Pending", model.getStatus());
        assertNotNull(model.getFormattedDate());
    }
}
