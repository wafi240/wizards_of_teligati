package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResearchPaperResponse {
    private Long id;
    private String title;
    private String abstractText;
    private String researchField;
    private String authors;
    private String publicationDate;
    private String doi;
    private String referenceUrl;
    private String attachmentUrl;

    private Long teacherId;
    private String teacherName;
    private String teacherDept;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
