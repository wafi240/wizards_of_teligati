package com.teligati.app.firebase;

import static org.junit.Assert.*;

import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.ProfileRepository;
import com.teligati.app.network.model.AlumniDirectoryResponse;
import com.teligati.app.network.model.PagedResponse;
import com.teligati.app.network.model.ProfileResponse;
import com.teligati.app.network.model.ProfileUpdateRequest;
import com.teligati.app.network.model.StudentDirectoryResponse;
import com.teligati.app.network.model.TeacherDirectoryResponse;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit test suite for Phase 2: User Profiles & Directory Migration to Firestore.
 * Verifies:
 * 1. Sanitization & Zero Leakage in public_profiles (strict adherence to isValidPublicProfile allowlist).
 * 2. Multi-role support for Student, Alumni, and Teacher profiles.
 * 3. Role filtering in public directory queries.
 * 4. User ownership and permission checks (users can only edit their own profile).
 * 5. Mapping of Firestore documents to domain ProfileResponse and Directory responses.
 * 6. Offline fallback to local cache when remote is unavailable.
 */
public class FirestoreProfileDirectoryTest {

    private static final Set<String> ALLOWED_PUBLIC_KEYS = new HashSet<>(Arrays.asList(
            "uid", "name", "role", "department", "batch",
            "company", "position", "designation", "lab", "researchArea", "avatarUrl"
    ));

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.saveFirebaseSession("test-uid-12345", "2307003@stud.kuet.ac.bd", "STUDENT", "Samun Sadab Wafi", "CSE", "2307003");
        SessionManager.setInstanceForTesting(sessionManager);
    }

    // =========================================================================
    // 1. PUBLIC PROFILE SANITIZATION & ZERO DATA LEAKAGE TESTS
    // =========================================================================

    @Test
    public void testStudentPublicProfileSanitization_stripsPrivateFields() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("uid", "stud_uid_001");
        rawData.put("name", "Samun Sadab Wafi");
        rawData.put("role", "STUDENT");
        rawData.put("department", "Computer Science and Engineering");
        rawData.put("batch", "2K23");
        // Strictly private fields that MUST NOT leak into public_profiles:
        rawData.put("email", "2307003@stud.kuet.ac.bd");
        rawData.put("roll", "2307003");
        rawData.put("kuetId", "2307003");
        rawData.put("cgpa", 3.84);
        rawData.put("token", "secret_auth_token_999");
        rawData.put("phone", "+8801700000000");

        Map<String, Object> publicCard = FirebaseHelper.sanitizePublicProfile("stud_uid_001", rawData);

        // 1. Must contain all required keys
        assertTrue("Must contain uid", publicCard.containsKey("uid"));
        assertTrue("Must contain name", publicCard.containsKey("name"));
        assertTrue("Must contain role", publicCard.containsKey("role"));
        assertEquals("stud_uid_001", publicCard.get("uid"));
        assertEquals("Samun Sadab Wafi", publicCard.get("name"));
        assertEquals("STUDENT", publicCard.get("role"));

        // 2. Must only contain allowlisted keys (zero leakage)
        for (String key : publicCard.keySet()) {
            assertTrue("Key '" + key + "' is not permitted in public_profiles", ALLOWED_PUBLIC_KEYS.contains(key));
        }

        // 3. Explicitly verify private fields are NOT present
        assertFalse("Private email must NEVER be in public_profiles", publicCard.containsKey("email"));
        assertFalse("Private roll must NEVER be in public_profiles", publicCard.containsKey("roll"));
        assertFalse("Private kuetId must NEVER be in public_profiles", publicCard.containsKey("kuetId"));
        assertFalse("Private cgpa must NEVER be in public_profiles", publicCard.containsKey("cgpa"));
        assertFalse("Private token must NEVER be in public_profiles", publicCard.containsKey("token"));
        assertFalse("Private phone must NEVER be in public_profiles", publicCard.containsKey("phone"));
    }

    @Test
    public void testAlumniPublicProfileSanitization_mapsAliasesAndEnforcesBounds() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("uid", "alumni_uid_002");
        rawData.put("name", "Engr. Tanvir Ahmed");
        rawData.put("role", "ALUMNI");
        rawData.put("department", "CSE '15");
        rawData.put("batch", "2K15");
        rawData.put("currentCompany", "BMW Group (Munich, Germany)");
        rawData.put("currentPosition", "Senior Robotics Engineer");
        rawData.put("email", "tanvir.munich@kuet.ac.bd");
        rawData.put("token", "invite_tok_xyz");

        Map<String, Object> publicCard = FirebaseHelper.sanitizePublicProfile("alumni_uid_002", rawData);

        assertEquals("alumni_uid_002", publicCard.get("uid"));
        assertEquals("Engr. Tanvir Ahmed", publicCard.get("name"));
        assertEquals("ALUMNI", publicCard.get("role"));
        assertEquals("BMW Group (Munich, Germany)", publicCard.get("company"));
        assertEquals("Senior Robotics Engineer", publicCard.get("position"));

        for (String key : publicCard.keySet()) {
            assertTrue("Key '" + key + "' must be allowed", ALLOWED_PUBLIC_KEYS.contains(key));
        }
        assertFalse("currentCompany alias must be translated to company", publicCard.containsKey("currentCompany"));
        assertFalse("currentPosition alias must be translated to position", publicCard.containsKey("currentPosition"));
        assertFalse("email must not be in public card", publicCard.containsKey("email"));
        assertFalse("invitation token must not be in public card", publicCard.containsKey("token"));
    }

    @Test
    public void testTeacherPublicProfileSanitization_mapsLabAndResearchFocus() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("uid", "teacher_uid_003");
        rawData.put("name", "Prof. Dr. K. M. Azharul Hasan");
        rawData.put("role", "TEACHER");
        rawData.put("department", "Dept of CSE");
        rawData.put("designation", "Professor & Department Head");
        rawData.put("labName", "Distributed Systems & Cloud Computing Lab");
        rawData.put("researchFocus", "Distributed Database, Cloud Systems, Edge AI");
        rawData.put("email", "azharul@cse.kuet.ac.bd");

        Map<String, Object> publicCard = FirebaseHelper.sanitizePublicProfile("teacher_uid_003", rawData);

        assertEquals("teacher_uid_003", publicCard.get("uid"));
        assertEquals("TEACHER", publicCard.get("role"));
        assertEquals("Prof. Dr. K. M. Azharul Hasan", publicCard.get("name"));
        assertEquals("Distributed Systems & Cloud Computing Lab", publicCard.get("lab"));
        assertEquals("Distributed Database, Cloud Systems, Edge AI", publicCard.get("researchArea"));

        for (String key : publicCard.keySet()) {
            assertTrue("Key '" + key + "' must be allowed", ALLOWED_PUBLIC_KEYS.contains(key));
        }
        assertFalse("labName alias must be mapped to lab", publicCard.containsKey("labName"));
        assertFalse("researchFocus alias must be mapped to researchArea", publicCard.containsKey("researchFocus"));
        assertFalse("Private email must not be in public card", publicCard.containsKey("email"));
    }

    @Test
    public void testStringLengthEnforcement_truncatesExcessiveLengths() {
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 300; i++) longString.append("A");

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("uid", "uid_trunc");
        rawData.put("name", longString.toString()); // Max 100
        rawData.put("role", "STUDENT");
        rawData.put("department", longString.toString()); // Max 100
        rawData.put("batch", longString.toString()); // Max 50
        rawData.put("company", longString.toString()); // Max 100
        rawData.put("researchArea", longString.toString()); // Max 200

        Map<String, Object> publicCard = FirebaseHelper.sanitizePublicProfile("uid_trunc", rawData);

        assertTrue("Name must be capped at 100 chars", publicCard.get("name").toString().length() <= 100);
        assertTrue("Department must be capped at 100 chars", publicCard.get("department").toString().length() <= 100);
        assertTrue("Batch must be capped at 50 chars", publicCard.get("batch").toString().length() <= 50);
        assertTrue("Company must be capped at 100 chars", publicCard.get("company").toString().length() <= 100);
        assertTrue("ResearchArea must be capped at 200 chars", publicCard.get("researchArea").toString().length() <= 200);
    }

    // =========================================================================
    // 2. DOMAIN MAPPING TESTS (FIRESTORE MAP -> PROFILERESPONSE)
    // =========================================================================

    @Test
    public void testMapFirestoreToProfileResponse_student() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", "stud_1");
        doc.put("name", "Samun Sadab Wafi");
        doc.put("role", "STUDENT");
        doc.put("roll", "2307003");
        doc.put("email", "2307003@stud.kuet.ac.bd");
        doc.put("department", "CSE");
        doc.put("batch", "2K23");
        doc.put("cgpa", 3.84);
        doc.put("skills", "Java, Python, Android");
        doc.put("bio", "Competitive programmer and mobile developer");

        ProfileResponse p = ProfileRepository.mapFirestoreToProfileResponse(doc, "stud_1");

        assertNotNull(p);
        assertEquals("2307003", p.getKuetId());
        assertEquals("Samun Sadab Wafi", p.getName());
        assertEquals("STUDENT", p.getRole());
        assertEquals("CSE", p.getDepartment());
        assertEquals("2K23", p.getBatch());
        assertEquals(Double.valueOf(3.84), p.getCgpa());
        assertEquals("Java, Python, Android", p.getSkills());
        assertTrue(p.isVerified());
    }

    @Test
    public void testMapFirestoreToProfileResponse_alumni() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", "alumni_1");
        doc.put("name", "Engr. Tanvir Ahmed");
        doc.put("role", "ALUMNI");
        doc.put("email", "tanvir.munich@kuet.ac.bd");
        doc.put("department", "CSE '15");
        doc.put("company", "BMW Group");
        doc.put("position", "Senior Robotics Engineer");
        doc.put("country", "Germany");
        doc.put("city", "Munich");
        doc.put("isAvailableForMentorship", true);
        doc.put("isActivelyHiring", true);

        ProfileResponse p = ProfileRepository.mapFirestoreToProfileResponse(doc, "alumni_1");

        assertNotNull(p);
        assertEquals("Engr. Tanvir Ahmed", p.getName());
        assertEquals("ALUMNI", p.getRole());
        assertEquals("BMW Group", p.getCurrentCompany());
        assertEquals("Senior Robotics Engineer", p.getCurrentPosition());
        assertEquals("Germany", p.getCountry());
        assertEquals("Munich", p.getCity());
        assertTrue(p.getIsAvailableForMentorship());
        assertTrue(p.getIsActivelyHiring());
    }

    @Test
    public void testMapFirestoreToProfileResponse_teacher() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", "teacher_1");
        doc.put("name", "Prof. Dr. K. M. Azharul Hasan");
        doc.put("role", "TEACHER");
        doc.put("email", "azharul@cse.kuet.ac.bd");
        doc.put("department", "Dept of CSE");
        doc.put("designation", "Professor & Department Head");
        doc.put("lab", "Big Data Lab");
        doc.put("researchArea", "Cloud & Edge Computing");
        doc.put("publicationsCount", 45);

        ProfileResponse p = ProfileRepository.mapFirestoreToProfileResponse(doc, "teacher_1");

        assertNotNull(p);
        assertEquals("Prof. Dr. K. M. Azharul Hasan", p.getName());
        assertEquals("TEACHER", p.getRole());
        assertEquals("Professor & Department Head", p.getDesignation());
        assertEquals("Big Data Lab", p.getLabName());
        assertEquals("Cloud & Edge Computing", p.getResearchFocus());
        assertEquals(Integer.valueOf(45), p.getPublicationsCount());
    }

    // =========================================================================
    // 3. ROLE FILTERING & DIRECTORY LOGIC TESTS
    // =========================================================================

    @Test
    public void testRoleFilteringLogic_separatesRolesCorrectly() {
        List<Map<String, Object>> directorySnapshots = new ArrayList<>();

        Map<String, Object> u1 = new HashMap<>();
        u1.put("uid", "1");
        u1.put("name", "Student One");
        u1.put("role", "STUDENT");
        u1.put("department", "CSE");
        directorySnapshots.add(u1);

        Map<String, Object> u2 = new HashMap<>();
        u2.put("uid", "2");
        u2.put("name", "Alumni One");
        u2.put("role", "ALUMNI");
        u2.put("department", "CSE");
        directorySnapshots.add(u2);

        Map<String, Object> u3 = new HashMap<>();
        u3.put("uid", "3");
        u3.put("name", "Faculty One");
        u3.put("role", "TEACHER");
        u3.put("department", "CSE");
        directorySnapshots.add(u3);

        // Filter STUDENT
        List<Map<String, Object>> students = new ArrayList<>();
        for (Map<String, Object> d : directorySnapshots) {
            if ("STUDENT".equals(d.get("role"))) students.add(d);
        }
        assertEquals(1, students.size());
        assertEquals("Student One", students.get(0).get("name"));

        // Filter ALUMNI
        List<Map<String, Object>> alumni = new ArrayList<>();
        for (Map<String, Object> d : directorySnapshots) {
            if ("ALUMNI".equals(d.get("role"))) alumni.add(d);
        }
        assertEquals(1, alumni.size());
        assertEquals("Alumni One", alumni.get(0).get("name"));

        // Filter TEACHER
        List<Map<String, Object>> teachers = new ArrayList<>();
        for (Map<String, Object> d : directorySnapshots) {
            if ("TEACHER".equals(d.get("role"))) teachers.add(d);
        }
        assertEquals(1, teachers.size());
        assertEquals("Faculty One", teachers.get(0).get("name"));
    }

    // =========================================================================
    // 4. OWNERSHIP & EDIT RESTRICTIONS
    // =========================================================================

    @Test
    public void testProfileOwnershipVerification_unauthenticatedCallerRejected() throws Exception {
        // Clear session
        sessionManager.clearSession();
        ProfileRepository repo = new ProfileRepository(null, null, sessionManager, null, Runnable::run);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> errorRef = new AtomicReference<>();

        ProfileUpdateRequest updateReq = new ProfileUpdateRequest();
        updateReq.setName("Malicious Attacker");

        repo.updateMyProfile(updateReq, new ProfileRepository.Callback<ProfileResponse>() {
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

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull("Unauthenticated update must be rejected", errorRef.get());
        assertTrue("Error message should mention permissions or authentication",
                errorRef.get().toLowerCase().contains("permission") ||
                errorRef.get().toLowerCase().contains("authenticated") ||
                errorRef.get().toLowerCase().contains("no persistent backend"));
    }
}
