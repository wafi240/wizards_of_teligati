package com.teligati.backend.research;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ResearchAcademicDomainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private String studentToken;
    private String teacherToken;
    private String otherTeacherToken;

    @BeforeEach
    void setUp() throws Exception {
        studentToken = obtainAccessToken("2307003", "demo");
        teacherToken = obtainAccessToken("azharul@cse.kuet.ac.bd", "demo");
        otherTeacherToken = obtainAccessToken("tanvir.munich@kuet.ac.bd", "demo"); // alumni token acts as non-teacher
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

    @Test
    @DisplayName("Teacher: Create research opportunity successfully")
    void teacherCreateOpportunity_success() throws Exception {
        ResearchOpportunityRequest request = ResearchOpportunityRequest.builder()
                .title("Distributed Graph Neural Networks on Edge Clusters")
                .description("Investigating communication-efficient GNN aggregation protocols.")
                .category("COMPUTER_SCIENCE")
                .requiredSkills("Python, PyTorch, PyG, Distributed Systems")
                .preferredBackground("B.Sc. in CSE or ECE")
                .positionsCount(2)
                .deadline(LocalDateTime.now().plusDays(30))
                .eligibilityCriteria("Min CGPA 3.50, Completed Data Structures")
                .duration("6 Months")
                .researchType("Funded Lab Assistantship")
                .minCgpa(3.50)
                .ownerDept("Dept of CSE")
                .build();

        mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Distributed Graph Neural Networks on Edge Clusters"))
                .andExpect(jsonPath("$.data.category").value("COMPUTER_SCIENCE"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.positionsCount").value(2));
    }

    @Test
    @DisplayName("Student: Cannot create research opportunities (400 Bad Request)")
    void studentCreateOpportunity_fails() throws Exception {
        ResearchOpportunityRequest request = ResearchOpportunityRequest.builder()
                .title("Illegal Student Opportunity Attempt")
                .description("Students should not be allowed to publish faculty calls.")
                .build();

        mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Research Opportunity: Complete application workflow (Apply -> Review -> Revision -> Resubmit -> Accept)")
    void researchApplicationWorkflow_complete() throws Exception {
        // 1. Teacher creates an opportunity
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Edge-assisted SLAM in Robotics")
                .description("Autonomous navigation and mapping.")
                .category("ROBOTICS")
                .requiredSkills("ROS 2, C++, Python")
                .deadline(LocalDateTime.now().plusDays(15))
                .positionsCount(1)
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Student applies
        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("I have completed robotics coursework and built autonomous rovers.")
                .skills("C++, ROS 2, OpenCV")
                .cgpa(3.84)
                .department("Dept of CSE")
                .build();

        MvcResult appResult = mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 3. Duplicate application attempt rejected
        mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isBadRequest());

        // 4. Teacher requests revision
        ResearchApplicationReviewRequest revisionReq = ResearchApplicationReviewRequest.builder()
                .status("REVISION_REQUESTED")
                .feedback("Please provide specific GitHub repository link demonstrating ROS 2 navigation.")
                .build();

        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revisionReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVISION_REQUESTED"))
                .andExpect(jsonPath("$.data.feedback").value(containsString("GitHub repository link")));

        // 5. Student resubmits application with requested details
        ResearchApplicationResubmitRequest resubmitReq = ResearchApplicationResubmitRequest.builder()
                .statement("Updated statement: My ROS 2 navigation stack is available at github.com/kuet/slam-nav.")
                .skills("C++, ROS 2, Nav2, SLAM")
                .build();

        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/resubmit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resubmitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // 6. Teacher reviews and accepts application
        ResearchApplicationReviewRequest acceptReq = ResearchApplicationReviewRequest.builder()
                .status("ACCEPTED")
                .feedback("Code reviewed. Welcome to the Robotics & Intelligent Systems Lab!")
                .build();

        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // 7. Verify notifications generated for student
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Research Application ACCEPTED"));
    }

    @Test
    @DisplayName("Research Application: Cannot apply after deadline")
    void applyAfterDeadline_rejected() throws Exception {
        // Teacher creates opportunity with past deadline
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Expired Research Call")
                .description("Past deadline opportunity.")
                .deadline(LocalDateTime.now().minusDays(1))
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student applies -> must be rejected
        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Late application.")
                .build();

        mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("deadline")));
    }

    @Test
    @DisplayName("Research Application: Student can withdraw pending application")
    void studentWithdrawApplication_success() throws Exception {
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Quantum Computing Seminar Lead")
                .description("Quantum algorithms research.")
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Interest in quantum entanglement.")
                .build();

        MvcResult appResult = mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student withdraws
        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/withdraw")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    @Test
    @DisplayName("Research Papers: Full lifecycle (Publish -> Browse -> Update -> Delete)")
    void researchPaperLifecycle_success() throws Exception {
        // 1. Teacher publishes paper
        ResearchPaperRequest publishReq = ResearchPaperRequest.builder()
                .title("Vision Transformers for Real-Time Satellite Telemetry")
                .abstractText("We introduce a low-latency ViT architecture benchmarked on KUET ground station data.")
                .researchField("COMPUTER_VISION")
                .authors("Dr. K. M. Azharul Hasan, Samun Sadab Wafi")
                .publicationDate("November 2026")
                .doi("10.1109/KUET.2026.10101")
                .referenceUrl("https://ieeexplore.ieee.org/document/10101")
                .build();

        MvcResult pubResult = mockMvc.perform(post("/api/v1/research/papers")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Vision Transformers for Real-Time Satellite Telemetry"))
                .andExpect(jsonPath("$.data.researchField").value("COMPUTER_VISION"))
                .andReturn();

        long paperId = objectMapper.readTree(pubResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Student browses papers (read-only)
        mockMvc.perform(get("/api/v1/research/papers")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("search", "Satellite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(paperId));

        // 3. Student cannot delete paper (Forbidden / Unauthorized)
        mockMvc.perform(delete("/api/v1/research/papers/" + paperId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 4. Publishing teacher updates paper
        publishReq.setTitle("Vision Transformers for Real-Time Satellite Telemetry (Extended)");
        mockMvc.perform(put("/api/v1/research/papers/" + paperId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Vision Transformers for Real-Time Satellite Telemetry (Extended)"));

        // 5. Publishing teacher deletes paper
        mockMvc.perform(delete("/api/v1/research/papers/" + paperId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Unverified student cannot apply for research opportunity")
    void unverifiedStudentCannotApply_fails() throws Exception {
        // Register a new student (by default isVerified = false)
        StudentRegisterRequest reg = StudentRegisterRequest.builder()
                .name("Unverified Candidate")
                .email("unverified.candidate@kuet.ac.bd")
                .roll("2307999")
                .department("CSE")
                .password("StrongPass123!")
                .build();

        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        String unverifiedToken = obtainAccessToken("2307999", "StrongPass123!");

        // Teacher creates opportunity
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Neural Quantum States Simulation")
                .description("VMC benchmarks on Spin Hamiltonians.")
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Unverified student tries to apply -> 400 Bad Request
        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Eager to join the quantum simulation research group.")
                .cgpa(3.90)
                .build();

        mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + unverifiedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("verified")));
    }

    @Test
    @DisplayName("Eligibility: Applicant CGPA below minimum required CGPA is rejected")
    void applicantBelowMinCgpa_rejected() throws Exception {
        // Teacher creates opportunity requiring min CGPA 3.80
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("High-Performance FPGA Matrix Acceleration")
                .description("Accelerating tensor primitives.")
                .minCgpa(3.80)
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student with CGPA 3.50 applies -> 400 Bad Request
        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Knowledgeable in Verilog and Vivado HLS.")
                .cgpa(3.50)
                .build();

        mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("below the minimum required CGPA")));
    }

    @Test
    @DisplayName("Security: Teacher cannot review another teacher's opportunity applicants")
    void teacherCannotReviewOtherTeachersApplicants_forbidden() throws Exception {
        // Teacher 1 creates opportunity
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Teacher 1 Opportunity")
                .description("Exclusive to Teacher 1 lab.")
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student applies
        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Application for Teacher 1 opportunity.")
                .build();

        MvcResult appResult = mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Create or get Teacher 2
        userRepository.findByEmail("rival.faculty@cse.kuet.ac.bd").orElseGet(() ->
                userRepository.save(User.builder()
                        .name("Dr. Rival Faculty")
                        .email("rival.faculty@cse.kuet.ac.bd")
                        .password(passwordEncoder.encode("demo"))
                        .role("ROLE_TEACHER")
                        .isVerified(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build())
        );

        String teacher2Token = obtainAccessToken("rival.faculty@cse.kuet.ac.bd", "demo");

        // Teacher 2 attempts to review Teacher 1's applicant -> 403 Forbidden
        ResearchApplicationReviewRequest reviewReq = ResearchApplicationReviewRequest.builder()
                .status("ACCEPTED")
                .feedback("Hijacking review decision.")
                .build();

        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + teacher2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lifecycle: Cannot review a withdrawn application")
    void cannotReviewWithdrawnApplication_rejected() throws Exception {
        // Teacher creates opportunity
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Robotic Vision Navigation")
                .description("Autonomous drones.")
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student applies
        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Drone perception experience.")
                .build();

        MvcResult appResult = mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student withdraws
        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/withdraw")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // Teacher attempts to review withdrawn application -> 400 Bad Request
        ResearchApplicationReviewRequest reviewReq = ResearchApplicationReviewRequest.builder()
                .status("ACCEPTED")
                .feedback("Trying to accept withdrawn application.")
                .build();

        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/review")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("withdrawn")));
    }

    @Test
    @DisplayName("Lifecycle: Student can re-apply after withdrawal without duplicate error")
    void reapplyAfterWithdrawal_success() throws Exception {
        // Teacher creates opportunity
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Cyber-Physical Security in Smart Grids")
                .description("Protocol fuzzing.")
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student applies
        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Initial pitch.")
                .build();

        MvcResult appResult = mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student withdraws
        mockMvc.perform(patch("/api/v1/research/applications/" + appId + "/withdraw")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // Student re-applies -> status resets to PENDING smoothly
        ResearchApplicationSubmitRequest reapplyReq = ResearchApplicationSubmitRequest.builder()
                .statement("Revised and motivated re-application pitch.")
                .build();

        mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reapplyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(appId))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.statement").value("Revised and motivated re-application pitch."));
    }

    @Test
    @DisplayName("Privacy: User cannot mark another user's notification as read")
    void notificationPrivacy_forbidden() throws Exception {
        // Student applies to generate teacher notification
        ResearchOpportunityRequest createReq = ResearchOpportunityRequest.builder()
                .title("Notification Privacy Test Call")
                .description("Privacy verification.")
                .build();

        MvcResult oppResult = mockMvc.perform(post("/api/v1/research/opportunities")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long oppId = objectMapper.readTree(oppResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        ResearchApplicationSubmitRequest submitReq = ResearchApplicationSubmitRequest.builder()
                .statement("Applicant statement.")
                .build();

        mockMvc.perform(post("/api/v1/research/opportunities/" + oppId + "/applications")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated());

        // Teacher fetches their notification
        MvcResult notifResult = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andReturn();

        long notifId = objectMapper.readTree(notifResult.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();

        // Student tries to mark Teacher's notification as read -> 403 Forbidden
        mockMvc.perform(patch("/api/v1/notifications/" + notifId + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
