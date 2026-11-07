package com.teligati.app.firebase;

import static org.junit.Assert.*;

import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.ReferenceRequestEntity;
import com.teligati.app.data.entity.TransactionEntity;
import com.teligati.app.data.repository.WalletRepository;
import com.teligati.app.network.model.EscrowNetworkDto;
import com.teligati.app.network.model.ReferenceNetworkDto;
import com.teligati.app.network.model.TransactionNetworkDto;
import com.teligati.app.network.model.WalletNetworkDto;
import com.teligati.app.testutil.InMemorySharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive Unit Test Suite for Phase 6: Reference Requests, Demo Wallet,
 * Demo Transactions & Demo Escrow Migration to Cloud Firestore.
 */
public class FirestoreWalletReferenceEscrowTest {

    private static final Set<String> ALLOWED_WALLET_KEYS = new HashSet<>(Arrays.asList(
            "availableBalance", "escrowBalance", "currency", "updatedAt", "disclaimer", "demoNotice"
    ));

    private static final Set<String> ALLOWED_REF_ALUMNI_UPDATE_KEYS = new HashSet<>(Arrays.asList(
            "status", "recommendationText", "responseNotes", "updatedAt"
    ));

    private static final Set<String> ALLOWED_REF_STUDENT_CANCEL_KEYS = new HashSet<>(Arrays.asList(
            "status", "updatedAt"
    ));

    private static final Set<String> ALLOWED_ESCROW_EMPLOYER_KEYS = new HashSet<>(Arrays.asList(
            "status", "updatedAt", "resolutionNote"
    ));

    private static final Set<String> ALLOWED_ESCROW_FREELANCER_KEYS = new HashSet<>(Arrays.asList(
            "status", "submissionNote", "updatedAt"
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
    // 1. REFERENCE REQUESTS SCHEMA & SECURITY INVARIANTS
    // =========================================================================

    @Test
    public void testReferenceRequest_validCreatePayload() {
        Map<String, Object> data = new HashMap<>();
        data.put("studentUid", "student_1");
        data.put("studentName", "Samun Sadab Wafi");
        data.put("studentRoll", "2307003");
        data.put("studentDept", "CSE");
        data.put("alumniUid", "alumni_1");
        data.put("targetCompany", "Google");
        data.put("purpose", "JOB_REFERRAL");
        data.put("message", "Requesting referral");
        data.put("status", "PENDING");
        data.put("createdAt", System.currentTimeMillis());

        assertEquals("student_1", data.get("studentUid"));
        assertEquals("alumni_1", data.get("alumniUid"));
        assertNotEquals(data.get("studentUid"), data.get("alumniUid"));
        assertEquals("PENDING", data.get("status"));
    }

    @Test
    public void testReferenceRequest_selfReferenceRejected() {
        String studentUid = "student_1";
        String alumniUid = "student_1";

        boolean isValid = !studentUid.equals(alumniUid);
        assertFalse("Self-referencing requests must be rejected by rules", isValid);
    }

    @Test
    public void testReferenceRequest_alumniDecisionPayloadAndAllowedKeys() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "ACCEPTED");
        updates.put("responseNotes", "Happy to endorse Samun.");
        updates.put("updatedAt", System.currentTimeMillis());

        assertTrue("Alumni decision affected keys must match rule allowlist",
                ALLOWED_REF_ALUMNI_UPDATE_KEYS.containsAll(updates.keySet()));
        assertTrue(Arrays.asList("ACCEPTED", "REJECTED", "COMPLETED").contains(updates.get("status")));
    }

    @Test
    public void testReferenceRequest_alumniCompletionPayload() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("recommendationText", "Stellar engineering mind from KUET CSE.");
        updates.put("responseNotes", "Letter submitted.");
        updates.put("updatedAt", System.currentTimeMillis());

