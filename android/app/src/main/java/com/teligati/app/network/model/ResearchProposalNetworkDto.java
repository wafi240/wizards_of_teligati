package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ResearchProposalNetworkDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("topic")
    private String topic;

    @SerializedName("proposalAbstract")
    private String proposalAbstract;

    @SerializedName("degreeType")
    private String degreeType;

    @SerializedName("status")
    private String status;

    @SerializedName("feedback")
    private String feedback;

    @SerializedName("studentId")
    private Long studentId;

    @SerializedName("studentName")
    private String studentName;

    @SerializedName("studentRoll")
    private String studentRoll;

    @SerializedName("facultyId")
    private Long facultyId;

    @SerializedName("facultyName")
    private String facultyName;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() { return id != null ? id : 0L; }
    public String getTopic() { return topic != null ? topic : ""; }
    public String getProposalAbstract() { return proposalAbstract != null ? proposalAbstract : ""; }
    public String getDegreeType() { return degreeType != null ? degreeType : ""; }
    public String getStatus() { return status != null ? status : "Pending"; }
    public String getFeedback() { return feedback != null ? feedback : ""; }
    public Long getStudentId() { return studentId != null ? studentId : 0L; }
    public String getStudentName() { return studentName != null ? studentName : ""; }
    public String getStudentRoll() { return studentRoll != null ? studentRoll : ""; }
    public Long getFacultyId() { return facultyId != null ? facultyId : 0L; }
    public String getFacultyName() { return facultyName != null ? facultyName : ""; }
    public String getCreatedAt() { return createdAt; }
}
