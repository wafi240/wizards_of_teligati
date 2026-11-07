package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResearchProposalRequest {
    @NotBlank(message = "Proposal topic is required")
    private String topic;

    @NotBlank(message = "Proposal abstract is required")
    private String proposalAbstract;

    @NotBlank(message = "Degree type is required")
    private String degreeType; // PhD Proposal, Undergrad Thesis, Master Thesis

    @NotNull(message = "Target faculty ID is required")
    private Long facultyId;
}
