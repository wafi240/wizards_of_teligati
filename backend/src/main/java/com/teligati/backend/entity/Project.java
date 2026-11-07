package com.teligati.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String technologies;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "is_open_for_contribution", nullable = false)
    @Builder.Default
    private boolean isOpenForContribution = true;

    @Column(name = "type")
    @Builder.Default
    private String type = "Research Opportunity";

    @Column(name = "owner_dept")
    private String ownerDept;

    @Column(name = "contributors_count")
    @Builder.Default
    private Integer contributorsCount = 0;

    @Column(name = "paper_doi")
    private String paperDoi;

    @Column(nullable = false)
    @Builder.Default
    private String status = "OPEN"; // OPEN, CLOSED, COMPLETED

    @Column(name = "positions_count")
    @Builder.Default
    private Integer positionsCount = 1;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "preferred_background")
    private String preferredBackground;

    @Column(name = "eligibility_criteria", columnDefinition = "TEXT")
    private String eligibilityCriteria;

    @Column(name = "duration")
    private String duration;

    @Column(name = "skills_required")
    private String skillsRequired;

    @Column(name = "category")
    @Builder.Default
    private String category = "COMPUTER_SCIENCE";

    @Column(name = "min_cgpa")
    private Double minCgpa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (type == null) {
            type = "Research Opportunity";
        }
        if (status == null) {
            status = "OPEN";
        }
        if (contributorsCount == null) {
            contributorsCount = 0;
        }
        if (positionsCount == null) {
            positionsCount = 1;
        }
        if (category == null) {
            category = "COMPUTER_SCIENCE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
