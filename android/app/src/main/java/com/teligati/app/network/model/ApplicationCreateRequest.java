package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ApplicationCreateRequest {
    @SerializedName("opportunityType")
    private String opportunityType;

    @SerializedName("jobId")
    private Long jobId;

    @SerializedName("projectId")
    private Long projectId;

    @SerializedName("statement")
    private String statement;

    public ApplicationCreateRequest(String opportunityType, Long jobId, Long projectId, String statement) {
        this.opportunityType = opportunityType;
        this.jobId = jobId;
        this.projectId = projectId;
        this.statement = statement;
    }

    public String getOpportunityType() { return opportunityType; }
    public Long getJobId() { return jobId; }
    public Long getProjectId() { return projectId; }
    public String getStatement() { return statement; }
}
