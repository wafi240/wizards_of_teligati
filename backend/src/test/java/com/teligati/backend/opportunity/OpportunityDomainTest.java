package com.teligati.backend.opportunity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teligati.backend.dto.*;
import com.teligati.backend.entity.User;
import com.teligati.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class OpportunityDomainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String studentToken;
    private String alumniToken;
    private String teacherToken;

    @BeforeEach
    void setUp() throws Exception {
        studentToken = obtainAccessToken("2307003", "demo");
        alumniToken = obtainAccessToken("tanvir.munich@kuet.ac.bd", "demo");
        teacherToken = obtainAccessToken("azharul@cse.kuet.ac.bd", "demo");
    }

    private String obtainAccessToken(String identifier, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier(identifier)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    // =========================================================================
    // 1. ASSISTANT JOBS
    // =========================================================================

    @Test
    @DisplayName("Assistant Jobs: Alumni creates assistant job successfully")
    void createAssistantJob_success() throws Exception {
        JobRequest request = JobRequest.builder()
                .title("Python Script Automation Assistant")
                .organization("KUET Alumni Tech Venture")
                .compensation("৳15,000 / month")
                .duration("10 hrs/week")
                .skillsRequired("Python, SQL, REST APIs")
                .description("Assist with automation pipelines and web scraping.")
                .build();

        mockMvc.perform(post("/api/v1/assistant-jobs")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Python Script Automation Assistant")))
                .andExpect(jsonPath("$.data.category", is("ASSISTANT_WORK")))
                .andExpect(jsonPath("$.data.status", is("OPEN")));
    }

    @Test
    @DisplayName("Assistant Jobs: Non-owner cannot update assistant job (403 Forbidden)")
    void updateAssistantJob_forbiddenForNonOwner() throws Exception {
        // 1. Create job as Alumni
        JobRequest createReq = JobRequest.builder()
                .title("Database Tuning Helper")
                .description("PostgreSQL performance audit")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/assistant-jobs")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long jobId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Student attempts to update Alumni's job -> 403 Forbidden
        JobRequest updateReq = JobRequest.builder()
                .title("Malicious Title Override")
                .build();

        mockMvc.perform(put("/api/v1/assistant-jobs/" + jobId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. PROFESSIONAL JOBS
    // =========================================================================

    @Test
    @DisplayName("Professional Jobs: Alumni creates professional job successfully")
    void createProfessionalJob_success() throws Exception {
        JobRequest request = JobRequest.builder()
                .title("Software Engineer - Distributed Systems")
                .company("Tech Corp Munich")
                .location("Munich, Germany / Hybrid")
                .type("Full-time")
                .compensation("€65,000 / year")
                .description("Looking for bright KUET graduates to join our backend platform team.")
                .skillsRequired("Go, Kubernetes, Java, Kafka")
                .minCgpa(3.5)
                .category("FULLTIME_JOBS")
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Software Engineer - Distributed Systems")))
                .andExpect(jsonPath("$.data.category", is("FULLTIME_JOBS")));
    }

    @Test
    @DisplayName("Professional Jobs: Student cannot post full-time professional job (400 Bad Request)")
    void createProfessionalJob_studentForbidden() throws Exception {
        JobRequest request = JobRequest.builder()
                .title("Lead Architect Position")
                .category("FULLTIME_JOBS")
                .build();

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Students are not authorized to post full-time professional jobs")));
    }

    // =========================================================================
    // 3. RESEARCH OPPORTUNITIES & PROPOSALS
    // =========================================================================

    @Test
    @DisplayName("Research Opportunities: Faculty creates research project successfully")
    void createResearchOpportunity_success() throws Exception {
        ProjectRequest request = ProjectRequest.builder()
                .name("Edge Intelligence Benchmark Testbed")
                .description("Measuring inference latency across heterogeneous edge nodes.")
                .technologies("PyTorch, ONNX, C++, Docker")
                .type("Call for Co-Authors")
                .ownerDept("Computer Science and Engineering")
                .isOpenForContribution(true)
                .build();

        mockMvc.perform(post("/api/v1/research-opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Edge Intelligence Benchmark Testbed")))
                .andExpect(jsonPath("$.data.isOpenForContribution", is(true)));
    }

    @Test
    @DisplayName("Research Proposals: Student submits proposal to faculty, and faculty accepts")
    void submitAndDecideResearchProposal() throws Exception {
        User teacher = userRepository.findByEmail("azharul@cse.kuet.ac.bd").orElseThrow();

        // 1. Student submits proposal
        ResearchProposalRequest propReq = ResearchProposalRequest.builder()
                .topic("Byzantine-Resilient Consensus for Mobile Mesh")
                .proposalAbstract("Investigating gossip protocols with verifiable secret sharing in ad-hoc mesh networks.")
                .degreeType("Undergrad Thesis")
                .facultyId(teacher.getId())
                .build();

        MvcResult submitResult = mockMvc.perform(post("/api/v1/research-opportunities/proposals")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn();

        long propId = objectMapper.readTree(submitResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Faculty accepts proposal with feedback
        ProposalDecisionRequest decisionReq = ProposalDecisionRequest.builder()
                .status("ACCEPTED")
                .feedback("Approved for lab supervision. Please report to the ISN Lab.")
                .build();

        mockMvc.perform(patch("/api/v1/research-opportunities/proposals/" + propId + "/decision")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.data.feedback", containsString("Approved for lab supervision")));
    }

    // =========================================================================
    // 4. APPLICATIONS DOMAIN
    // =========================================================================

    @Test
    @DisplayName("Applications: Student applies for job, prevents duplicates, owner shortlists")
    void applicationLifecycleAndDuplicatePrevention() throws Exception {
        // 1. Alumni creates assistant job
        JobRequest jobReq = JobRequest.builder()
                .title("Mobile App QA Assistant")
                .compensation("৳12,000 / month")
                .build();

        MvcResult jobResult = mockMvc.perform(post("/api/v1/assistant-jobs")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Student applies
        ApplicationRequest appReq = ApplicationRequest.builder()
                .opportunityType("JOB")
                .jobId(jobId)
                .statement("Experienced with Android unit testing, Espresso, and Mockito.")
                .build();

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPLIED")))
                .andExpect(jsonPath("$.data.applicantRoll", is("2307003")))
                .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Duplicate application attempt is rejected (400 Bad Request)
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already applied")));

        // 4. Poster views applications for this job
        mockMvc.perform(get("/api/v1/applications/jobs/" + jobId)
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].applicantRoll", is("2307003")));

        // 5. Poster shortlists applicant
        ApplicationStatusUpdateRequest statusReq = ApplicationStatusUpdateRequest.builder()
                .status("SHORTLISTED")
                .build();

        mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SHORTLISTED")));

        // 6. Poster accepts applicant
        statusReq.setStatus("ACCEPTED");
        mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACCEPTED")));
    }

    @Test
    @DisplayName("Applications: Self-application to own job is rejected")
    void selfApplication_rejected() throws Exception {
        // Alumni posts job
        JobRequest jobReq = JobRequest.builder()
                .title("Backend Architect Role")
                .build();

        MvcResult jobResult = mockMvc.perform(post("/api/v1/assistant-jobs")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Alumni tries to apply to their own job
        ApplicationRequest appReq = ApplicationRequest.builder()
                .opportunityType("JOB")
                .jobId(jobId)
                .statement("Applying to myself")
                .build();

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot apply to your own")));
    }

    @Test
    @DisplayName("Applications: Applicant can withdraw application, but cannot arbitrarily mark ACCEPTED")
    void applicantWithdraw_andArbitraryTransitionRejected() throws Exception {
        // 1. Alumni posts job
        JobRequest jobReq = JobRequest.builder()
                .title("Research Assistant in Edge AI")
                .build();

        MvcResult jobResult = mockMvc.perform(post("/api/v1/assistant-jobs")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Student applies
        ApplicationRequest appReq = ApplicationRequest.builder()
                .opportunityType("JOB")
                .jobId(jobId)
                .statement("Interested in assisting.")
                .build();

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Student tries to mark themselves as "ACCEPTED" -> 400 Bad Request
        ApplicationStatusUpdateRequest hackStatus = ApplicationStatusUpdateRequest.builder()
                .status("ACCEPTED")
                .build();

        mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Applicants can only withdraw")));

        // 4. Student withdraws application -> 200 OK
        ApplicationStatusUpdateRequest withdrawStatus = ApplicationStatusUpdateRequest.builder()
                .status("WITHDRAWN")
                .build();

        mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawStatus)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("WITHDRAWN")));

        // 5. Subsequent update to withdrawn application is rejected -> 400 Bad Request
        mockMvc.perform(patch("/api/v1/applications/" + appId + "/status")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("withdrawn")));
    }
}