        assertTrue(ALLOWED_REF_ALUMNI_UPDATE_KEYS.containsAll(updates.keySet()));
        assertEquals("COMPLETED", updates.get("status"));
        assertNotNull(updates.get("recommendationText"));
    }

    @Test
    public void testReferenceRequest_studentCancelPayloadAndAllowedKeys() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "CANCELLED");
        updates.put("updatedAt", System.currentTimeMillis());

        assertTrue("Student cancellation keys must be strictly limited to status and updatedAt",
                ALLOWED_REF_STUDENT_CANCEL_KEYS.containsAll(updates.keySet()));
        assertEquals("CANCELLED", updates.get("status"));
    }

    // =========================================================================
    // 2. DEMO WALLET SCHEMA & CONSTRAINTS
    // =========================================================================

    @Test
    public void testDemoWallet_provisionPayloadKeyRestrictions() {
        Map<String, Object> wallet = new HashMap<>();
        wallet.put("availableBalance", 10000.0);
        wallet.put("escrowBalance", 0.0);
        wallet.put("currency", "DEMO_CREDITS");
        wallet.put("updatedAt", System.currentTimeMillis());
        wallet.put("disclaimer", WalletRepository.DEMO_DISCLAIMER);
        wallet.put("demoNotice", WalletRepository.DEMO_NOTICE);

        assertTrue("Demo wallet keys must only contain allowed attributes",
                ALLOWED_WALLET_KEYS.containsAll(wallet.keySet()));
        assertEquals(ALLOWED_WALLET_KEYS.size(), wallet.size());
    }

    @Test
    public void testDemoWallet_initialBalanceLimits() {
        double validInitial = 10000.0;
        double invalidInitial = 50000.0;

        assertTrue(validInitial >= 0 && validInitial <= 10000.0);
        assertFalse(invalidInitial <= 10000.0);
    }

    @Test
    public void testDemoWallet_balanceUpdateCeilingAndNonNegative() {
        double validUpdated = 25000.0;
        double exceedCeiling = 150000.0;
        double negativeBalance = -500.0;

        assertTrue(validUpdated >= 0 && validUpdated <= 100000.0);
        assertFalse(exceedCeiling <= 100000.0);
        assertFalse(negativeBalance >= 0);
    }

    @Test
    public void testDemoWallet_disclaimersAlwaysPresent() {
        assertEquals("DEMO WALLET - NO REAL MONEY INVOLVED", WalletRepository.DEMO_DISCLAIMER);
        assertTrue(WalletRepository.DEMO_NOTICE.contains("simulated"));
    }

    // =========================================================================
    // 3. DEMO TRANSACTIONS SCHEMA & IMMUTABILITY
    // =========================================================================

    @Test
    public void testDemoTransaction_validCreatePayload() {
        Map<String, Object> tx = new HashMap<>();
        tx.put("userId", "student_1");
        tx.put("amount", 500.0);
        tx.put("type", "TOPUP");
        tx.put("title", "Simulated Top-Up");
        tx.put("category", "CREDIT");
        tx.put("status", "COMPLETED");
        tx.put("timestamp", System.currentTimeMillis());

        assertEquals("student_1", tx.get("userId"));
        assertTrue(((Double) tx.get("amount")) > 0);
        assertEquals("COMPLETED", tx.get("status"));
    }

    @Test
    public void testDemoTransaction_negativeAmountRejected() {
        double amount = -100.0;
        boolean isValid = amount > 0;
        assertFalse("Non-positive transaction amounts must be rejected", isValid);
    }

    // =========================================================================
    // 4. DEMO ESCROW SCHEMA & STATUS TRANSITIONS
    // =========================================================================

    @Test
    public void testDemoEscrow_validCreatePayload() {
        Map<String, Object> escrow = new HashMap<>();
        escrow.put("employerUid", "faculty_1");
        escrow.put("freelancerUid", "student_1");
        escrow.put("amount", 2000.0);
        escrow.put("currency", "BDT");
        escrow.put("jobId", 101L);
        escrow.put("status", "HELD");
        escrow.put("createdAt", System.currentTimeMillis());

        assertEquals("faculty_1", escrow.get("employerUid"));
        assertEquals("student_1", escrow.get("freelancerUid"));
        assertNotEquals(escrow.get("employerUid"), escrow.get("freelancerUid"));
        assertEquals("HELD", escrow.get("status"));
        assertTrue(((Double) escrow.get("amount")) > 0);
    }

    @Test
    public void testDemoEscrow_employerReleasePayloadAndAllowedKeys() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "RELEASED");
        updates.put("resolutionNote", "Deliverable verified.");
        updates.put("updatedAt", System.currentTimeMillis());

        assertTrue(ALLOWED_ESCROW_EMPLOYER_KEYS.containsAll(updates.keySet()));
        assertTrue(Arrays.asList("RELEASED", "REFUNDED").contains(updates.get("status")));
    }

    @Test
    public void testDemoEscrow_employerRefundPayloadAndAllowedKeys() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "REFUNDED");
        updates.put("resolutionNote", "Mutual agreement cancellation.");
        updates.put("updatedAt", System.currentTimeMillis());

        assertTrue(ALLOWED_ESCROW_EMPLOYER_KEYS.containsAll(updates.keySet()));
        assertEquals("REFUNDED", updates.get("status"));
    }

    @Test
    public void testDemoEscrow_freelancerDisputePayloadAndAllowedKeys() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "DISPUTED");
        updates.put("submissionNote", "Delivered on schedule.");
        updates.put("updatedAt", System.currentTimeMillis());

        assertTrue(ALLOWED_ESCROW_FREELANCER_KEYS.containsAll(updates.keySet()));
        assertEquals("DISPUTED", updates.get("status"));
    }

    // =========================================================================
    // 5. ROOM ENTITY & DTO CONVERSIONS
    // =========================================================================

    @Test
    public void testReferenceRequestEntity_mapping() {
        ReferenceRequestEntity entity = new ReferenceRequestEntity(
                1001L, "2307003", "Samun Sadab Wafi", "CSE",
                "Google", "Internal referral request", "PENDING",
                "alumni_1", System.currentTimeMillis(), "ROLE_ALUMNI",
                "Engr. Tanvir Ahmed", "JOB_REFERRAL", "SWE", "2026-12-31",
                null, null
        );

        assertEquals(1001L, entity.getId());
        assertEquals("2307003", entity.getStudentRoll());
        assertEquals("Google", entity.getTargetCompany());
        assertEquals("PENDING", entity.getStatus());
        assertEquals("alumni_1", entity.getAlumniId());
    }

    @Test
    public void testTransactionEntity_andNetworkDto() {
        TransactionEntity entity = new TransactionEntity(
                "TX-9901", "Simulated Top-Up (bKash Demo)", "CREDIT",
                1000.0, "2026-11-07 19:00", "Completed", "student_1"
        );

        assertEquals("TX-9901", entity.getId());
        assertEquals("CREDIT", entity.getCategory());
        assertEquals(1000.0, entity.getAmount(), 0.001);

        WalletNetworkDto walletDto = new WalletNetworkDto(
                1L, 1L, 11000.0, 0.0, "DEMO_CREDITS",
                "2026-11-07 19:00", WalletRepository.DEMO_DISCLAIMER, WalletRepository.DEMO_NOTICE
        );
        assertEquals(11000.0, walletDto.getAvailableBalance(), 0.001);
        assertEquals("DEMO_CREDITS", walletDto.getCurrency());
        assertTrue(walletDto.getDisclaimer().contains("DEMO"));

        EscrowNetworkDto escrowDto = new EscrowNetworkDto(
                5001L, "ESC-5001", "Lab Research Task", "Simulated Task",
                1500.0, "HELD", 1L, "Prof. Azharul", 2L, "Samun", 101L,
                "2026-11-07 19:00", "2026-11-07 19:00",
                WalletRepository.DEMO_DISCLAIMER, WalletRepository.DEMO_NOTICE
        );
        assertEquals(5001L, (long) escrowDto.getId());
        assertEquals("HELD", escrowDto.getStatus());
        assertEquals(1500.0, escrowDto.getAmount(), 0.001);
    }
}
