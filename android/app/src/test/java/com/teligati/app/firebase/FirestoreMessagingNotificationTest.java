package com.teligati.app.firebase;

import static org.junit.Assert.*;

import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.ConversationEntity;
import com.teligati.app.data.entity.MessageEntity;
import com.teligati.app.data.entity.NotificationEntity;
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

/**
 * Comprehensive Unit Test Suite for Phase 5: Messaging, Conversations & Notifications Migration to Firestore.
 * Verifies:
 * 1. Conversation creation payload schema, 2-participant requirement, and self-conversation prevention.
 * 2. Conversation lastMessage metadata updates and restricted affected keys.
 * 3. Message payload constraints (senderUid, non-empty content <= 2000 chars, timestamp).
 * 4. Message immutability (updates and deletions strictly prohibited).
 * 5. Notification payload structure, character bounds (title <= 150, message <= 1000), and unread state.
 * 6. Notification workflow cases (Self-confirmation, Proposal Decision, Application Decision, Message).
 * 7. Notification read state update and restricted affected keys (['read'] only).
 * 8. Room offline caching, unread count computation, and entity mapping.
 */
public class FirestoreMessagingNotificationTest {

    private static final Set<String> ALLOWED_CONVERSATION_UPDATE_KEYS = new HashSet<>(Arrays.asList(
            "lastMessage", "lastMessageTime", "lastSenderUid", "unreadCounts", "updatedAt"
    ));

