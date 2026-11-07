package com.teligati.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpRequest {
    @NotNull(message = "Simulated top-up amount is required")
    @DecimalMin(value = "1.00", message = "Minimum simulated top-up amount is 1.00")
    @DecimalMax(value = "1000000.00", message = "Maximum simulated top-up amount is 1,000,000.00")
    private BigDecimal amount;

    private String paymentMethod; // e.g., "SIMULATED_BKASH", "SIMULATED_BANK", "SIMULATED_CAMPUS_CARD"
    private String note;
}
