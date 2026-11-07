package com.teligati.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceDecisionRequest {

    @NotBlank(message = "Decision status is required")
    @Pattern(regexp = "^(ACCEPTED|DECLINED)$", message = "Status must be either ACCEPTED or DECLINED")
    private String status;

    private String responseNotes;
}
