package com.teligati.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    private String name;
    private String department;
    private String batch;
    private String graduationYear;
    private String status;

    private String currentCompany;
    private String currentPosition;
    private String country;
    private String city;
    private Boolean isAvailableForMentorship;

    private String bio;
    private String skills;
    private String projects;
    private String githubUrl;
    private String linkedinUrl;

    @DecimalMin(value = "0.00", message = "CGPA cannot be negative")
    @DecimalMax(value = "4.00", message = "CGPA cannot exceed 4.00")
    private Double cgpa;

    private String specialization;
    private String clubs;

    // Faculty fields
    private String designation;
    private String researchFocus;
    private String labName;
    private Integer publicationsCount;
    private String contactEmail;

    // Alumni fields
    private Boolean isActivelyHiring;
}
