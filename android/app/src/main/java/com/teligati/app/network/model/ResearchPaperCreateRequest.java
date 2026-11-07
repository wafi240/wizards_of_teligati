package com.teligati.app.network.model;

public class ResearchPaperCreateRequest {
    private String title;
    private String abstractText;
    private String researchField;
    private String authors;
    private String publicationDate;
    private String doi;
    private String referenceUrl;
    private String attachmentUrl;

    public ResearchPaperCreateRequest(String title, String abstractText, String researchField,
                                      String authors, String publicationDate, String doi,
                                      String referenceUrl, String attachmentUrl) {
        this.title = title;
        this.abstractText = abstractText;
        this.researchField = researchField;
        this.authors = authors;
        this.publicationDate = publicationDate;
        this.doi = doi;
        this.referenceUrl = referenceUrl;
        this.attachmentUrl = attachmentUrl;
    }

    public String getTitle() { return title; }
    public String getAbstractText() { return abstractText; }
    public String getResearchField() { return researchField; }
    public String getAuthors() { return authors; }
    public String getPublicationDate() { return publicationDate; }
    public String getDoi() { return doi; }
    public String getReferenceUrl() { return referenceUrl; }
    public String getAttachmentUrl() { return attachmentUrl; }
}
