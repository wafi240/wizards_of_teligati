package com.teligati.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceRequestResponse {

    private Long id;

    // Student Information
    private Long studentId;
    private String studentName;
    private String studentRoll;
    private String studentDept;
    private String studentEmail;
    private Double studentCgpa;
    private String studentSkills;

    // Recipient Information
    private Long recipientId;
    private String recipientName;
    private String recipientRole;
    private String recipientDept;
    private String recipientEmail;

    // Request Details
    private String purpose;
    private String targetInstitution;
    private String targetProgram;
    private LocalDateTime deadline;
    private String personalMessage;
    private String supportingInfo;

    // Status & Output
    private String status;
    private String recommendationText;
    private String responseNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
