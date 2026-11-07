package com.teligati.app.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;

public class FirebaseIntegrationTest {

    @Test
    public void testGoogleServicesConfigurationIntegrity() throws Exception {
        File jsonFile = new File("google-services.json");
        if (!jsonFile.exists()) {
            jsonFile = new File("app/google-services.json");
        }
        if (!jsonFile.exists()) {
            jsonFile = new File("android/app/google-services.json");
        }

        assertTrue("google-services.json should exist in the app module", jsonFile.exists());

        JsonObject root = JsonParser.parseReader(new FileReader(jsonFile)).getAsJsonObject();
        assertTrue("Root must contain project_info", root.has("project_info"));
        JsonObject projectInfo = root.getAsJsonObject("project_info");
        assertTrue("project_info must have project_id", projectInfo.has("project_id"));

        assertTrue("Root must contain client array", root.has("client"));
        JsonArray clients = root.getAsJsonArray("client");
        assertTrue("Clients array must not be empty", clients.size() > 0);

        JsonObject client0 = clients.get(0).getAsJsonObject();
        JsonObject clientInfo = client0.getAsJsonObject("client_info");
        JsonObject androidClientInfo = clientInfo.getAsJsonObject("android_client_info");
        assertEquals("Package name must match Wizards of Teligati namespace",
                "com.teligati.app", androidClientInfo.get("package_name").getAsString());
    }

    @Test
    public void testFirebaseServicesContract() {
        assertNotNull("FirebaseHelper class should be loaded on classpath", FirebaseHelper.class);
        assertNotNull("FirebaseDemoActivity class should be registered on classpath", FirebaseDemoActivity.class);
    }

    @Test
    public void testFirestoreRulesIntegrity() throws Exception {
        File rulesFile = new File("firestore.rules");
        if (!rulesFile.exists()) {
            rulesFile = new File("../firestore.rules");
        }
        if (!rulesFile.exists()) {
            rulesFile = new File("android/firestore.rules");
        }
        assertTrue("firestore.rules must exist", rulesFile.exists());

        String content = new String(Files.readAllBytes(rulesFile.toPath()));
        // Ensure no insecure permissive rules exist
        assertFalse("Never allow unrestricted read/write", content.contains("allow read, write: if true"));
        assertFalse("Never allow unrestricted write", content.contains("allow write: if true"));

        // Verify key domain collections are defined
        assertTrue("Must cover users", content.contains("match /users/{uid}"));
        assertTrue("Must cover public profiles", content.contains("match /public_profiles/{uid}"));
        assertTrue("Must cover jobs", content.contains("match /jobs/{jobId}"));
        assertTrue("Must cover job applications", content.contains("match /job_applications/{appId}"));
        assertTrue("Must cover research papers", content.contains("match /research_papers/{paperId}"));
        assertTrue("Must cover research proposals", content.contains("match /research_proposals/{proposalId}"));
        assertTrue("Must cover conversations", content.contains("match /conversations/{conversationId}"));
        assertTrue("Must cover student projects", content.contains("match /student_projects/{projectId}"));
        assertTrue("Must cover demo transactions", content.contains("match /demo_transactions/{txId}"));
        assertTrue("Must cover demo escrows", content.contains("match /demo_escrows/{escrowId}"));
        assertTrue("Must cover reference requests", content.contains("match /reference_requests/{requestId}"));

        // Verify default deny
        assertTrue("Must have default deny", content.contains("match /{document=**}"));
    }

    @Test
    public void testStorageRulesIntegrity() throws Exception {
        File rulesFile = new File("storage.rules");
        if (!rulesFile.exists()) {
            rulesFile = new File("../storage.rules");
        }
        if (!rulesFile.exists()) {
            rulesFile = new File("android/storage.rules");
        }
        assertTrue("storage.rules must exist", rulesFile.exists());

        String content = new String(Files.readAllBytes(rulesFile.toPath()));
        assertFalse("Never allow unrestricted storage write", content.contains("allow write: if true"));

        // Verify storage targets
        assertTrue("Must cover profile avatars", content.contains("match /profile_avatars/{uid}.jpg"));
        assertTrue("Must cover research papers", content.contains("match /research_papers/{paperId}/{fileName}"));
        assertTrue("Must cover student resumes", content.contains("match /student_resumes/{uid}/{fileName}"));
        assertTrue("Must cover project previews", content.contains("match /project_previews/{projectId}/{fileName}"));

        // Verify default deny
        assertTrue("Must have default deny", content.contains("match /{allPaths=**}"));
    }

