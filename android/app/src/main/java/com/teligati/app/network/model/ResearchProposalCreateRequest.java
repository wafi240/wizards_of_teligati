package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ResearchProposalCreateRequest {
    @SerializedName("topic")
    private String topic;

    @SerializedName("proposalAbstract")
    private String proposalAbstract;

    @SerializedName("degreeType")
    private String degreeType;

    @SerializedName("facultyId")
    private Long facultyId;

    public ResearchProposalCreateRequest(String topic, String proposalAbstract, String degreeType, Long facultyId) {
        this.topic = topic;
        this.proposalAbstract = proposalAbstract;
        this.degreeType = degreeType;
        this.facultyId = facultyId;
    }

    public String getTopic() { return topic; }
    public String getProposalAbstract() { return proposalAbstract; }
    public String getDegreeType() { return degreeType; }
    public Long getFacultyId() { return facultyId; }
}
