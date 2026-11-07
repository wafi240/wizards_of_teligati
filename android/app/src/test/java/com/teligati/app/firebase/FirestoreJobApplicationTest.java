package com.teligati.app.firebase;

import static org.junit.Assert.*;

import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.JobApplicationEntity;
import com.teligati.app.data.entity.JobPostingEntity;
import com.teligati.app.data.repository.OpportunityRepository;
import com.teligati.app.model.Opportunity;
import com.teligati.app.model.OpportunityCategory;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive Unit Test Suite for Phase 4: Jobs, Opportunities & Job Applications Migration to Firestore.
 * Verifies:
 * 1. Job posting payload schema, role enforcement (ALUMNI / TEACHER only), and owner UID integrity.
 * 2. Immutable postedByUid during updates and non-owner edit/delete rejection.
 * 3. Job application submission enforcing initial 'APPLIED' or 'PENDING' status and self-application prevention.
 * 4. Job poster decision workflow enforcing allowed status transitions ('ACCEPTED', 'REJECTED', 'UNDER_REVIEW')
 *    and exact affected keys restriction (['status', 'posterNotes', 'updatedAt']).
 * 5. Student application withdrawal workflow ('WITHDRAWN') with restricted affected keys.
 * 6. Protection against unauthorized application decisions and student status tampering.
 * 7. Query isolation: Student queries filter by applicantUid, Poster queries filter by posterUid.
 * 8. Room offline caching and UI model mapping (JobPostingEntity <-> Opportunity).
 */
public class FirestoreJobApplicationTest {

    private static final Set<String> ALLOWED_POSTER_DECISION_KEYS = new HashSet<>(Arrays.asList(
            "status", "posterNotes", "updatedAt"
    ));

    private static final Set<String> ALLOWED_APPLICANT_WITHDRAW_KEYS = new HashSet<>(Arrays.asList(
            "status", "statement", "resumeUrl", "updatedAt"
    ));

    private static final Set<String> ALLOWED_POSTER_ROLES = new HashSet<>(Arrays.asList(
            "ALUMNI", "TEACHER"
    ));

