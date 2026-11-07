package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationRequest {
    @NotBlank(message = "Opportunity type is required (JOB or PROJECT)")
    private String opportunityType;

    private Long jobId;
    private Long projectId;
    private String statement;
}
