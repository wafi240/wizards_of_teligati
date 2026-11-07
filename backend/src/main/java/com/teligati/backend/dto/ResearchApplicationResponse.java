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
public class ResearchApplicationResponse {
    private Long id;
    private Long opportunityId;
    private String opportunityTitle;
    private Long applicantId;
    private String applicantName;
    private String applicantRoll;
    private String applicantDept;
    private Double applicantCgpa;
    private String applicantSkills;
    private String statement;
    private String feedback;
    private String reviewNotes;
    private String status; // PENDING, UNDER_REVIEW, REVISION_REQUESTED, ACCEPTED, REJECTED, WITHDRAWN

    private Long teacherId;
    private String teacherName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
