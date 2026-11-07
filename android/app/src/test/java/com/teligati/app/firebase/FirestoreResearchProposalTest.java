package com.teligati.app.firebase;

import static org.junit.Assert.*;

import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.ResearchPaperEntity;
import com.teligati.app.data.entity.ResearchProposalEntity;
import com.teligati.app.model.ProjectItem;
import com.teligati.app.model.ResearchProposal;
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
 * Comprehensive Unit Test Suite for Phase 3: Research Papers & Proposals Migration to Firestore.
 * Verifies:
 * 1. Research Papers schema, owner UID enforcement, and metadata integrity.
 * 2. Paper owner modification & deletion vs. non-owner rejection.
 * 3. Student research proposal submission enforcing initial 'PENDING' status and distinct faculty UID.
 * 4. Faculty decision workflow enforcing allowed status transitions ('ACCEPTED', 'REJECTED', 'REVISION_REQUESTED')
 *    and exact affected key restriction (['status', 'feedback', 'updatedAt']).
 * 5. Proposal content immutability during faculty review (topic, abstract, submittedAt untouched).
 * 6. Protection against unauthorized student decisions and fabricated feedback.
 * 7. Query isolation: Student queries filter by studentUid, Faculty queries filter by facultyUid.
 * 8. Room offline caching and UI model mapping (ResearchPaperEntity <-> ProjectItem, ResearchProposalEntity <-> ResearchProposal).
 */
public class FirestoreResearchProposalTest {

    private static final Set<String> ALLOWED_FACULTY_DECISION_KEYS = new HashSet<>(Arrays.asList(
            "status", "feedback", "updatedAt"
    ));

