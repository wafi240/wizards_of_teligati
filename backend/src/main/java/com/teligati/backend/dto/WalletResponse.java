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
public class WalletResponse {
    private Long id;
    private Long userId;
    private BigDecimal availableBalance;
    private BigDecimal escrowBalance;
    private String currency;
    private LocalDateTime updatedAt;

    @Builder.Default
    private String disclaimer = "DEMO WALLET — NO REAL MONEY INVOLVED";

    @Builder.Default
    private String demoNotice = "DEMO MODE — All balances and transactions are simulated. No real money is transferred.";
}
