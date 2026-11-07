package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
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
public class ResearchOpportunityRequest {
    private String title;
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    private String category;
    private String requiredSkills;
    private String preferredBackground;
    private Integer positionsCount;
    private LocalDateTime deadline;
    private String eligibilityCriteria;
    private String duration;
    private String researchType;
    private String status;
    private Double minCgpa;
    private String ownerDept;
    private String paperDoi;

    public String getResolvedTitle() {
        if (title != null && !title.isBlank()) return title;
        if (name != null && !name.isBlank()) return name;
        return "Untitled Research Opportunity";
    }
}
