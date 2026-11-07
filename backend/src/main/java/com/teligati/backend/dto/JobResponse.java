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
public class JobResponse {
    private Long id;
    private String title;
    private String company;
    private String organization;
    private String location;
    private String type;
    private String description;
    private String compensation;
    private String category;
    private String duration;
    private String skillsRequired;
    private Double minCgpa;
    private String status;

    private Long postedByUserId;
    private String postedByName;
    private String posterRole;

    @JsonProperty("isApplied")
    private boolean isApplied;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
