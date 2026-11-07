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
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private String technologies;
    private String githubUrl;

    @JsonProperty("isOpenForContribution")
    private boolean isOpenForContribution;

    private String type;
    private String ownerDept;
    private Integer contributorsCount;
    private String paperDoi;
    private String status;

    private Long ownerId;
    private String ownerName;

    @JsonProperty("isApplied")
    private boolean isApplied;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