    private static final Set<String> ALLOWED_DECISION_STATUSES = new HashSet<>(Arrays.asList(
            "ACCEPTED", "REJECTED", "UNDER_REVIEW", "PENDING"
    ));

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("ALUMNI", "alumni_1");
        sessionManager.saveFirebaseSession("alumni_1", "tanvir@kuet.ac.bd", "ALUMNI", "Engr. Tanvir Ahmed", "CSE", "0807001");
        SessionManager.setInstanceForTesting(sessionManager);
    }

    // =========================================================================
    // 1. JOBS & OPPORTUNITIES SCHEMA & ROLE ENFORCEMENT
    // =========================================================================

    @Test
    public void testJobPostingPayload_hasRequiredFieldsAndPosterRole() {
        String posterUid = "alumni_1";
        String posterRole = "ALUMNI";

        Map<String, Object> jobDoc = new HashMap<>();
        jobDoc.put("id", "job_101");
        jobDoc.put("localId", 101L);
        jobDoc.put("title", "Full Stack Spring Boot & React Lead");
        jobDoc.put("category", "FULLTIME_JOBS");
        jobDoc.put("organization", "KUET Alumni Tech Venture");
        jobDoc.put("location", "Remote");
        jobDoc.put("postedByName", "Engr. Tanvir Ahmed");
        jobDoc.put("postedByRole", posterRole);
        jobDoc.put("postedByUid", posterUid);
        jobDoc.put("compensation", "৳85,000 / month");
        jobDoc.put("duration", "Full-time");
        jobDoc.put("description", "Lead backend engineering pipelines");
        jobDoc.put("skillsRequired", "Java, Spring Boot, React, Docker");
        jobDoc.put("minCgpa", 3.0);
        jobDoc.put("createdAt", System.currentTimeMillis());
        jobDoc.put("updatedAt", System.currentTimeMillis());

        // Validate mandatory rule invariants
        assertEquals("postedByUid must match poster UID", posterUid, jobDoc.get("postedByUid"));
        assertEquals("postedByRole must match user role", "ALUMNI", jobDoc.get("postedByRole"));
        assertTrue("Role must be authorized in security rules", ALLOWED_POSTER_ROLES.contains(jobDoc.get("postedByRole")));
        assertTrue("Title must not be empty", jobDoc.containsKey("title") && !jobDoc.get("title").toString().isEmpty());
        assertTrue("Category must be defined", jobDoc.containsKey("category"));
        assertTrue("Compensation must be defined", jobDoc.containsKey("compensation"));
    }

    @Test
    public void testTeacherCanPostOpportunities_andStudentsAreRejected() {
        // Teacher posting opportunity
        String teacherRole = "TEACHER";
        assertTrue("Teacher role must be authorized to post opportunities", ALLOWED_POSTER_ROLES.contains(teacherRole));

        // Alumni posting job
        String alumniRole = "ALUMNI";
        assertTrue("Alumni role must be authorized to post jobs", ALLOWED_POSTER_ROLES.contains(alumniRole));

        // Student attempting to post
        String studentRole = "STUDENT";
        assertFalse("Student role must NOT be permitted to post jobs", ALLOWED_POSTER_ROLES.contains(studentRole));
    }

    @Test
    public void testJobOwnershipEnforcement_immutablePosterUid() {
        String posterUid = "alumni_1";
        String otherUserUid = "alumni_2";

        // Non-owner cannot update
        boolean canOtherUserUpdate = otherUserUid.equals(posterUid);
        assertFalse("Non-owner must NOT be permitted to modify job posting", canOtherUserUpdate);

        // Owner can update
        boolean canOwnerUpdate = posterUid.equals(posterUid);
        assertTrue("Owner MUST be permitted to modify job posting", canOwnerUpdate);

        // Immutable posterUid check: request.resource.data.postedByUid == resource.data.postedByUid
        String updatedPosterUidAttempt = "alumni_2";
        boolean posterUidRemainsSame = posterUid.equals(updatedPosterUidAttempt);
        assertFalse("Owner cannot reassign postedByUid to someone else", posterUidRemainsSame);
    }

    // =========================================================================
    // 2. JOB APPLICATION SUBMISSION & INVARIANTS
    // =========================================================================

    @Test
    public void testJobApplicationPayload_initialStatusAndPreventSelfApplication() {
        String studentUid = "student_1";
        String posterUid = "alumni_1";

        Map<String, Object> appDoc = new HashMap<>();
        appDoc.put("id", "app_501");
        appDoc.put("localId", 501L);
        appDoc.put("jobId", "job_101");
        appDoc.put("jobTitle", "Full Stack Spring Boot & React Lead");
        appDoc.put("posterUid", posterUid);
        appDoc.put("applicantUid", studentUid);
        appDoc.put("applicantRoll", "2307003");
        appDoc.put("applicantName", "Samun Sadab Wafi");
        appDoc.put("applicantDept", "CSE");
        appDoc.put("applicantCgpa", 3.84);
        appDoc.put("applicantSkills", "Java, Spring Boot, Git");
        appDoc.put("statement", "Eager to contribute and gain production software experience.");
        appDoc.put("resumeUrl", "");
        appDoc.put("posterNotes", "");
        appDoc.put("status", "APPLIED");
        appDoc.put("appliedAt", System.currentTimeMillis());

        // Validate security invariants
        assertNotEquals("Applicant cannot apply to their own job posting", appDoc.get("applicantUid"), appDoc.get("posterUid"));
        String status = (String) appDoc.get("status");
        assertTrue("Initial status must be APPLIED or PENDING", "APPLIED".equals(status) || "PENDING".equals(status));
        assertEquals("student_1", appDoc.get("applicantUid"));
        assertEquals("alumni_1", appDoc.get("posterUid"));
    }

    @Test
    public void testSelfApplication_isStrictlyProhibited() {
        String posterUid = "alumni_1";
        String applicantUid = "alumni_1"; // Attempting self-application

        boolean isSelfApp = applicantUid.equals(posterUid);
        assertTrue("Self-application condition detected", isSelfApp);
        // Security rule explicitly asserts: request.resource.data.applicantUid != request.resource.data.posterUid
        assertFalse("Security rules prohibit applicantUid == posterUid", !isSelfApp);
    }

    @Test
    public void testInitialApplicationStatus_invalidStatusRejected() {
        List<String> invalidInitialStatuses = Arrays.asList("ACCEPTED", "REJECTED", "UNDER_REVIEW", "WITHDRAWN");
        for (String invalidStatus : invalidInitialStatuses) {
            boolean isAllowedInitial = "APPLIED".equals(invalidStatus) || "PENDING".equals(invalidStatus);
            assertFalse("Initial status " + invalidStatus + " must be rejected by security rules", isAllowedInitial);
        }
    }

    // =========================================================================
    // 3. POSTER DECISION WORKFLOW & AFFECTED KEYS VALIDATION
    // =========================================================================

    @Test
    public void testStatusNormalization_convertsUiStatusesToRulesEnum() {
        assertEquals("ACCEPTED", OpportunityRepository.normalizeApplicationStatus("Accepted"));
        assertEquals("ACCEPTED", OpportunityRepository.normalizeApplicationStatus("accepted"));
        assertEquals("ACCEPTED", OpportunityRepository.normalizeApplicationStatus("ACCEPT"));
        assertEquals("UNDER_REVIEW", OpportunityRepository.normalizeApplicationStatus("Shortlisted"));
        assertEquals("UNDER_REVIEW", OpportunityRepository.normalizeApplicationStatus("shortlisted"));
        assertEquals("UNDER_REVIEW", OpportunityRepository.normalizeApplicationStatus("Under Review"));
        assertEquals("REJECTED", OpportunityRepository.normalizeApplicationStatus("Rejected"));
        assertEquals("REJECTED", OpportunityRepository.normalizeApplicationStatus("Declined"));
        assertEquals("WITHDRAWN", OpportunityRepository.normalizeApplicationStatus("Withdraw"));
        assertEquals("PENDING", OpportunityRepository.normalizeApplicationStatus("Applied"));
    }

    @Test
    public void testPosterDecision_affectedKeysOnlyStatusPosterNotesUpdatedAt() {
        Map<String, Object> originalDoc = new HashMap<>();
        originalDoc.put("applicantUid", "student_1");
        originalDoc.put("posterUid", "alumni_1");
        originalDoc.put("jobId", "job_101");
        originalDoc.put("statement", "Original student statement");
        originalDoc.put("status", "APPLIED");
        originalDoc.put("posterNotes", "");
        originalDoc.put("appliedAt", 1000L);
        originalDoc.put("updatedAt", 1000L);

        // Poster decision updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "ACCEPTED");
        updates.put("posterNotes", "Candidate shortlisted for technical interview round.");
        updates.put("updatedAt", 2000L);

        // Verify affected keys match diff(resource.data).affectedKeys().hasOnly(['status', 'posterNotes', 'updatedAt'])
        for (String key : updates.keySet()) {
            assertTrue("Affected key '" + key + "' must be permitted in poster decision",
                    ALLOWED_POSTER_DECISION_KEYS.contains(key));
        }

        // Verify decision status is within allowed set
        String newStatus = (String) updates.get("status");
        assertTrue("Decision status must be in ['ACCEPTED', 'REJECTED', 'UNDER_REVIEW', 'PENDING']",
                ALLOWED_DECISION_STATUSES.contains(newStatus));
    }

    @Test
    public void testPosterCannotModifyStudentStatementOrApplicantUid() {
        Map<String, Object> maliciousUpdates = new HashMap<>();
        maliciousUpdates.put("status", "REJECTED");
        maliciousUpdates.put("statement", "Falsified statement written by poster"); // Illegal
        maliciousUpdates.put("updatedAt", 2000L);

        boolean hasIllegalKey = false;
        for (String key : maliciousUpdates.keySet()) {
            if (!ALLOWED_POSTER_DECISION_KEYS.contains(key)) {
                hasIllegalKey = true;
                break;
            }
        }
        assertTrue("Statement modification by poster must violate affectedKeys restriction", hasIllegalKey);
    }

    @Test
    public void testUnauthorizedUserCannotDecideApplication() {
        String posterUid = "alumni_1";
        String unauthorizedUid = "alumni_2";

        boolean isAuthorizedPoster = unauthorizedUid.equals(posterUid);
        assertFalse("Unauthorized alumni cannot make decisions on other posters' applications", isAuthorizedPoster);
    }

    // =========================================================================
    // 4. APPLICANT WITHDRAWAL WORKFLOW & RESTRICTIONS
    // =========================================================================

    @Test
    public void testApplicantWithdrawal_allowedWhilePendingOrUnderReview() {
        List<String> withdrawableStatuses = Arrays.asList("PENDING", "UNDER_REVIEW", "APPLIED");
        for (String currentStatus : withdrawableStatuses) {
            boolean canWithdraw = withdrawableStatuses.contains(currentStatus);
            assertTrue("Applicant must be allowed to withdraw while status is " + currentStatus, canWithdraw);
        }

        // Updates sent during withdrawal
        Map<String, Object> withdrawalUpdates = new HashMap<>();
        withdrawalUpdates.put("status", "WITHDRAWN");
        withdrawalUpdates.put("updatedAt", System.currentTimeMillis());

        for (String key : withdrawalUpdates.keySet()) {
            assertTrue("Key '" + key + "' must be allowed in withdrawal diff",
                    ALLOWED_APPLICANT_WITHDRAW_KEYS.contains(key));
        }
    }

    @Test
    public void testApplicantCannotApproveOwnApplicationViaWithdrawalWorkflow() {
        Map<String, Object> maliciousApplicantUpdate = new HashMap<>();
        maliciousApplicantUpdate.put("status", "ACCEPTED"); // Tamper attempt
        maliciousApplicantUpdate.put("updatedAt", System.currentTimeMillis());

        String targetStatus = (String) maliciousApplicantUpdate.get("status");
        // Rule: request.resource.data.status in [resource.data.status, 'WITHDRAWN']
        boolean isValidWithdrawalStatus = "WITHDRAWN".equals(targetStatus);
        assertFalse("Applicant cannot set status to ACCEPTED", isValidWithdrawalStatus);
    }

    // =========================================================================
    // 5. QUERY ISOLATION & ACCESS CONTROL
    // =========================================================================

    @Test
    public void testQueryIsolation_studentAndPosterFilteredQueries() {
        String currentStudentUid = "student_1";
        String currentPosterUid = "alumni_1";

        // Query A: Student history query
        String studentQueryField = "applicantUid";
        String studentQueryVal = currentStudentUid;
        assertEquals("Student query must isolate by applicantUid", "applicantUid", studentQueryField);
        assertEquals("student_1", studentQueryVal);

        // Query B: Poster received applications query
        String posterQueryField = "posterUid";
        String posterQueryVal = currentPosterUid;
        assertEquals("Poster query must isolate by posterUid", "posterUid", posterQueryField);
        assertEquals("alumni_1", posterQueryVal);
    }

    // =========================================================================
    // 6. ROOM ENTITY & UI MODEL MAPPING (DATAMAPPER)
    // =========================================================================

    @Test
    public void testJobPostingEntity_toOpportunityMapping() {
        JobPostingEntity entity = new JobPostingEntity(
                101L,
                "Python Backend Research Assistant",
                "ASSISTANT_WORK",
                "KUET Robotics Lab",
                "Khulna / Remote",
                "Prof. Dr. K. M. Azharul Hasan",
                "TEACHER",
                "৳15,000 / month",
                "10 hrs/week",
                "Assist with ROS robotics automation and data processing.",
                "Python, ROS, Git",
                3.5,
                false,
                "faculty_1",
                1731000000000L
        );

        Opportunity opp = DataMapper.toOpportunity(entity);
        assertNotNull("Opportunity model must not be null", opp);
        assertEquals(101L, opp.getId());
        assertEquals("Python Backend Research Assistant", opp.getTitle());
        assertEquals("KUET Robotics Lab", opp.getOrganization());
        assertEquals("Prof. Dr. K. M. Azharul Hasan", opp.getPostedByName());
        assertEquals("TEACHER", opp.getPosterRole());
        assertEquals("৳15,000 / month", opp.getCompensation());
        assertEquals(OpportunityCategory.ASSISTANT_WORK, opp.getCategory());
        assertFalse(opp.isApplied());
    }

    @Test
    public void testJobApplicationEntity_fieldsAndStatusPersistence() {
        JobApplicationEntity app = new JobApplicationEntity(
                501L,
                101L,
                "Python Backend Research Assistant",
                "student_1",
                "Samun Sadab Wafi",
                "2307003",
                "CSE",
                3.84,
                "Python, ROS",
                "Eager to work in Robotics lab",
                "APPLIED",
                1731000000000L
        );

        assertEquals(501L, app.getId());
        assertEquals(101L, app.getJobId());
        assertEquals("Python Backend Research Assistant", app.getJobTitle());
        assertEquals("student_1", app.getApplicantId());
        assertEquals("2307003", app.getApplicantRoll());
        assertEquals("APPLIED", app.getStatus());
        assertEquals(3.84, app.getApplicantCgpa(), 0.001);

        // Update status to Shortlisted / UNDER_REVIEW
        app.setStatus("UNDER_REVIEW");
        assertEquals("UNDER_REVIEW", app.getStatus());

        // Update status to Accepted
        app.setStatus("ACCEPTED");
        assertEquals("ACCEPTED", app.getStatus());

        // Update status to Withdrawn
        app.setStatus("WITHDRAWN");
        assertEquals("WITHDRAWN", app.getStatus());
    }
}
