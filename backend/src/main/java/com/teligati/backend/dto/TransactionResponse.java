package com.teligati.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private String title;
    private String category;
    private BigDecimal amount;
    private String type; // TOPUP, WITHDRAWAL, ESCROW_LOCK, ESCROW_RELEASE, ESCROW_REFUND
    private String status;
    private LocalDateTime timestamp;
    private LocalDateTime completedAt;
    private Long userId;
    private Long escrowId;

    @Builder.Default
    private String disclaimer = "DEMO WALLET — NO REAL MONEY INVOLVED";

    @Builder.Default
    private String demoNotice = "DEMO MODE — All balances and transactions are simulated. No real money is transferred.";
}
