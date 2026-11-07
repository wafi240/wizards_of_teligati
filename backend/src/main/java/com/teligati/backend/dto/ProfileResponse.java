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
public class ProfileResponse {
    private Long userId;
    private String kuetId;
    private String email;
    private String name;
    private String role;

    @JsonProperty("isVerified")
    private boolean isVerified;

    private String department;
    private String batch;
    private String graduationYear;
    private String status;

    private String currentCompany;
    private String currentPosition;
    private String country;
    private String city;

    @JsonProperty("isAvailableForMentorship")
    private Boolean isAvailableForMentorship;

    private String bio;
    private String skills;
    private String projects;
    private String githubUrl;
    private String linkedinUrl;

    // Academic privacy: visible only to profile owner
    private Double cgpa;

    private String specialization;
    private String clubs;

    // Faculty specific
    private String designation;
    private String researchFocus;
    private String labName;
    private Integer publicationsCount;
    private String contactEmail;

    // Alumni specific
    @JsonProperty("isActivelyHiring")
    private Boolean isActivelyHiring;
}
