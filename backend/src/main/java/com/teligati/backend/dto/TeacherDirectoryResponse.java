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
public class TeacherDirectoryResponse {
    private Long userId;
    private String name;
    private String department;
    private String designation;
    private String researchFocus;
    private String labName;
    private Integer publicationsCount;
    private String contactEmail;
    private String bio;

    @JsonProperty("isVerified")
    private boolean isVerified;
}
