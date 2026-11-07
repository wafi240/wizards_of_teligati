package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ApplicationNetworkDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("opportunityType")
    private String opportunityType;

    @SerializedName("jobId")
    private Long jobId;

    @SerializedName("jobTitle")
    private String jobTitle;

    @SerializedName("projectId")
    private Long projectId;

    @SerializedName("projectTitle")
    private String projectTitle;

    @SerializedName("applicantId")
    private Long applicantId;

    @SerializedName("applicantName")
    private String applicantName;

    @SerializedName("applicantRoll")
    private String applicantRoll;

    @SerializedName("applicantDept")
    private String applicantDept;

    @SerializedName("applicantCgpa")
    private Double applicantCgpa;

    @SerializedName("applicantSkills")
    private String applicantSkills;

    @SerializedName("statement")
    private String statement;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() { return id != null ? id : 0L; }
    public String getOpportunityType() { return opportunityType != null ? opportunityType : "JOB"; }
    public Long getJobId() { return jobId != null ? jobId : 0L; }
    public String getJobTitle() { return jobTitle != null ? jobTitle : ""; }
    public Long getProjectId() { return projectId != null ? projectId : 0L; }
    public String getProjectTitle() { return projectTitle != null ? projectTitle : ""; }
    public Long getApplicantId() { return applicantId != null ? applicantId : 0L; }
    public String getApplicantName() { return applicantName != null ? applicantName : ""; }
    public String getApplicantRoll() { return applicantRoll != null ? applicantRoll : ""; }
    public String getApplicantDept() { return applicantDept != null ? applicantDept : ""; }
    public Double getApplicantCgpa() { return applicantCgpa != null ? applicantCgpa : 0.0; }
    public String getApplicantSkills() { return applicantSkills != null ? applicantSkills : ""; }
    public String getStatement() { return statement != null ? statement : ""; }
    public String getStatus() { return status != null ? status : "APPLIED"; }
    public String getCreatedAt() { return createdAt; }
}
