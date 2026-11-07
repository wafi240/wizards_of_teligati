package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationResponse {
    private Long id;
    private String opportunityType;
    private Long jobId;
    private String jobTitle;
    private Long projectId;
    private String projectTitle;

    private Long applicantId;
    private String applicantName;
    private String applicantRoll;
    private String applicantDept;
    private Double applicantCgpa;
    private String applicantSkills;

    private String statement;
    private String status; // APPLIED, SHORTLISTED, ACCEPTED, REJECTED, WITHDRAWN

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
