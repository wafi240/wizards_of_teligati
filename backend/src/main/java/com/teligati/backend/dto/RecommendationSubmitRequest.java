package com.teligati.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSubmitRequest {

    @NotBlank(message = "Recommendation text is required")
    private String recommendationText;

    private String responseNotes;
}
