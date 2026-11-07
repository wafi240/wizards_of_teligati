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
public class ApplicationStatusUpdateRequest {
    @NotBlank(message = "Status is required (SHORTLISTED, ACCEPTED, REJECTED, WITHDRAWN)")
    private String status;
}
