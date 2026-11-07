package com.teligati.app.network;

import com.google.gson.Gson;
import com.teligati.app.data.SessionManager;
import com.teligati.app.network.model.*;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.*;

public class MessagingIntegrationTest {

    private MockWebServer server;
    private SessionManager sessionManager;
    private MessagingApiService apiService;
    private Gson gson;
    private String baseUrl;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.setAccessToken("test-msg-token-123");
        sessionManager.setRefreshToken("test-msg-refresh-token-456");
        SessionManager.setInstanceForTesting(sessionManager);

        baseUrl = server.url("/").toString();
        gson = new Gson();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(sessionManager, baseUrl))
                .authenticator(new TokenAuthenticator(sessionManager, baseUrl))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(MessagingApiService.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void getConversations_sendsGetAndParsesPagedResponse() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"content\": [\n" +
                "      {\n" +
                "        \"id\": 10,\n" +
                "        \"otherParticipantId\": 2,\n" +
                "        \"otherParticipantName\": \"Engr. Tanvir Ahmed\",\n" +
                "        \"lastMessageText\": \"Let's discuss the ROS rover stack.\",\n" +
                "        \"unreadCount\": 1\n" +
                "      }\n" +
                "    ],\n" +
                "    \"totalElements\": 1,\n" +
                "    \"totalPages\": 1\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        Response<ApiResponse<PagedResponse<ConversationNetworkDto>>> response =
                apiService.getConversations(0, 20).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        List<ConversationNetworkDto> list = response.body().getData().getContent();
        assertEquals(1, list.size());
        assertEquals("Engr. Tanvir Ahmed", list.get(0).getOtherParticipantName());
        assertEquals(1, list.get(0).getUnreadCount());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals("/api/v1/conversations?page=0&size=20", recorded.getPath());
        assertEquals("Bearer test-msg-token-123", recorded.getHeader("Authorization"));
    }

    @Test
    public void getOrCreateConversation_sendsPostWithRecipient() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 10,\n" +
                "    \"otherParticipantId\": 3,\n" +
                "    \"otherParticipantName\": \"Prof. Dr. K. M. Azharul Hasan\",\n" +
                "    \"lastMessageText\": \"Regarding research co-authorship\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        ConversationCreateRequest request = new ConversationCreateRequest(3L, "Regarding research co-authorship");
        Response<ApiResponse<ConversationNetworkDto>> response =
                apiService.getOrCreateConversation(request).execute();

        assertTrue(response.isSuccessful());
        assertEquals(Long.valueOf(10), response.body().getData().getId());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/api/v1/conversations", recorded.getPath());
        assertTrue(recorded.getBody().readUtf8().contains("Regarding research"));
    }

    @Test
    public void getConversationMessages_sendsGetWithPagination() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"content\": [\n" +
                "      {\n" +
                "        \"id\": 101,\n" +
                "        \"conversationId\": 10,\n" +
                "        \"senderName\": \"Samun Sadab Wafi\",\n" +
                "        \"content\": \"Hello Sir\",\n" +
                "        \"isRead\": true\n" +
                "      }\n" +
                "    ],\n" +
                "    \"totalElements\": 1,\n" +
                "    \"totalPages\": 1\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        Response<ApiResponse<PagedResponse<MessageNetworkDto>>> response =
                apiService.getConversationMessages(10L, 0, 50).execute();

        assertTrue(response.isSuccessful());
        List<MessageNetworkDto> list = response.body().getData().getContent();
        assertEquals(1, list.size());
        assertEquals("Hello Sir", list.get(0).getContent());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("GET", recorded.getMethod());
        assertEquals("/api/v1/conversations/10/messages?page=0&size=50", recorded.getPath());
    }

    @Test
    public void sendMessage_sendsPostWithContent() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 102,\n" +
                "    \"conversationId\": 10,\n" +
                "    \"content\": \"I have uploaded the experiment draft.\",\n" +
                "    \"isRead\": false\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        MessageSendNetworkRequest request = new MessageSendNetworkRequest("I have uploaded the experiment draft.");
        Response<ApiResponse<MessageNetworkDto>> response =
                apiService.sendMessage(10L, request).execute();

        assertTrue(response.isSuccessful());
        assertEquals("I have uploaded the experiment draft.", response.body().getData().getContent());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/api/v1/conversations/10/messages", recorded.getPath());
        assertTrue(recorded.getBody().readUtf8().contains("uploaded the experiment"));
    }

    @Test
    public void markMessageAsRead_sendsPatch() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 102,\n" +
                "    \"isRead\": true\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        Response<ApiResponse<MessageNetworkDto>> response = apiService.markMessageAsRead(102L).execute();

        assertTrue(response.isSuccessful());
        assertTrue(response.body().getData().isRead());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("PATCH", recorded.getMethod());
        assertEquals("/api/v1/messages/102/read", recorded.getPath());
    }

    @Test
    public void markConversationAsRead_sendsPatch() throws Exception {
        String jsonResponse = "{\n" +
                "  \"success\": true\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        Response<ApiResponse<Void>> response = apiService.markConversationAsRead(10L).execute();

        assertTrue(response.isSuccessful());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("PATCH", recorded.getMethod());
        assertEquals("/api/v1/conversations/10/read", recorded.getPath());
    }
}
