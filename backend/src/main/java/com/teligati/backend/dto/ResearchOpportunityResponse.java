package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ResearchOpportunityResponse {
    private Long id;
    private String title;
    private String name; // alias
    private String description;
    private String category;
    private String requiredSkills;
    private String technologies; // alias
    private String preferredBackground;
    private Integer positionsCount;
    private LocalDateTime deadline;
    private String eligibilityCriteria;
    private String duration;
    private String researchType;
    private String type; // alias
    private String status;
    private Double minCgpa;
    private String ownerDept;
    private String paperDoi;

    private Long ownerId;
    private String ownerName;
    private String ownerRole;

    @JsonProperty("isApplied")
    private boolean isApplied;

    private Integer applicantsCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
