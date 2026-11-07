package com.teligati.backend.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teligati.backend.dto.LoginRequest;
import com.teligati.backend.dto.ProfileUpdateRequest;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ProfileDirectoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String studentToken;
    private String alumniToken;
    private String teacherToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate demo users to retrieve JWT tokens
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
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    // =========================================================================
    // 1. PROFILE /ME ENDPOINTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/profiles/me without token should return 401 Unauthorized")
    void getMyProfileUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/profiles/me as Student returns owner profile with CGPA and role")
    void getMyProfileAsStudent() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kuetId").value("2307003"))
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.data.cgpa").value(3.92))
                .andExpect(jsonPath("$.data.department").value("Computer Science and Engineering"))
                .andExpect(jsonPath("$.data.skills").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/profiles/me as Alumni returns alumni details")
    void getMyProfileAsAlumni() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("tanvir.munich@kuet.ac.bd"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ALUMNI"))
                .andExpect(jsonPath("$.data.currentCompany").value("Tech Corp Munich"))
                .andExpect(jsonPath("$.data.country").value("Germany"))
                .andExpect(jsonPath("$.data.isAvailableForMentorship").value(true))
                .andExpect(jsonPath("$.data.isActivelyHiring").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/profiles/me as Teacher returns faculty details")
    void getMyProfileAsTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("azharul@cse.kuet.ac.bd"))
                .andExpect(jsonPath("$.data.role").value("ROLE_TEACHER"))
                .andExpect(jsonPath("$.data.designation").value("Professor & Head, CSE"))
                .andExpect(jsonPath("$.data.labName").value("Intelligent Systems & Networks Laboratory"));
    }

    // =========================================================================
    // 2. PROFILE UPDATE & SECURITY ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("PUT /api/v1/profiles/me updates student fields successfully")
    void updateStudentProfile() throws Exception {
        ProfileUpdateRequest update = ProfileUpdateRequest.builder()
                .bio("Dedicated distributed systems researcher and open-source contributor")
                .skills("Java 21, Spring Boot 3, Android, Postgres, Redis")
                .projects("Wizards of Teligati v5.0")
                .githubUrl("https://github.com/wafi-updated")
                .linkedinUrl("https://linkedin.com/in/wafi-updated")
                .cgpa(3.95)
                .specialization("Cloud Native Systems")
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bio").value("Dedicated distributed systems researcher and open-source contributor"))
                .andExpect(jsonPath("$.data.skills").value("Java 21, Spring Boot 3, Android, Postgres, Redis"))
                .andExpect(jsonPath("$.data.githubUrl").value("https://github.com/wafi-updated"))
                .andExpect(jsonPath("$.data.cgpa").value(3.95))
                .andExpect(jsonPath("$.data.specialization").value("Cloud Native Systems"));
    }

    @Test
    @DisplayName("Security: Client cannot alter role or elevate permissions via profile update")
    void cannotAlterRoleViaProfileUpdate() throws Exception {
        // Attempting to send role or status tamper
        String maliciousJson = """
                {
                    "bio": "Hacker attempting role elevation",
                    "role": "ROLE_TEACHER",
                    "isVerified": true
                }
                """;

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT")); // Role is unchanged!
    }

    @Test
    @DisplayName("Validation: Invalid CGPA values outside [0.0, 4.0] are rejected with 400")
    void invalidCgpaRejected() throws Exception {
        ProfileUpdateRequest badCgpa = ProfileUpdateRequest.builder()
                .cgpa(4.50) // invalid
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badCgpa)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 3. PROFILE BY ID & PRIVACY RESTRICTIONS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/profiles/{userId} as other user hides private CGPA")
    void getOtherUserProfileHidesCgpa() throws Exception {
        // Teacher viewing student profile (userId = 1 for student)
        mockMvc.perform(get("/api/v1/profiles/1")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.cgpa").doesNotExist()); // Privacy enforced!
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{userId} for nonexistent user returns 404")
    void getNonexistentProfile() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/99999")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. DIRECTORY APIS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/directory/students returns paginated student list without CGPA")
    void getStudentDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/directory/students")
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].roll").value("2307003"))
                .andExpect(jsonPath("$.data.content[0].cgpa").doesNotExist()) // Omitted from public directory
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/directory/students filters by department and name")
    void getStudentDirectoryWithFilter() throws Exception {
        mockMvc.perform(get("/api/v1/directory/students")
                        .header("Authorization", "Bearer " + alumniToken)
                        .param("name", "Wafi")
                        .param("department", "Computer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", containsString("Wafi")));
    }

    @Test
    @DisplayName("GET /api/v1/directory/alumni returns paginated alumni with company and mentorship info")
    void getAlumniDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/directory/alumni")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].currentCompany").value("Tech Corp Munich"))
                .andExpect(jsonPath("$.data.content[0].country").value("Germany"))
                .andExpect(jsonPath("$.data.content[0].isAvailableForMentorship").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/directory/alumni filters by country and company")
    void getAlumniDirectoryWithFilters() throws Exception {
        mockMvc.perform(get("/api/v1/directory/alumni")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("country", "Germany")
                        .param("company", "Tech Corp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", containsString("Tanvir")));
    }

    @Test
    @DisplayName("GET /api/v1/directory/teachers returns faculty directory with lab and research focus")
    void getTeacherDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/directory/teachers")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].name", containsString("Azharul")))
                .andExpect(jsonPath("$.data.content[0].designation").value("Professor & Head, CSE"))
                .andExpect(jsonPath("$.data.content[0].labName").value("Intelligent Systems & Networks Laboratory"));
    }

    @Test
    @DisplayName("GET /api/v1/directory/teachers filters by research focus")
    void getTeacherDirectoryWithFocusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/directory/teachers")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("researchFocus", "Distributed AI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", containsString("Azharul")));
    }

    // =========================================================================
    // 5. ROLE-SPECIFIC FIELD UPDATE VALIDATION (SECURITY AUDIT)
    // =========================================================================

    @Test
    @DisplayName("Security: Student cannot modify faculty designation")
    void studentCannotModifyFacultyDesignation() throws Exception {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .designation("Professor & Dean")
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("faculty members")));
    }

    @Test
    @DisplayName("Security: Student cannot modify alumni hiring status")
    void studentCannotModifyAlumniHiringStatus() throws Exception {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .isActivelyHiring(true)
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("alumni")));
    }

    @Test
    @DisplayName("Security: Faculty member cannot modify student CGPA")
    void facultyCannotModifyStudentCgpa() throws Exception {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .cgpa(3.99)
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("students")));
    }

    @Test
    @DisplayName("Security: Alumni cannot modify student CGPA")
    void alumniCannotModifyStudentCgpa() throws Exception {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .cgpa(4.00)
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("students")));
    }

    @Test
    @DisplayName("Authorization: Faculty member can legitimately update faculty designation and lab name")
    void facultyCanUpdateFacultyAttributes() throws Exception {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .designation("Senior Professor")
                .labName("Advanced Autonomous Robotics Lab")
                .researchFocus("Quantum Machine Intelligence")
                .publicationsCount(45)
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.designation").value("Senior Professor"))
                .andExpect(jsonPath("$.data.labName").value("Advanced Autonomous Robotics Lab"))
                .andExpect(jsonPath("$.data.publicationsCount").value(45));
    }

    @Test
    @DisplayName("Authorization: Alumni can legitimately update actively hiring status")
    void alumniCanUpdateHiringStatus() throws Exception {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .isActivelyHiring(false)
                .build();

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActivelyHiring").value(false));
    }

    // =========================================================================
    // 6. PAGINATION BOUNDARY & RESILIENCE TESTS
    // =========================================================================

    @Test
    @DisplayName("Pagination: Negative page index and non-positive size clamp safely without error")
    void paginationBoundaryClamping() throws Exception {
        mockMvc.perform(get("/api/v1/directory/students")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("page", "-5")
                        .param("size", "-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20)); // Clamped to safe defaults
    }

    @Test
    @DisplayName("Pagination: Out-of-bounds high page index returns empty list without error")
    void paginationOutOfBoundsReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/directory/students")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("page", "999")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.page").value(999));
    }

    @Test
    @DisplayName("Directory: Simultaneous multi-parameter filtering works accurately")
    void simultaneousMultiFilterDirectory() throws Exception {
        mockMvc.perform(get("/api/v1/directory/alumni")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("company", "Tech Corp")
                        .param("country", "Germany")
                        .param("department", "Computer")
                        .param("position", "Senior"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", containsString("Tanvir")))
                .andExpect(jsonPath("$.data.content[0].country").value("Germany"));
    }
}
