package com.teligati.backend.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teligati.backend.dto.*;
import com.teligati.backend.entity.User;
import com.teligati.backend.repository.NotificationRepository;
import com.teligati.backend.repository.ReferenceRequestRepository;
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
class ReferenceRecommendationDomainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferenceRequestRepository referenceRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private String studentToken;
    private String teacherToken;
    private String alumniToken;
    private Long studentUserId;
    private Long teacherUserId;
    private Long alumniUserId;

    @BeforeEach
    void setUp() throws Exception {
        studentToken = obtainAccessToken("2307003", "demo");
        teacherToken = obtainAccessToken("azharul@cse.kuet.ac.bd", "demo");
        alumniToken = obtainAccessToken("tanvir.munich@kuet.ac.bd", "demo");

        studentUserId = userRepository.findByIdentifier("2307003").orElseThrow().getId();
        teacherUserId = userRepository.findByIdentifier("azharul@cse.kuet.ac.bd").orElseThrow().getId();
        alumniUserId = userRepository.findByIdentifier("tanvir.munich@kuet.ac.bd").orElseThrow().getId();
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
    @DisplayName("Student: Create professional referral request to Alumni successfully")
    void studentCreateReferralRequestToAlumni_success() throws Exception {
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(alumniUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution("Google Munich")
                .targetProgram("Software Engineer III (Core Systems)")
                .deadline(LocalDateTime.now().plusWeeks(2))
                .personalMessage("Hello Tanvir vai, I am applying for the Core Systems role at Google Munich. Your internal referral would mean the world!")
                .supportingInfo("GitHub: https://github.com/kuet-wizards, GPA: 3.84")
                .build();

        mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.targetInstitution").value("Google Munich"))
                .andExpect(jsonPath("$.data.recipientRole", anyOf(is("ROLE_ALUMNI"), is("ALUMNI"))))
                .andExpect(jsonPath("$.data.studentRoll").value("2307003"));
    }

    @Test
    @DisplayName("Student: Create academic recommendation request to Teacher successfully")
    void studentCreateAcademicReferenceRequestToTeacher_success() throws Exception {
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(teacherUserId)
                .purpose("GRADUATE_ADMISSION")
                .targetInstitution("ETH Zurich")
                .targetProgram("MSc in Computer Science")
                .deadline(LocalDateTime.now().plusMonths(1))
                .personalMessage("Respected Sir, I am preparing my application for ETH Zurich MSc in CS. Requesting your kind academic reference letter.")
                .supportingInfo("Thesis under your lab, 1 IEEE paper submitted")
                .build();

        mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.targetInstitution").value("ETH Zurich"))
                .andExpect(jsonPath("$.data.recipientRole", anyOf(is("ROLE_TEACHER"), is("TEACHER"))));
    }

    @Test
    @DisplayName("Security: Unverified student cannot request reference")
    void unverifiedStudentCannotRequestReference_fails() throws Exception {
        String testKuetId = "2307998";
        String testEmail = "unverified.ref@stud.kuet.ac.bd";
        if (userRepository.findByEmail(testEmail).isEmpty()) {
            User unverified = User.builder()
                    .kuetId(testKuetId)
                    .email(testEmail)
                    .name("Unverified Candidate")
                    .department("CSE")
                    .role("ROLE_STUDENT")
                    .password(passwordEncoder.encode("StrongPass123!"))
                    .isVerified(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(unverified);
        }

        String unverifiedToken = obtainAccessToken(testKuetId, "StrongPass123!");

        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(alumniUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution("Microsoft Redmond")
                .personalMessage("Please refer me")
                .build();

        mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + unverifiedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("verified students")));
    }

    @Test
    @DisplayName("Security: Non-student cannot request reference")
    void nonStudentCannotCreateReferenceRequest_fails() throws Exception {
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(teacherUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution("Meta London")
                .personalMessage("Alumni requesting teacher reference")
                .build();

        mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Validation: Requesting reference from self is rejected")
    void requestReferenceFromSelf_fails() throws Exception {
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(studentUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution("Self Corp")
                .personalMessage("Self reference")
                .build();

        mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("yourself")));
    }

    @Test
    @DisplayName("Validation: Duplicate active reference request is rejected")
    void duplicateActiveReferenceRequest_fails() throws Exception {
        String institution = "Amazon Dublin " + System.currentTimeMillis();
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(alumniUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution(institution)
                .personalMessage("Referral request 1")
                .build();

        // 1st request succeeds
        mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // 2nd request for same institution and referee fails
        mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("active reference request")));
    }

    @Test
    @DisplayName("Lifecycle: Full flow (Student request -> Recipient accepts -> Recipient completes recommendation)")
    void fullReferenceRecommendationWorkflow_success() throws Exception {
        String target = "TU Munich MSc Informatics " + System.currentTimeMillis();
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(teacherUserId)
                .purpose("GRADUATE_ADMISSION")
                .targetInstitution(target)
                .targetProgram("Informatics")
                .personalMessage("Applying for MSc Informatics at TUM.")
                .build();

        // 1. Student creates request
        MvcResult createResult = mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long refId = root.path("data").path("id").asLong();

        // 2. Teacher views incoming requests
        mockMvc.perform(
                get("/api/v1/references/incoming")
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + refId + ")]").exists());

        // 3. Teacher accepts request
        ReferenceDecisionRequest decision = ReferenceDecisionRequest.builder()
                .status("ACCEPTED")
                .responseNotes("I am pleased to provide a recommendation for TUM.")
                .build();

        mockMvc.perform(
                patch("/api/v1/references/" + refId + "/decision")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.responseNotes").value("I am pleased to provide a recommendation for TUM."));

        // 4. Teacher submits completed recommendation letter
        RecommendationSubmitRequest recommendation = RecommendationSubmitRequest.builder()
                .recommendationText("To Admissions Committee, TUM: Samun Sadab Wafi ranked in the top 3% of his class with exemplary research potential in distributed systems.")
                .responseNotes("Official KUET letter sealed and submitted.")
                .build();

        mockMvc.perform(
                patch("/api/v1/references/" + refId + "/recommendation")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recommendation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.recommendationText", containsString("Samun Sadab Wafi ranked in the top 3%")));

        // 5. Student views completed recommendation in sent list
        mockMvc.perform(
                get("/api/v1/references/" + refId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.recommendationText", containsString("top 3%")));
    }

    @Test
    @DisplayName("Lifecycle: Student cancels pending request")
    void studentCancelPendingRequest_success() throws Exception {
        String target = "Apple Cupertino " + System.currentTimeMillis();
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(alumniUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution(target)
                .personalMessage("Referral for iOS team")
                .build();

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long refId = root.path("data").path("id").asLong();

        // Student cancels
        mockMvc.perform(
                patch("/api/v1/references/" + refId + "/cancel")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Cannot cancel again or decide on cancelled
        mockMvc.perform(
                patch("/api/v1/references/" + refId + "/cancel")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CANCELLED")));
    }

    @Test
    @DisplayName("Lifecycle: Referee declines request")
    void refereeDeclineRequest_success() throws Exception {
        String target = "Uber Amsterdam " + System.currentTimeMillis();
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(alumniUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution(target)
                .personalMessage("Referral for backend")
                .build();

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long refId = root.path("data").path("id").asLong();

        ReferenceDecisionRequest decline = ReferenceDecisionRequest.builder()
                .status("DECLINED")
                .responseNotes("Unfortunately our team currently only has openings for Senior Staff.")
                .build();

        mockMvc.perform(
                patch("/api/v1/references/" + refId + "/decision")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decline)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }

    @Test
    @DisplayName("Security: Non-referee cannot accept or complete recommendation")
    void nonRefereeCannotDecideOrComplete_fails() throws Exception {
        String target = "Intel Hillsboro " + System.currentTimeMillis();
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(alumniUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution(target)
                .personalMessage("Referral for compiler team")
                .build();

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long refId = root.path("data").path("id").asLong();

        ReferenceDecisionRequest decision = ReferenceDecisionRequest.builder()
                .status("ACCEPTED")
                .build();

        // Teacher tries to decide on request intended for Alumni
        mockMvc.perform(
                patch("/api/v1/references/" + refId + "/decision")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lifecycle: Cannot complete recommendation while status is PENDING")
    void cannotCompleteRecommendationWhenPending_fails() throws Exception {
        String target = "NVIDIA Santa Clara " + System.currentTimeMillis();
        ReferenceRequestCreateRequest req = ReferenceRequestCreateRequest.builder()
                .recipientId(alumniUserId)
                .purpose("JOB_REFERRAL")
                .targetInstitution(target)
                .personalMessage("Referral for GPU kernels")
                .build();

        MvcResult createResult = mockMvc.perform(
                post("/api/v1/references")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long refId = root.path("data").path("id").asLong();

        RecommendationSubmitRequest recommendation = RecommendationSubmitRequest.builder()
                .recommendationText("Premature recommendation")
                .build();

        mockMvc.perform(
                patch("/api/v1/references/" + refId + "/recommendation")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recommendation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ACCEPTED")));
    }
}
