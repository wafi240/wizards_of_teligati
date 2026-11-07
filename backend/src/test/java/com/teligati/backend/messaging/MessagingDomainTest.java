package com.teligati.backend.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teligati.backend.dto.*;
import com.teligati.backend.entity.User;
import com.teligati.backend.repository.NotificationRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class MessagingDomainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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
    @DisplayName("Conversation: Student starts conversation with Alumni successfully")
    void studentStartConversationWithAlumni_success() throws Exception {
        ConversationCreateRequest req = ConversationCreateRequest.builder()
                .recipientId(alumniUserId)
                .initialMessage("Hello Engr. Tanvir vai, I wanted to inquire about systems engineering opportunities at BMW.")
                .build();

        mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otherParticipantId").value(alumniUserId))
                .andExpect(jsonPath("$.data.otherParticipantName", containsString("Tanvir")))
                .andExpect(jsonPath("$.data.lastMessageText", containsString("BMW")));
    }

    @Test
    @DisplayName("Conversation: Student starts conversation with Teacher successfully")
    void studentStartConversationWithTeacher_success() throws Exception {
        ConversationCreateRequest req = ConversationCreateRequest.builder()
                .recipientId(teacherUserId)
                .initialMessage("Respected Sir, regarding the lab opening for Computer Vision.")
                .build();

        mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otherParticipantId").value(teacherUserId))
                .andExpect(jsonPath("$.data.otherParticipantRole", anyOf(is("ROLE_TEACHER"), is("TEACHER"))));
    }

    @Test
    @DisplayName("Validation: User cannot create a conversation with self")
    void cannotCreateConversationWithSelf_fails() throws Exception {
        ConversationCreateRequest req = ConversationCreateRequest.builder()
                .recipientId(studentUserId)
                .initialMessage("Self talking")
                .build();

        mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("yourself")));
    }

    @Test
    @DisplayName("Idempotency: Single conversation thread between two users (prevents duplicates)")
    void preventDuplicateConversations_returnsSameConversation() throws Exception {
        ConversationCreateRequest req1 = ConversationCreateRequest.builder()
                .recipientId(alumniUserId)
                .build();

        MvcResult res1 = mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        long convId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Alumni initiates with student
        ConversationCreateRequest req2 = ConversationCreateRequest.builder()
                .recipientId(studentUserId)
                .build();

        MvcResult res2 = mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        long convId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).path("data").path("id").asLong();

        assertEquals(convId1, convId2, "Conversation between student and alumni must be single canonical thread");
    }

    @Test
    @DisplayName("Messaging: Send and view text messages in conversation")
    void sendAndListMessages_success() throws Exception {
        // 1. Get conversation
        ConversationCreateRequest convReq = ConversationCreateRequest.builder()
                .recipientId(alumniUserId)
                .build();

        MvcResult convRes = mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long convId = objectMapper.readTree(convRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Student sends message
        MessageSendRequest msgReq = MessageSendRequest.builder()
                .content("Do you think ROS 2 is preferred over ROS 1 for automotive middleware?")
                .build();

        MvcResult msgRes = mockMvc.perform(
                post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content", containsString("ROS 2")))
                .andExpect(jsonPath("$.data.isRead").value(false))
                .andReturn();

        long msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 3. Alumni views conversation messages
        mockMvc.perform(
                get("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + msgId + ")]").exists());

        // 4. Alumni replies
        MessageSendRequest replyReq = MessageSendRequest.builder()
                .content("Yes, absolutely. All modern autonomy stacks at BMW use ROS 2 / DDS.")
                .build();

        mockMvc.perform(
                post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content", containsString("DDS")));
    }

    @Test
    @DisplayName("Security: Non-participant cannot view or send messages")
    void nonParticipantCannotAccessConversation_fails() throws Exception {
        // Create conversation between Student and Alumni
        ConversationCreateRequest convReq = ConversationCreateRequest.builder()
                .recipientId(alumniUserId)
                .build();

        MvcResult convRes = mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long convId = objectMapper.readTree(convRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Teacher (not participant) tries to view messages
        mockMvc.perform(
                get("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        // Teacher tries to post message into this conversation
        MessageSendRequest hackMsg = MessageSendRequest.builder()
                .content("Eavesdropping message")
                .build();

        mockMvc.perform(
                post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackMsg)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Read Status: Recipient marks message as read; non-recipient is forbidden")
    void readStatusManagement_verified() throws Exception {
        // Student sends message to Alumni
        ConversationCreateRequest convReq = ConversationCreateRequest.builder()
                .recipientId(alumniUserId)
                .build();

        MvcResult convRes = mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long convId = objectMapper.readTree(convRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        MessageSendRequest msgReq = MessageSendRequest.builder()
                .content("Question for read receipt verification.")
                .build();

        MvcResult msgRes = mockMvc.perform(
                post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Student (sender) cannot mark as read
        mockMvc.perform(
                patch("/api/v1/messages/" + msgId + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Alumni (recipient) marks as read
        mockMvc.perform(
                patch("/api/v1/messages/" + msgId + "/read")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        // Alumni marks entire conversation as read
        mockMvc.perform(
                patch("/api/v1/conversations/" + convId + "/read")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Validation: Blank message content is rejected")
    void emptyMessageContent_fails() throws Exception {
        ConversationCreateRequest convReq = ConversationCreateRequest.builder()
                .recipientId(alumniUserId)
                .build();

        MvcResult convRes = mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long convId = objectMapper.readTree(convRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        MessageSendRequest emptyMsg = MessageSendRequest.builder()
                .content("    ")
                .build();

        mockMvc.perform(
                post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyMsg)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Security: Invalid conversation IDs return 404 Not Found")
    void invalidConversationId_returnsNotFound() throws Exception {
        mockMvc.perform(
                get("/api/v1/conversations/99999999/messages")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());

        MessageSendRequest msg = MessageSendRequest.builder()
                .content("Test invalid conversation")
                .build();

        mockMvc.perform(
                post("/api/v1/conversations/99999999/messages")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msg)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Security: Sender identity and timestamps are server-controlled")
    void messageOwnershipAndTimestamps_serverControlled() throws Exception {
        ConversationCreateRequest convReq = ConversationCreateRequest.builder()
                .recipientId(teacherUserId)
                .build();

        MvcResult convRes = mockMvc.perform(
                post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long convId = objectMapper.readTree(convRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        MessageSendRequest msg = MessageSendRequest.builder()
                .content("Verifying server-controlled identity.")
                .build();

        MvcResult msgRes = mockMvc.perform(
                post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.senderId").value(studentUserId))
                .andExpect(jsonPath("$.data.recipientId").value(teacherUserId))
                .andExpect(jsonPath("$.data.isRead").value(false))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andReturn();

        String createdAtStr = objectMapper.readTree(msgRes.getResponse().getContentAsString()).path("data").path("createdAt").asText();
        assertNotNull(createdAtStr);
        assertFalse(createdAtStr.isEmpty());
    }
}
