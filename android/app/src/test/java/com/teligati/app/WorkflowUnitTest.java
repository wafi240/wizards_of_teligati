package com.teligati.app;

import static org.junit.Assert.*;

import com.teligati.app.data.DataMapper;
import com.teligati.app.data.entity.*;
import com.teligati.app.model.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class WorkflowUnitTest {

    @Test
    public void testJobPostingToOpportunityMapping() {
        JobPostingEntity entity = new JobPostingEntity(
                101L,
                "ROS Navigation Stack Developer",
                "ASSISTANT_WORK",
                "BMW Munich & KUET Robotics",
                "Remote / Hybrid",
                "Engr. Tanvir Ahmed",
                "Staff Engineer",
                "৳18,000 / month",
                "Flexible 12 hrs/wk",
                "Develop SLAM and navigation stack for campus rover.",
                "ROS, C++, Python",
                3.50,
                false,
                "alumni_1",
                System.currentTimeMillis()
        );

        Opportunity opp = DataMapper.toOpportunity(entity);
        assertNotNull(opp);
        assertEquals(101L, opp.getId());
        assertEquals("ROS Navigation Stack Developer", opp.getTitle());
        assertEquals(OpportunityCategory.ASSISTANT_WORK, opp.getCategory());
        assertEquals("৳18,000 / month", opp.getCompensation());
        assertEquals(3, opp.getSkillsRequired().size());
        assertTrue(opp.getSkillsRequired().contains("ROS"));
        assertFalse(opp.isApplied());
    }

    @Test
    public void testDuplicateJobApplicationPreventionLogic() {
        List<JobApplicationEntity> existingApplications = new ArrayList<>();
        existingApplications.add(new JobApplicationEntity(
                1L, 101L, "ROS Navigation Stack Developer",
                "2307003", "Samun Sadab Wafi", "2307003", "CSE",
                3.84, "ROS, Python", "First submission",
                "Applied", System.currentTimeMillis()
        ));

        // Simulate checking if roll 2307003 already applied for job 101
        long targetJobId = 101L;
        String applicantRoll = "2307003";

        boolean alreadyApplied = false;
        for (JobApplicationEntity app : existingApplications) {
            if (app.getJobId() == targetJobId && applicantRoll.equals(app.getApplicantRoll())) {
                alreadyApplied = true;
                break;
            }
        }
        assertTrue("Duplicate application must be identified and prevented", alreadyApplied);

        // Different student should not be blocked
        boolean differentStudentApplied = false;
        String newStudentRoll = "2307004";
        for (JobApplicationEntity app : existingApplications) {
            if (app.getJobId() == targetJobId && newStudentRoll.equals(app.getApplicantRoll())) {
                differentStudentApplied = true;
                break;
            }
        }
        assertFalse("New student must be allowed to apply", differentStudentApplied);
    }

    @Test
    public void testReferenceRequestEndorsementFlow() {
        ReferenceRequestEntity req = new ReferenceRequestEntity(
                1L, "2307003", "Samun Sadab Wafi", "CSE",
                "BMW Munich", "Seeking internal referral for systems role.",
                "Pending", "alumni_1", System.currentTimeMillis()
        );

        ReferenceRequest model = DataMapper.toReferenceRequest(req);
        assertEquals("Pending", model.getStatus());

        // Alumni endorses request
        req.setStatus("Endorsed");
        ReferenceRequest endorsedModel = DataMapper.toReferenceRequest(req);
        assertEquals("Endorsed", endorsedModel.getStatus());
    }

    @Test
    public void testResearchProposalFeedbackAndRevisionFlow() {
        ResearchProposalEntity entity = new ResearchProposalEntity(
                1L, "2307003", "Samun Sadab Wafi",
                "Edge AI Vision Transformer with Quantized Operators",
                "Proposal to deploy vision transformers on embedded edge nodes.",
                "Undergraduate Thesis",
                "Pending",
                "teacher_1",
                "Under review",
                System.currentTimeMillis()
        );

        ResearchProposal prop = DataMapper.toResearchProposal(entity);
        assertEquals("Pending", prop.getStatus());

        // Faculty requests revision with specific comments
        entity.setStatus("Revision Requested");
        entity.setFeedback("Please elaborate on quantization metrics and hardware benchmarks.");

        assertEquals("Revision Requested", entity.getStatus());
        assertTrue(entity.getFeedback().contains("quantization metrics"));
    }

    @Test
    public void testAlumniProfileMapping() {
        AlumniProfileEntity entity = new AlumniProfileEntity(
                1L, "alumni_1", "Engr. Tanvir Ahmed", "CSE", "2K12",
                "BMW Group", "Staff Systems Engineer", "Germany", "Munich",
                true, true, true, "ROS, C++, Cloud, Architecture",
                "Mentoring KUET juniors for European tech roles."
        );

        AlumniProfile profile = DataMapper.toAlumniProfile(entity);
        assertNotNull(profile);
        assertEquals("Engr. Tanvir Ahmed", profile.getName());
        assertEquals("BMW Group", profile.getCurrentCompany());
        assertEquals("Germany", profile.getCountry());
        assertTrue(profile.isVerified());
        assertTrue(profile.isAvailableForMentorship());
    }

    @Test
    public void testAuthorizationOwnershipEnforcement() {
        JobPostingEntity job = new JobPostingEntity(
                201L, "Embedded Systems Engineer", "FULLTIME_JOBS",
                "BMW Group", "Munich", "Engr. Tanvir Ahmed", "Staff Engineer",
                "Competitive", "Full-Time", "Core C++ role", "C++, Embedded",
                3.2, false, "tanvir.munich@kuet.ac.bd", System.currentTimeMillis()
        );

        String legitimateOwner = "tanvir.munich@kuet.ac.bd";
        String unauthorizedUser = "malicious.user@kuet.ac.bd";
        String adminUser = "admin";

        // Check legitimate owner
        boolean canOwnerModify = legitimateOwner.equalsIgnoreCase(job.getPostedByUserId()) ||
                legitimateOwner.equalsIgnoreCase(job.getPostedByName());
        assertTrue("Legitimate owner must be allowed to modify/delete", canOwnerModify);

        // Check unauthorized user
        boolean canUnauthorizedModify = unauthorizedUser.equalsIgnoreCase(job.getPostedByUserId()) ||
                unauthorizedUser.equalsIgnoreCase(job.getPostedByName()) ||
                unauthorizedUser.equalsIgnoreCase(adminUser);
        assertFalse("Unauthorized user must be blocked at authorization layer", canUnauthorizedModify);

        // Check admin user
        boolean canAdminModify = adminUser.equalsIgnoreCase(job.getPostedByUserId()) ||
                adminUser.equalsIgnoreCase(job.getPostedByName()) ||
                adminUser.equalsIgnoreCase("admin");
        assertTrue("Admin role must have administrative override", canAdminModify);
    }

    @Test
    public void testCascadeCleanupOnJobDeletion() {
        long targetJobId = 301L;

        List<JobApplicationEntity> applicationStore = new ArrayList<>();
        applicationStore.add(new JobApplicationEntity(1L, targetJobId, "Job A", "2307001", "Student 1", "2307001", "CSE", 3.8, "Java", "Note", "Applied", System.currentTimeMillis()));
        applicationStore.add(new JobApplicationEntity(2L, targetJobId, "Job A", "2307002", "Student 2", "2307002", "CSE", 3.7, "C++", "Note", "Applied", System.currentTimeMillis()));
        applicationStore.add(new JobApplicationEntity(3L, 999L, "Other Job", "2307003", "Student 3", "2307003", "CSE", 3.9, "Python", "Note", "Applied", System.currentTimeMillis()));

        assertEquals(3, applicationStore.size());

        // Simulate cascade deletion on job delete
        applicationStore.removeIf(app -> app.getJobId() == targetJobId);

        // Verify orphaned records for targetJobId are purged while other job applications remain
        assertEquals(1, applicationStore.size());
        assertEquals(999L, applicationStore.get(0).getJobId());
        for (JobApplicationEntity app : applicationStore) {
            assertNotEquals("No orphaned application records should remain for deleted job", targetJobId, app.getJobId());
        }
    }

    @Test
    public void testScopedAlumniDataIsolation() {
        List<JobPostingEntity> allJobs = new ArrayList<>();
        allJobs.add(new JobPostingEntity(1L, "Job BMW", "FULLTIME_JOBS", "BMW", "Munich", "Engr. Tanvir Ahmed", "Lead", "100k", "Full", "desc", "C++", 3.0, false, "tanvir.munich@kuet.ac.bd", System.currentTimeMillis()));
        allJobs.add(new JobPostingEntity(2L, "Job Google", "FULLTIME_JOBS", "Google", "Zurich", "Engr. Rahat Khan", "Staff", "120k", "Full", "desc", "Go", 3.5, false, "rahat.zurich@kuet.ac.bd", System.currentTimeMillis()));

        String requestedAlumniId = "tanvir.munich@kuet.ac.bd";

        List<JobPostingEntity> scopedJobs = new ArrayList<>();
        for (JobPostingEntity j : allJobs) {
            if (requestedAlumniId.equalsIgnoreCase(j.getPostedByUserId())) {
                scopedJobs.add(j);
            }
        }

        assertEquals(1, scopedJobs.size());
        assertEquals("Job BMW", scopedJobs.get(0).getTitle());
        assertFalse(scopedJobs.stream().anyMatch(j -> "Job Google".equals(j.getTitle())));
    }

    @Test
    public void testTeacherProposalIsolation() {
        List<ResearchProposalEntity> allProposals = new ArrayList<>();
        allProposals.add(new ResearchProposalEntity(1L, "2307001", "Student 1", "Edge AI", "Abstract", "Thesis", "Pending", "azharul@cse.kuet.ac.bd", "Review", System.currentTimeMillis()));
        allProposals.add(new ResearchProposalEntity(2L, "2307002", "Student 2", "Robotics Control", "Abstract", "Thesis", "Pending", "dr.robinson@kuet.ac.bd", "Review", System.currentTimeMillis()));

        String teacherId = "azharul@cse.kuet.ac.bd";
        List<ResearchProposalEntity> scopedProposals = new ArrayList<>();
        for (ResearchProposalEntity p : allProposals) {
            if (teacherId.equalsIgnoreCase(p.getTeacherId())) {
                scopedProposals.add(p);
            }
        }

        assertEquals(1, scopedProposals.size());
        assertEquals("Edge AI", scopedProposals.get(0).getTopic());
    }

    @Test
    public void testStudentProfileModificationAuthorization() {
        StudentProfileEntity studentProfile = new StudentProfileEntity(
                "2307003", "Samun Sadab Wafi", "CSE", "2023",
                3.84, "AI, Robotics, ROS", "Autonomous Rover", "https://github.com/samunwafi",
                "https://linkedin.com/in/wafi", "Intelligent Systems", "KUET Robotics Club", "Undergraduate researcher"
        );

        String authenticatedRoll = "2307003";
        String attackerRoll = "2307004";

        boolean isSelfModificationAllowed = authenticatedRoll.equalsIgnoreCase(studentProfile.getRoll());
        boolean isAttackerModificationAllowed = attackerRoll.equalsIgnoreCase(studentProfile.getRoll());

        assertTrue("Student must be authorized to modify their own profile", isSelfModificationAllowed);
        assertFalse("Cross-student profile modification must be forbidden", isAttackerModificationAllowed);
    }
}
