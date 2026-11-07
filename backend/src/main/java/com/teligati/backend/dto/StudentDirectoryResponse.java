package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentDirectoryResponse {
    private Long userId;
    private String roll; // kuetId
    private String name;
    private String department;
    private String batch;
    private String skills;
    private String projects;
    private String specialization;
    private String clubs;
    private String githubUrl;
    private String linkedinUrl;
    private String bio;

    @JsonProperty("isVerified")
    private boolean isVerified;
}
