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
public class JobRequest {
    @NotBlank(message = "Job title is required")
    private String title;

    private String company;
    private String organization;
    private String location;
    private String type;
    private String description;
    private String compensation;
    private String category; // ASSISTANT_WORK, FULLTIME_JOBS, RESEARCH_COLLABORATION, MICRO_TASKS
    private String duration;
    private String skillsRequired;
    private Double minCgpa;
}
