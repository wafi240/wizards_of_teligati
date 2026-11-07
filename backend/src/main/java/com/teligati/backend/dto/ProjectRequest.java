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
public class ProjectRequest {
    @NotBlank(message = "Project or opportunity name is required")
    private String name;

    private String description;
    private String technologies;
    private String githubUrl;
    private Boolean isOpenForContribution;
    private String type;
    private String ownerDept;
    private String paperDoi;
}