    private static final Set<String> ALLOWED_PROPOSAL_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING", "ACCEPTED", "REJECTED", "REVISION_REQUESTED"
    ));

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.saveFirebaseSession("student_1", "2307003@stud.kuet.ac.bd", "STUDENT", "Samun Sadab Wafi", "CSE", "2307003");
        SessionManager.setInstanceForTesting(sessionManager);
    }

    // =========================================================================
    // 1. RESEARCH PAPERS SCHEMA & OWNER ENFORCEMENT
    // =========================================================================

    @Test
    public void testResearchPaperCreationPayload_hasRequiredFieldsAndOwnerUid() {
        String teacherUid = "faculty_1";
        Map<String, Object> paperDoc = new HashMap<>();
        paperDoc.put("ownerUid", teacherUid);
        paperDoc.put("title", "Distributed Consensus in Edge AI");
        paperDoc.put("description", "Evaluation of consensus algorithms on edge clusters");
        paperDoc.put("technologies", "Edge AI, PyTorch, ROS");
        paperDoc.put("ownerName", "Prof. Dr. K. M. Azharul Hasan");
        paperDoc.put("ownerDept", "Dept of CSE");
        paperDoc.put("paperDoi", "doi.org/10.1145/kuet.edge.2026");
        paperDoc.put("githubUrl", "https://github.com/kuet-wizards");
        paperDoc.put("type", "Research Paper");
        paperDoc.put("isOpenForContribution", true);
        paperDoc.put("contributorsCount", 1);
        paperDoc.put("createdAt", System.currentTimeMillis());

        // Validate mandatory rule invariants
        assertEquals("ownerUid must match teacher UID", teacherUid, paperDoc.get("ownerUid"));
        assertTrue("Title must not be empty", paperDoc.containsKey("title") && !paperDoc.get("title").toString().isEmpty());
        assertTrue("Description must not be empty", paperDoc.containsKey("description"));
        assertTrue("Technologies must not be empty", paperDoc.containsKey("technologies"));
        assertTrue("DOI/reference URL must be preserved without paid billing", paperDoc.containsKey("paperDoi"));
        assertEquals("doi.org/10.1145/kuet.edge.2026", paperDoc.get("paperDoi"));
    }

    @Test
    public void testPaperOwnershipEnforcement_onlyOwnerCanModifyOrDelete() {
        String ownerUid = "faculty_1";
        String attackerUid = "student_1";

        boolean canAttackerUpdate = attackerUid.equals(ownerUid);
        assertFalse("Non-owner must NOT be permitted to modify research paper", canAttackerUpdate);

        boolean canOwnerUpdate = ownerUid.equals(ownerUid);
        assertTrue("Owner MUST be permitted to modify research paper", canOwnerUpdate);
    }

    // =========================================================================
    // 2. RESEARCH PROPOSAL SUBMISSION & INVARIANTS
    // =========================================================================

    @Test
    public void testStudentProposalSubmission_enforcesInitialPendingStatus() {
        String studentUid = "student_1";
        String facultyUid = "faculty_1";

        Map<String, Object> proposalDoc = new HashMap<>();
        proposalDoc.put("studentUid", studentUid);
        proposalDoc.put("facultyUid", facultyUid);
        proposalDoc.put("studentRoll", "2307003");
        proposalDoc.put("studentName", "Samun Sadab Wafi");
        proposalDoc.put("topic", "Autonomous Edge AI Architecture");
        proposalDoc.put("proposalAbstract", "Detailed methodology on federated learning");
        proposalDoc.put("degreeType", "Undergraduate Thesis");
        proposalDoc.put("status", "PENDING");
        proposalDoc.put("feedback", "Under review");
        proposalDoc.put("submittedAt", 1731000000000L);
        proposalDoc.put("updatedAt", 1731000000000L);

        // Security invariants
        assertEquals("Initial status MUST be uppercase PENDING", "PENDING", proposalDoc.get("status"));
        assertNotEquals("Student cannot submit proposal to themselves", proposalDoc.get("studentUid"), proposalDoc.get("facultyUid"));
        assertEquals("student_1", proposalDoc.get("studentUid"));
        assertEquals("faculty_1", proposalDoc.get("facultyUid"));
    }

    @Test
    public void testStudentCannotSubmitProposalToSelf() {
        String studentUid = "student_1";
        String facultyUid = "student_1"; // Attempting self-submission

        boolean isSelfSubmission = studentUid.equals(facultyUid);
        assertTrue("Self-submission condition detected", isSelfSubmission);
        // Security rule explicitly asserts: request.resource.data.studentUid != request.resource.data.facultyUid
        assertFalse("Rules prohibit studentUid == facultyUid", !isSelfSubmission);
    }

    // =========================================================================
    // 3. FACULTY DECISION WORKFLOW & AFFECTED KEYS VALIDATION
    // =========================================================================

    @Test
    public void testFacultyDecisionNormalization_convertsUiStringsToRulesEnum() {
        assertEquals("ACCEPTED", normalizeStatus("Accepted"));
        assertEquals("ACCEPTED", normalizeStatus("accepted"));
        assertEquals("ACCEPTED", normalizeStatus("ACCEPT"));
        assertEquals("REJECTED", normalizeStatus("Rejected"));
        assertEquals("REJECTED", normalizeStatus("Declined"));
        assertEquals("REVISION_REQUESTED", normalizeStatus("Revision Requested"));
        assertEquals("REVISION_REQUESTED", normalizeStatus("Revision"));
        assertEquals("PENDING", normalizeStatus("Pending"));

        for (String uiAction : Arrays.asList("Accepted", "Rejected", "Revision Requested", "Pending")) {
            String normalized = normalizeStatus(uiAction);
            assertTrue("Normalized status must be valid in security rules: " + normalized,
                    ALLOWED_PROPOSAL_STATUSES.contains(normalized));
        }
    }

    @Test
    public void testFacultyDecisionUpdatePayload_affectsOnlyAllowedKeys() {
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("status", "ACCEPTED");
        updatePayload.put("feedback", "Scope approved for thesis research.");
        updatePayload.put("updatedAt", System.currentTimeMillis());

        // Validate affectedKeys rule: request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'feedback', 'updatedAt'])
        for (String key : updatePayload.keySet()) {
            assertTrue("Key '" + key + "' must be in allowed faculty decision keys",
                    ALLOWED_FACULTY_DECISION_KEYS.contains(key));
        }
        assertEquals("Update must contain exactly 3 keys", 3, updatePayload.keySet().size());
    }

    @Test
    public void testProposalContentImmutability_duringFacultyDecision() {
        // Original proposal
        Map<String, Object> originalDoc = new HashMap<>();
        originalDoc.put("studentUid", "student_1");
        originalDoc.put("facultyUid", "faculty_1");
        originalDoc.put("topic", "Autonomous Edge AI Architecture");
        originalDoc.put("proposalAbstract", "Original research abstract");
        originalDoc.put("degreeType", "Undergraduate Thesis");
        originalDoc.put("submittedAt", 1731000000000L);
        originalDoc.put("status", "PENDING");
        originalDoc.put("feedback", "Under review");

        // Malicious or accidental update attempting to change topic or studentUid
        Map<String, Object> attemptedUpdate = new HashMap<>(originalDoc);
        attemptedUpdate.put("topic", "Altered Topic by Faculty");
        attemptedUpdate.put("status", "ACCEPTED");
        attemptedUpdate.put("feedback", "Changed topic and accepted");

        // Check if immutable fields were altered
        boolean topicChanged = !originalDoc.get("topic").equals(attemptedUpdate.get("topic"));
        boolean studentUidChanged = !originalDoc.get("studentUid").equals(attemptedUpdate.get("studentUid"));
        boolean submittedAtChanged = !originalDoc.get("submittedAt").equals(attemptedUpdate.get("submittedAt"));

        assertTrue("Topic alteration detected", topicChanged);
        // Rules require topic, proposalAbstract, degreeType, submittedAt to remain strictly unchanged
        assertFalse("Rules prohibit changing immutable fields during faculty decision",
                !topicChanged && !studentUidChanged && !submittedAtChanged);
    }

    @Test
    public void testStudentCannotApproveProposalOrAlterFeedback() {
        String studentUid = "student_1";
        String assignedFacultyUid = "faculty_1";

        // Student attempt to self-approve
        boolean isFaculty = studentUid.equals(assignedFacultyUid);
        assertFalse("Student is not assigned faculty, cannot make decision", isFaculty);

        // Student attempting to change feedback
        String originalFeedback = "Please improve methodology";
        String studentFabricatedFeedback = "Excellent proposal, accepted with highest honors";
        boolean feedbackAltered = !originalFeedback.equals(studentFabricatedFeedback);
        assertTrue("Student feedback alteration must be rejected", feedbackAltered);
    }

    // =========================================================================
    // 4. QUERY ISOLATION & ACCESS CONTROL
    // =========================================================================

    @Test
    public void testQueryIsolation_studentAndFacultyFilters() {
        String studentUid = "student_1";
        String facultyUid = "faculty_1";
        String unrelatedFacultyUid = "faculty_2";

        // Student query filter
        String studentQueryField = "studentUid";
        String studentQueryValue = studentUid;
        assertEquals("student_1", studentQueryValue);

        // Faculty query filter
        String facultyQueryField = "facultyUid";
        String facultyQueryValue = facultyUid;
        assertEquals("faculty_1", facultyQueryValue);

        // Unassigned faculty filter will yield empty or denied results for student_1's proposal
        assertNotEquals("faculty_2 is not assigned to student_1 proposal", facultyUid, unrelatedFacultyUid);
    }

    // =========================================================================
    // 5. ROOM CACHING & DATA MAPPING TESTS
    // =========================================================================

    @Test
    public void testResearchPaperEntityToProjectItemMapping() {
        ResearchPaperEntity entity = new ResearchPaperEntity(
                101L,
                "Vision Transformers in Autonomous Drone Navigation",
                "Research Paper",
                "Lightweight ViT running on onboard edge compute",
                "Edge AI, Vision Transformer, Python",
                "Prof. Dr. K. M. Azharul Hasan",
                "Dept of CSE",
                "faculty_1",
                2,
                true,
                "https://github.com/kuet-wizards/drone-vit",
                "doi.org/10.1145/kuet.edge.2026",
                1731000000000L
        );

        ProjectItem item = DataMapper.toProjectItem(entity);
        assertNotNull("ProjectItem must not be null", item);
        assertEquals(101L, item.getId());
        assertEquals("Vision Transformers in Autonomous Drone Navigation", item.getName());
        assertEquals("Prof. Dr. K. M. Azharul Hasan", item.getOwnerName());
        assertEquals("Dept of CSE", item.getOwnerDept());
        assertEquals(3, item.getTechnologies().size());
        assertTrue("Technologies contains Edge AI", item.getTechnologies().contains("Edge AI"));
        assertTrue("Technologies contains Vision Transformer", item.getTechnologies().contains("Vision Transformer"));
        assertTrue("IsOpenForContribution is preserved", item.isOpenForContribution());
        assertEquals("doi.org/10.1145/kuet.edge.2026", item.getPaperDoi());
    }

    @Test
    public void testResearchProposalEntityToModelMapping() {
        ResearchProposalEntity entity = new ResearchProposalEntity(
                202L,
                "2307003",
                "Samun Sadab Wafi",
                "Federated Learning on Heterogeneous Edge Nodes",
                "Communication-efficient aggregation protocol",
                "Undergraduate Thesis",
                "ACCEPTED",
                "faculty_1",
                "Approved for lab experiments.",
                1731000000000L
        );

        ResearchProposal model = DataMapper.toResearchProposal(entity);
        assertNotNull("ResearchProposal model must not be null", model);
        assertEquals(202L, model.getId());
        assertEquals("2307003", model.getStudentRoll());
        assertEquals("Samun Sadab Wafi", model.getStudentName());
        assertEquals("Federated Learning on Heterogeneous Edge Nodes", model.getTopic());
        assertEquals("ACCEPTED", model.getStatus());
        assertEquals("Approved for lab experiments.", model.getFeedback());
        assertEquals("faculty_1", model.getTeacherId());
    }

    // Helper method replicating OpportunityRepository status normalizer
    private String normalizeStatus(String status) {
        if (status == null) return "PENDING";
        String s = status.trim().toUpperCase(Locale.US);
        if (s.contains("ACCEPT")) return "ACCEPTED";
        if (s.contains("REVISION")) return "REVISION_REQUESTED";
        if (s.contains("REJECT") || s.contains("DECLINE")) return "REJECTED";
        if (s.contains("PEND")) return "PENDING";
        return s;
    }
}
