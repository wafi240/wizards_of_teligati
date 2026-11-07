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
public class ResearchProposalResponse {
    private Long id;
    private String topic;
    private String proposalAbstract;
    private String degreeType;
    private String status;
    private String feedback;

    private Long studentId;
    private String studentName;
    private String studentRoll;

    private Long facultyId;
    private String facultyName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
