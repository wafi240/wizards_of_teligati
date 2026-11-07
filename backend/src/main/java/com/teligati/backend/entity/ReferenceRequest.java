package com.teligati.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reference_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "recipient_role", nullable = false, length = 50)
    private String recipientRole;

    @Column(nullable = false, length = 100)
    private String purpose;

    @Column(name = "target_institution", nullable = false)
    private String targetInstitution;

    @Column(name = "target_program")
    private String targetProgram;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "personal_message", nullable = false, columnDefinition = "TEXT")
    private String personalMessage;

    @Column(name = "supporting_info", columnDefinition = "TEXT")
    private String supportingInfo;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "recommendation_text", columnDefinition = "TEXT")
    private String recommendationText;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
