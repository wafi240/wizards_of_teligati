package com.teligati.app.network;

import com.google.gson.Gson;
import com.teligati.app.data.SessionManager;
import com.teligati.app.network.model.*;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.junit.Assert.*;

public class WalletEscrowIntegrationTest {

    private MockWebServer server;
    private SessionManager sessionManager;
    private WalletApiService apiService;
    private Gson gson;
    private String baseUrl;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        InMemorySharedPreferences prefs = new InMemorySharedPreferences();
        sessionManager = new SessionManager(prefs);
        sessionManager.setSession("STUDENT", "2307003");
        sessionManager.setAccessToken("test-demo-access-token-123");
        sessionManager.setRefreshToken("test-demo-refresh-token-456");
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

        apiService = retrofit.create(WalletApiService.class);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void testGetWallet_parsesBalancesAndDemoDisclaimers() throws Exception {
        String json = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Demo wallet balance retrieved\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 1,\n" +
                "    \"userId\": 10,\n" +
                "    \"availableBalance\": 10000.00,\n" +
                "    \"escrowBalance\": 0.00,\n" +
                "    \"currency\": \"DEMO_CREDITS\",\n" +
                "    \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\",\n" +
                "    \"demoNotice\": \"DEMO MODE — All balances and transactions are simulated. No real money is transferred.\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));

        Response<ApiResponse<WalletNetworkDto>> response = apiService.getWallet().execute();
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().isSuccess());

        WalletNetworkDto wallet = response.body().getData();
        assertNotNull(wallet);
        assertEquals(10000.00, wallet.getAvailableBalance(), 0.01);
        assertEquals(0.00, wallet.getEscrowBalance(), 0.01);
        assertEquals("DEMO_CREDITS", wallet.getCurrency());
        assertEquals("DEMO WALLET — NO REAL MONEY INVOLVED", wallet.getDisclaimer());
        assertTrue(wallet.getDemoNotice().contains("DEMO MODE"));

        RecordedRequest req = server.takeRequest();
        assertEquals("/api/v1/wallet", req.getPath());
        assertEquals("Bearer test-demo-access-token-123", req.getHeader("Authorization"));
    }

    @Test
    public void testTopUp_sendsPayloadAndParsesResponse() throws Exception {
        String json = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Demo credits successfully added to wallet\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 1,\n" +
                "    \"userId\": 10,\n" +
                "    \"availableBalance\": 15000.00,\n" +
                "    \"escrowBalance\": 0.00,\n" +
                "    \"currency\": \"DEMO_CREDITS\",\n" +
                "    \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));

        TopUpRequest request = new TopUpRequest(5000.0, "SIMULATED_BKASH", "Demo test deposit");
        Response<ApiResponse<WalletNetworkDto>> response = apiService.topUp(request).execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(15000.0, response.body().getData().getAvailableBalance(), 0.01);

        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("/api/v1/wallet/topup", req.getPath());
        assertTrue(req.getBody().readUtf8().contains("SIMULATED_BKASH"));
    }

    @Test
    public void testWithdraw_sendsPayloadAndParsesResponse() throws Exception {
        String json = "{\n" +
                "  \"success\": true,\n" +
                "  \"message\": \"Simulated demo withdrawal processed\",\n" +
                "  \"data\": {\n" +
                "    \"id\": 1,\n" +
                "    \"userId\": 10,\n" +
                "    \"availableBalance\": 8000.00,\n" +
                "    \"escrowBalance\": 0.00,\n" +
                "    \"currency\": \"DEMO_CREDITS\",\n" +
                "    \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\"\n" +
                "  }\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));

        WithdrawRequest request = new WithdrawRequest(2000.0, "SIMULATED_BANK_ACCT_999", "Stipend payout");
        Response<ApiResponse<WalletNetworkDto>> response = apiService.withdraw(request).execute();

        assertTrue(response.isSuccessful());
        assertEquals(8000.0, response.body().getData().getAvailableBalance(), 0.01);

        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("/api/v1/wallet/withdraw", req.getPath());
        assertTrue(req.getBody().readUtf8().contains("SIMULATED_BANK_ACCT_999"));
    }

    @Test
    public void testGetTransactions_parsesList() throws Exception {
        String json = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "      \"id\": 101,\n" +
                "      \"transactionId\": \"TX-TOPUP-99AABB\",\n" +
                "      \"title\": \"Simulated Demo Credits Top-up\",\n" +
                "      \"category\": \"DEPOSIT\",\n" +
                "      \"amount\": 5000.00,\n" +
                "      \"type\": \"TOPUP\",\n" +
                "      \"status\": \"COMPLETED\",\n" +
                "      \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json));

        Response<ApiResponse<List<TransactionNetworkDto>>> response = apiService.getTransactions().execute();
        assertTrue(response.isSuccessful());
        List<TransactionNetworkDto> list = response.body().getData();
        assertEquals(1, list.size());
        assertEquals("TX-TOPUP-99AABB", list.get(0).getTransactionId());
        assertEquals(5000.0, list.get(0).getAmount(), 0.01);
    }

    @Test
    public void testEscrowLifecycle_createReleaseRefundDispute() throws Exception {
        // 1. Create Escrow
        String createJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 42,\n" +
                "    \"escrowReference\": \"ESC-TEST-7788\",\n" +
                "    \"title\": \"Backend Automation Milestone\",\n" +
                "    \"amount\": 3000.00,\n" +
                "    \"status\": \"FUNDED\",\n" +
                "    \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\"\n" +
                "  }\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json").setBody(createJson));

        EscrowCreateRequest createReq = new EscrowCreateRequest("Backend Automation Milestone", "Desc", 3000.0, 5L, null);
        Response<ApiResponse<EscrowNetworkDto>> createRes = apiService.createEscrow(createReq).execute();
        assertTrue(createRes.isSuccessful());
        assertEquals("FUNDED", createRes.body().getData().getStatus());

        // 2. Release Escrow
        String releaseJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 42,\n" +
                "    \"status\": \"RELEASED\",\n" +
                "    \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\"\n" +
                "  }\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(releaseJson));

        Response<ApiResponse<EscrowNetworkDto>> releaseRes = apiService.releaseEscrow(42).execute();
        assertTrue(releaseRes.isSuccessful());
        assertEquals("RELEASED", releaseRes.body().getData().getStatus());

        // 3. Refund Escrow
        String refundJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 43,\n" +
                "    \"status\": \"REFUNDED\",\n" +
                "    \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\"\n" +
                "  }\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(refundJson));

        Response<ApiResponse<EscrowNetworkDto>> refundRes = apiService.refundEscrow(43).execute();
        assertTrue(refundRes.isSuccessful());
        assertEquals("REFUNDED", refundRes.body().getData().getStatus());

        // 4. Dispute Escrow
        String disputeJson = "{\n" +
                "  \"success\": true,\n" +
                "  \"data\": {\n" +
                "    \"id\": 44,\n" +
                "    \"status\": \"DISPUTED\",\n" +
                "    \"disclaimer\": \"DEMO WALLET — NO REAL MONEY INVOLVED\"\n" +
                "  }\n" +
                "}";
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(disputeJson));

        Map<String, String> body = new HashMap<>();
        body.put("reason", "Milestone incomplete");
        Response<ApiResponse<EscrowNetworkDto>> disputeRes = apiService.disputeEscrow(44, body).execute();
        assertTrue(disputeRes.isSuccessful());
        assertEquals("DISPUTED", disputeRes.body().getData().getStatus());
    }
}
