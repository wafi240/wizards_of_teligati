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
public class ResearchApplicationSubmitRequest {
    @NotBlank(message = "Statement of interest is required")
    private String statement;

    private String skills;
    private Double cgpa;
    private String department;
    private String labInterest;
}
