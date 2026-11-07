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
public class AlumniDirectoryResponse {
    private Long userId;
    private String name;
    private String department;
    private String batch;
    private String graduationYear;
    private String currentCompany;
    private String currentPosition;
    private String country;
    private String city;
    private String skills;
    private String linkedinUrl;
    private String bio;

    @JsonProperty("isAvailableForMentorship")
    private boolean isAvailableForMentorship;

    @JsonProperty("isActivelyHiring")
    private boolean isActivelyHiring;

    @JsonProperty("isVerified")
    private boolean isVerified;
}
