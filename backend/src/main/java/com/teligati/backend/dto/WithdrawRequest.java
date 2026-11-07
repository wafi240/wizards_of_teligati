package com.teligati.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class WithdrawRequest {
    @NotNull(message = "Simulated withdrawal amount is required")
    @DecimalMin(value = "1.00", message = "Minimum simulated withdrawal amount is 1.00")
    private BigDecimal amount;

    @NotBlank(message = "Simulated destination account is required")
    private String destinationAccount; // e.g. "SIMULATED_STUDENT_BANK_ACCT"

    private String note;
}