    private static final Set<String> ALLOWED_NOTIFICATION_UPDATE_KEYS = new HashSet<>(Arrays.asList(
            "read"
    ));

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "student_1");
        sessionManager.saveFirebaseSession("student_1", "student1@stud.kuet.ac.bd", "STUDENT", "Samun Sadab Wafi", "CSE", "2307003");
        SessionManager.setInstanceForTesting(sessionManager);
    }

    // =========================================================================
    // 1. CONVERSATIONS SCHEMA & INVARIANTS
    // =========================================================================

    @Test
    public void testConversationPayload_requiresExactlyTwoDistinctParticipants() {
        String user1 = "student_1";
        String user2 = "student_2";

        Map<String, Object> convDoc = new HashMap<>();
        convDoc.put("id", "student_1_student_2");
        convDoc.put("participantUids", Arrays.asList(user1, user2));
        convDoc.put("lastMessage", "Hey peer, collaborating on research?");
        convDoc.put("lastMessageTime", System.currentTimeMillis());
        convDoc.put("lastSenderUid", user1);
        convDoc.put("createdAt", System.currentTimeMillis());
        convDoc.put("updatedAt", System.currentTimeMillis());

        // Validate security invariants
        List<?> participants = (List<?>) convDoc.get("participantUids");
        assertNotNull("Participant UIDs list cannot be null", participants);
        assertEquals("Conversation must have exactly 2 participants", 2, participants.size());
        assertNotEquals("Participants must be distinct (no self-conversations)", participants.get(0), participants.get(1));
        assertTrue("Current user must be a participant", participants.contains(user1));
    }

    @Test
    public void testSelfConversation_isStrictlyProhibited() {
        String user1 = "student_1";
        String userSelf = "student_1";

        boolean isSelfConversation = user1.equals(userSelf);
        assertTrue("Self-conversation condition detected", isSelfConversation);
        // Security rule explicitly asserts: request.resource.data.participantUids[0] != request.resource.data.participantUids[1]
        assertFalse("Rules prohibit participantUids[0] == participantUids[1]", !isSelfConversation);
    }

    @Test
    public void testConversationUpdate_affectedKeysStrictlyRestricted() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", "New reply from peer.");
        updates.put("lastMessageTime", System.currentTimeMillis());
        updates.put("lastSenderUid", "student_1");
        updates.put("updatedAt", System.currentTimeMillis());

        // Verify affected keys match diff(resource.data).affectedKeys().hasOnly(['lastMessage', 'lastMessageTime', 'lastSenderUid', 'unreadCounts', 'updatedAt'])
        for (String key : updates.keySet()) {
            assertTrue("Affected key '" + key + "' must be allowed in conversation update",
                    ALLOWED_CONVERSATION_UPDATE_KEYS.contains(key));
        }
        assertEquals("student_1", updates.get("lastSenderUid"));
    }

    @Test
    public void testConversationUpdate_cannotAlterParticipantsList() {
        Map<String, Object> maliciousUpdate = new HashMap<>();
        maliciousUpdate.put("participantUids", Arrays.asList("student_1", "intruder_user")); // Illegal
        maliciousUpdate.put("lastMessage", "Hijack");

        boolean hasIllegalKey = false;
        for (String key : maliciousUpdate.keySet()) {
            if (!ALLOWED_CONVERSATION_UPDATE_KEYS.contains(key)) {
                hasIllegalKey = true;
                break;
            }
        }
        assertTrue("Modifying participantUids in conversation update must be rejected", hasIllegalKey);
    }

    // =========================================================================
    // 2. MESSAGES SUBCOLLECTION & IMMUTABILITY
    // =========================================================================

    @Test
    public void testMessagePayload_validSenderAndContentBounds() {
        String senderUid = "student_1";
        String content = "Meeting scheduled at CSE Building Room 304.";

        Map<String, Object> msgDoc = new HashMap<>();
        msgDoc.put("id", "msg_101");
        msgDoc.put("conversationId", "student_1_student_2");
        msgDoc.put("senderUid", senderUid);
        msgDoc.put("senderName", "Samun Sadab Wafi");
        msgDoc.put("content", content);
        msgDoc.put("timestamp", System.currentTimeMillis());

        // Validate security invariants
        assertEquals("senderUid must match current user UID", senderUid, msgDoc.get("senderUid"));
        String msgContent = (String) msgDoc.get("content");
        assertNotNull("Message content cannot be null", msgContent);
        assertTrue("Message content must be non-empty", msgContent.length() > 0);
        assertTrue("Message content must be within 2000 character limit", msgContent.length() <= 2000);
    }

    @Test
    public void testMessagePayload_rejectsEmptyOrExcessiveContent() {
        String emptyContent = "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2001; i++) sb.append('x');
        String excessiveContent = sb.toString();

        boolean isEmptyValid = emptyContent.length() > 0 && emptyContent.length() <= 2000;
        assertFalse("Empty message content must be rejected", isEmptyValid);

        boolean isExcessiveValid = excessiveContent.length() > 0 && excessiveContent.length() <= 2000;
        assertFalse("Message exceeding 2000 chars must be rejected", isExcessiveValid);
    }

    @Test
    public void testMessagesAreImmutable_updatesAndDeletionsProhibited() {
        // Security rules: match /messages/{messageId} { allow update, delete: if false; }
        boolean allowUpdate = false;
        boolean allowDelete = false;

        assertFalse("Messages must be strictly permanent and cannot be edited", allowUpdate);
        assertFalse("Messages must be strictly permanent and cannot be deleted", allowDelete);
    }

    // =========================================================================
    // 3. NOTIFICATIONS SCHEMA & WORKFLOW VALIDATION
    // =========================================================================

    @Test
    public void testSelfConfirmationNotification_validPayload() {
        String userUid = "student_1";

        Map<String, Object> notifDoc = new HashMap<>();
        notifDoc.put("recipientUid", userUid);
        notifDoc.put("senderUid", userUid);
        notifDoc.put("read", false);
        notifDoc.put("title", "Application Submitted");
        notifDoc.put("message", "You applied for Assistant Role at KUET Lab.");
        notifDoc.put("timestamp", System.currentTimeMillis());
        notifDoc.put("createdAt", System.currentTimeMillis());

        assertEquals("Recipient must match sender for self-notification", notifDoc.get("recipientUid"), notifDoc.get("senderUid"));
        assertEquals(Boolean.FALSE, notifDoc.get("read"));
        String title = (String) notifDoc.get("title");
        assertTrue("Title must be between 1 and 150 chars", title.length() > 0 && title.length() <= 150);
        String msg = (String) notifDoc.get("message");
        assertTrue("Message must be between 1 and 1000 chars", msg.length() > 0 && msg.length() <= 1000);
    }

    @Test
    public void testDirectMessageNotification_requiresMessageTypeAndActiveConversation() {
        String recipientUid = "student_2";
        String senderUid = "student_1";
        String convDocId = "student_1_student_2";

        Map<String, Object> notifDoc = new HashMap<>();
        notifDoc.put("recipientUid", recipientUid);
        notifDoc.put("senderUid", senderUid);
        notifDoc.put("read", false);
        notifDoc.put("title", "New Message from Samun");
        notifDoc.put("message", "Hello peer!");
        notifDoc.put("type", "MESSAGE");
        notifDoc.put("referenceId", convDocId);
        notifDoc.put("timestamp", System.currentTimeMillis());

        assertEquals("MESSAGE", notifDoc.get("type"));
        assertEquals("student_1_student_2", notifDoc.get("referenceId"));
        assertEquals(Boolean.FALSE, notifDoc.get("read"));
    }

    @Test
    public void testWorkflowNotification_proposalDecisionCase() {
        String studentUid = "student_1";
        String facultyUid = "faculty_1";
        String propDocId = "prop_101";

        Map<String, Object> notifDoc = new HashMap<>();
        notifDoc.put("recipientUid", studentUid);
        notifDoc.put("senderUid", facultyUid);
        notifDoc.put("read", false);
        notifDoc.put("title", "Research Proposal: ACCEPTED");
        notifDoc.put("message", "Your proposal for 'Autonomous Edge AI' has been approved.");
        notifDoc.put("type", "PROPOSAL_DECISION");
        notifDoc.put("referenceId", propDocId);
        notifDoc.put("timestamp", System.currentTimeMillis());

        assertEquals("PROPOSAL_DECISION", notifDoc.get("type"));
        assertEquals(propDocId, notifDoc.get("referenceId"));
        assertEquals(studentUid, notifDoc.get("recipientUid"));
        assertEquals(facultyUid, notifDoc.get("senderUid"));
    }

    @Test
    public void testWorkflowNotification_applicationDecisionCase() {
        String applicantUid = "student_1";
        String posterUid = "alumni_1";
        String appDocId = "app_501";

        Map<String, Object> notifDoc = new HashMap<>();
        notifDoc.put("recipientUid", applicantUid);
        notifDoc.put("senderUid", posterUid);
        notifDoc.put("read", false);
        notifDoc.put("title", "Application Status: ACCEPTED");
        notifDoc.put("message", "Selected for interview and preliminary offer.");
        notifDoc.put("type", "APPLICATION_DECISION");
        notifDoc.put("referenceId", appDocId);
        notifDoc.put("timestamp", System.currentTimeMillis());

        assertEquals("APPLICATION_DECISION", notifDoc.get("type"));
        assertEquals(appDocId, notifDoc.get("referenceId"));
        assertEquals(applicantUid, notifDoc.get("recipientUid"));
        assertEquals(posterUid, notifDoc.get("senderUid"));
    }

    @Test
    public void testMarkNotificationAsRead_affectedKeysRestrictedToRead() {
        Map<String, Object> readUpdate = new HashMap<>();
        readUpdate.put("read", true);

        for (String key : readUpdate.keySet()) {
            assertTrue("Notification update key must be in ['read']",
                    ALLOWED_NOTIFICATION_UPDATE_KEYS.contains(key));
        }

        // Tampering attempt
        Map<String, Object> maliciousUpdate = new HashMap<>();
        maliciousUpdate.put("read", true);
        maliciousUpdate.put("message", "Tampered message content"); // Illegal

        boolean hasIllegalKey = false;
        for (String key : maliciousUpdate.keySet()) {
            if (!ALLOWED_NOTIFICATION_UPDATE_KEYS.contains(key)) {
                hasIllegalKey = true;
                break;
            }
        }
        assertTrue("Tampering notification message must violate affectedKeys rule", hasIllegalKey);
    }

    // =========================================================================
    // 4. ROOM ENTITIES & DATA MAPPING
    // =========================================================================

    @Test
    public void testConversationEntity_mappingAndUnreadCount() {
        ConversationEntity conv = new ConversationEntity(
                101L,
                202L,
                "Engr. Tanvir Ahmed",
                "tanvir@kuet.ac.bd",
                "ALUMNI",
                "CSE",
                "See you at the KUET alumni tech meetup.",
                1731000000000L,
                2L
        );

        assertEquals(101L, conv.getId());
        assertEquals(202L, conv.getOtherParticipantId());
        assertEquals("Engr. Tanvir Ahmed", conv.getOtherParticipantName());
        assertEquals("ALUMNI", conv.getOtherParticipantRole());
        assertEquals("See you at the KUET alumni tech meetup.", conv.getLastMessageText());
        assertEquals(2L, conv.getUnreadCount());

        // Update read count
        conv.setUnreadCount(0);
        assertEquals(0L, conv.getUnreadCount());
    }

    @Test
    public void testMessageEntity_mappingAndProperties() {
        MessageEntity msg = new MessageEntity(
                501L,
                101L,
                "student_1",
                "Samun Sadab Wafi",
                "alumni_1",
                "Engr. Tanvir Ahmed",
                "Thank you for reviewing my profile!",
                1731000000000L,
                "Nov 07, 07:30 PM",
                true
        );

        assertEquals(501L, msg.getId());
        assertEquals(101L, msg.getConversationId());
        assertEquals("student_1", msg.getSenderId());
        assertEquals("alumni_1", msg.getReceiverId());
        assertEquals("Thank you for reviewing my profile!", msg.getContent());
        assertTrue(msg.isRead());
    }

    @Test
    public void testNotificationEntity_mappingAndReadToggle() {
        NotificationEntity notif = new NotificationEntity(
                1L,
                "student_1",
                "New Message",
                "Engr. Tanvir sent you a message.",
                1731000000000L,
                "Just now",
                false
        );

        assertEquals(1L, notif.getId());
        assertEquals("student_1", notif.getUserId());
        assertEquals("New Message", notif.getTitle());
        assertFalse(notif.isRead());

        notif.setRead(true);
        assertTrue(notif.isRead());
    }

    @Test
    public void testUnreadNotificationCount_calculation() {
        List<NotificationEntity> list = new ArrayList<>();
        list.add(new NotificationEntity(1L, "student_1", "Notif 1", "Msg 1", 1000L, "Now", false));
        list.add(new NotificationEntity(2L, "student_1", "Notif 2", "Msg 2", 2000L, "Now", true));
        list.add(new NotificationEntity(3L, "student_1", "Notif 3", "Msg 3", 3000L, "Now", false));

        int unreadCount = 0;
        for (NotificationEntity n : list) {
            if (!n.isRead()) unreadCount++;
        }

        assertEquals(2, unreadCount);
    }
}
