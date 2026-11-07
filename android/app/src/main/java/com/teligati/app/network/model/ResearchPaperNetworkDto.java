package com.teligati.app.network.model;

public class ResearchPaperNetworkDto {
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
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAbstractText() { return abstractText; }
    public String getResearchField() { return researchField; }
    public String getAuthors() { return authors; }
    public String getPublicationDate() { return publicationDate; }
    public String getDoi() { return doi; }
    public String getReferenceUrl() { return referenceUrl; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getTeacherDept() { return teacherDept; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
