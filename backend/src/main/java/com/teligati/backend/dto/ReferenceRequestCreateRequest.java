package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceRequestCreateRequest {

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    @NotBlank(message = "Purpose is required (e.g. GRADUATE_ADMISSION, JOB_REFERRAL)")
    @Size(max = 100, message = "Purpose must not exceed 100 characters")
    private String purpose;

    @NotBlank(message = "Target institution or organization is required")
    @Size(max = 255, message = "Target institution must not exceed 255 characters")
    private String targetInstitution;

    @Size(max = 255, message = "Target program/role must not exceed 255 characters")
    private String targetProgram;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;

    @NotBlank(message = "Personal pitch message is required")
    private String personalMessage;

    private String supportingInfo;
}
