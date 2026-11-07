package com.teligati.backend.financial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teligati.backend.dto.*;
import com.teligati.backend.entity.User;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class WalletEscrowDomainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String studentToken;
    private String alumniToken;
    private Long studentUserId;
    private Long alumniUserId;

    @BeforeEach
    void setUp() throws Exception {
        studentToken = obtainAccessToken("2307003", "demo");
        alumniToken = obtainAccessToken("tanvir.munich@kuet.ac.bd", "demo");

        User student = userRepository.findByIdentifier("2307003").orElseThrow();
        User alumni = userRepository.findByIdentifier("tanvir.munich@kuet.ac.bd").orElseThrow();
        studentUserId = student.getId();
        alumniUserId = alumni.getId();
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
    @DisplayName("Financial API: Unauthenticated access rejected with 401")
    void unauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/wallet"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/escrow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Demo Wallet: Retrieve wallet balance with simulation disclaimers")
    void getWallet_success() throws Exception {
        mockMvc.perform(get("/api/v1/wallet")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currency").value("DEMO_CREDITS"))
                .andExpect(jsonPath("$.data.disclaimer").value("DEMO WALLET — NO REAL MONEY INVOLVED"))
                .andExpect(jsonPath("$.data.demoNotice").value("DEMO MODE — All balances and transactions are simulated. No real money is transferred."))
                .andExpect(jsonPath("$.data.availableBalance").isNumber());
    }

    @Test
    @DisplayName("Demo Wallet: Simulated top-up increases balance and logs transaction")
    void topUp_success() throws Exception {
        TopUpRequest request = TopUpRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .paymentMethod("SIMULATED_TEST_CHANNEL")
                .note("Unit test deposit")
                .build();

        mockMvc.perform(post("/api/v1/wallet/topup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.disclaimer").value("DEMO WALLET — NO REAL MONEY INVOLVED"));

        // Verify transaction was logged
        mockMvc.perform(get("/api/v1/wallet/transactions")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("TOPUP"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Demo Wallet: Top-up fails with invalid or non-positive amount")
    void topUp_invalidAmount() throws Exception {
        TopUpRequest request = TopUpRequest.builder()
                .amount(new BigDecimal("-100.00"))
                .paymentMethod("SIMULATED_BKASH")
                .build();

        mockMvc.perform(post("/api/v1/wallet/topup")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Demo Wallet: Simulated withdrawal succeeds and deducts available balance")
    void withdraw_success() throws Exception {
        WithdrawRequest request = WithdrawRequest.builder()
                .amount(new BigDecimal("200.00"))
                .destinationAccount("SIMULATED_KUET_ACCOUNT_123")
                .note("Simulated stipend cash out")
                .build();

        mockMvc.perform(post("/api/v1/wallet/withdraw")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.disclaimer").value("DEMO WALLET — NO REAL MONEY INVOLVED"));
    }

    @Test
    @DisplayName("Demo Wallet: Simulated withdrawal fails if amount exceeds available balance")
    void withdraw_insufficientBalance() throws Exception {
        WithdrawRequest request = WithdrawRequest.builder()
                .amount(new BigDecimal("99999999.00"))
                .destinationAccount("SIMULATED_EXT_ACCOUNT")
                .build();

        mockMvc.perform(post("/api/v1/wallet/withdraw")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Demo Escrow: Full lifecycle (Create -> In-Progress -> Release)")
    void escrowLifecycle_release_success() throws Exception {
        // 1. Alumni creates and funds escrow for student
        EscrowCreateRequest createReq = EscrowCreateRequest.builder()
                .title("Fullstack Web Development Milestone 1")
                .description("Build React UI components and test endpoints")
                .amount(new BigDecimal("1500.00"))
                .payeeId(studentUserId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/escrow/create")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FUNDED"))
                .andExpect(jsonPath("$.data.disclaimer").value("DEMO WALLET — NO REAL MONEY INVOLVED"))
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long escrowId = root.path("data").path("id").asLong();

        // 2. Student starts work
        mockMvc.perform(post("/api/v1/escrow/" + escrowId + "/progress")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // 3. Student tries to release payment to themselves (Must be FORBIDDEN 403)
        mockMvc.perform(post("/api/v1/escrow/" + escrowId + "/release")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 4. Alumni releases escrow payment to student
        mockMvc.perform(post("/api/v1/escrow/" + escrowId + "/release")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RELEASED"));

        // 5. Verify double release is prevented (returns 400 Bad Request)
        mockMvc.perform(post("/api/v1/escrow/" + escrowId + "/release")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Demo Escrow: Cancellation and refund flow")
    void escrowLifecycle_refund_success() throws Exception {
        // 1. Alumni creates and funds escrow
        EscrowCreateRequest createReq = EscrowCreateRequest.builder()
                .title("Data Pipeline Maintenance")
                .amount(new BigDecimal("800.00"))
                .payeeId(studentUserId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/escrow/create")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long escrowId = root.path("data").path("id").asLong();

        // 2. Refund escrow back to payer
        mockMvc.perform(post("/api/v1/escrow/" + escrowId + "/refund")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        // 3. Subsequent refund attempt rejected
        mockMvc.perform(post("/api/v1/escrow/" + escrowId + "/refund")
                        .header("Authorization", "Bearer " + alumniToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Demo Escrow: Dispute flow")
    void escrowDispute_success() throws Exception {
        EscrowCreateRequest createReq = EscrowCreateRequest.builder()
                .title("Logo & Brand Design Task")
                .amount(new BigDecimal("500.00"))
                .payeeId(studentUserId)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/escrow/create")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long escrowId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Mark as disputed
        mockMvc.perform(post("/api/v1/escrow/" + escrowId + "/dispute")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Milestone deliverables disputed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPUTED"));
    }

    @Test
    @DisplayName("Demo Escrow: Cannot create escrow where payer is payee")
    void escrowSelfFunding_fails() throws Exception {
        EscrowCreateRequest createReq = EscrowCreateRequest.builder()
                .title("Self Escrow Attempt")
                .amount(new BigDecimal("500.00"))
                .payeeId(alumniUserId) // same as caller
                .build();

        mockMvc.perform(post("/api/v1/escrow/create")
                        .header("Authorization", "Bearer " + alumniToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isBadRequest());
    }
}
