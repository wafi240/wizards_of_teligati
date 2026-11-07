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
public class ResearchPaperRequest {
    @NotBlank(message = "Paper title is required")
    private String title;

    @NotBlank(message = "Abstract text is required")
    private String abstractText;

    @NotBlank(message = "Research field is required")
    private String researchField;

    @NotBlank(message = "Author names are required")
    private String authors;

    private String publicationDate;
    private String doi;
    private String referenceUrl;
    private String attachmentUrl;
}
