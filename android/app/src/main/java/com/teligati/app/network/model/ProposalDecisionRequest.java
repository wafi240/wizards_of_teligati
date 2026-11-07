package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ProposalDecisionRequest {
    @SerializedName("status")
    private String status;

    @SerializedName("feedback")
    private String feedback;

    public ProposalDecisionRequest(String status, String feedback) {
        this.status = status;
        this.feedback = feedback;
    }

    public String getStatus() { return status; }
    public String getFeedback() { return feedback; }
}