    @Test
    public void testHardenedSecurityRulesInvariants() throws Exception {
        File rulesFile = new File("firestore.rules");
        if (!rulesFile.exists()) {
            rulesFile = new File("../firestore.rules");
        }
        if (!rulesFile.exists()) {
            rulesFile = new File("android/firestore.rules");
        }
        assertTrue("firestore.rules must exist", rulesFile.exists());

        String content = new String(Files.readAllBytes(rulesFile.toPath()));

        // 1. users/{uid} is strictly private (only owner can read)
        assertTrue("users/{uid} read must be owner-only", content.contains("match /users/{uid} {\n      // Strictly Private: Only the user can read their own account data (email, roll, phone, etc.)\n      allow read: if isOwner(uid);") || content.contains("allow read: if isOwner(uid);"));

        // 2. Notifications: senderUid and recipientUid validation
        assertTrue("Notifications must verify recipientUid", content.contains("request.resource.data.recipientUid == uid"));
        assertTrue("Notifications must verify senderUid", content.contains("request.resource.data.senderUid == request.auth.uid"));
        assertTrue("Notifications must initialize read as false", content.contains("request.resource.data.read == false"));

        // 3. Research Proposals: Assigned faculty only can change status/feedback; student can only revise when pending/revision requested
        assertTrue("Faculty decision must check facultyUid", content.contains("resource.data.facultyUid == request.auth.uid"));
        assertTrue("Student revisions must check status", content.contains("resource.data.status in ['PENDING', 'REVISION_REQUESTED']"));
        assertTrue("Student must not be allowed to change faculty feedback", content.contains("(!('feedback' in resource.data) || request.resource.data.feedback == resource.data.feedback)"));

        // 4. Job Applications: poster decision vs applicant withdrawal
        assertTrue("Poster decision must check posterUid", content.contains("resource.data.posterUid == request.auth.uid"));
        assertTrue("Applicant update must be restricted", content.contains("resource.data.applicantUid == request.auth.uid"));
        assertTrue("Applicant can withdraw", content.contains("request.resource.data.status in [resource.data.status, 'WITHDRAWN']"));

        // 5. Demo Escrows: strictly simulation; immutable amount and participant UIDs
        assertTrue("Escrow must keep amount immutable", content.contains("request.resource.data.amount == resource.data.amount"));
        assertTrue("Escrow must keep employer immutable", content.contains("request.resource.data.employerUid == resource.data.employerUid"));
        assertTrue("Escrow must keep freelancer immutable", content.contains("request.resource.data.freelancerUid == resource.data.freelancerUid"));
        assertTrue("Employer release/refund check", content.contains("request.resource.data.status in ['RELEASED', 'REFUNDED']"));

        // 6. Reference Requests: targeted alumni decision vs student cancel
        assertTrue("Alumni decision check", content.contains("resource.data.alumniUid == request.auth.uid"));
        assertTrue("Student cancellation check", content.contains("request.resource.data.status == 'CANCELLED'"));

        // 7. Conversations & Messages: immutable participantUids and immutable messages
        assertTrue("Conversations must keep participantUids immutable", content.contains("request.resource.data.participantUids == resource.data.participantUids"));
        assertTrue("Messages must be strictly immutable", content.contains("// Messages are strictly permanent and immutable\n        allow update, delete: if false;"));

        // 8. Public Profiles: validation helper
        assertTrue("Public profiles must use validation function", content.contains("isValidPublicProfile(uid)"));

        // 9. Hardened Notification verification against verified workflows
        assertTrue("Proposal notifications require real proposal check", content.contains("exists(/databases/$(database)/documents/research_proposals/$(request.resource.data.referenceId))"));
        assertTrue("Application notifications require real application check", content.contains("exists(/databases/$(database)/documents/job_applications/$(request.resource.data.referenceId))"));
        assertTrue("Reference notifications require real reference request check", content.contains("exists(/databases/$(database)/documents/reference_requests/$(request.resource.data.referenceId))"));
        assertTrue("Message notifications require real conversation check", content.contains("exists(/databases/$(database)/documents/conversations/$(request.resource.data.referenceId))"));

        // 10. Conversation recipient verification
        assertTrue("Conversation creation requires recipient public profile check", content.contains("exists(/databases/$(database)/documents/public_profiles/$("));
        assertTrue("Conversation participants must be distinct", content.contains("request.resource.data.participantUids[0] != request.resource.data.participantUids[1]"));

        // 11. Demo wallet bounded constraints
        assertTrue("Demo wallet balance must not be negative", content.contains("request.resource.data.availableBalance >= 0"));
        assertTrue("Demo wallet has demo ceiling", content.contains("request.resource.data.availableBalance <= 100000"));
    }
}
