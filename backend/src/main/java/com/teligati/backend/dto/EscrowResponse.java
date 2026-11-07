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
public class EscrowResponse {
    private Long id;
    private String escrowReference;
    private String title;
    private String description;
    private BigDecimal amount;
    private String status;
    private Long payerId;
    private String payerName;
    private Long payeeId;
    private String payeeName;
    private Long jobId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private String disclaimer = "DEMO WALLET — NO REAL MONEY INVOLVED";

    @Builder.Default
    private String demoNotice = "DEMO MODE — All balances and transactions are simulated. No real money is transferred.";
}
