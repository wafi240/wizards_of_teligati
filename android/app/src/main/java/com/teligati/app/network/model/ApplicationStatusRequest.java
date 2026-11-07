package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ApplicationStatusRequest {
    @SerializedName("status")
    private String status;

    @SerializedName("feedback")
    private String feedback;

    @SerializedName("reviewNotes")
    private String reviewNotes;

    public ApplicationStatusRequest() {}

    public ApplicationStatusRequest(String status) {
        this.status = status;
    }

    public ApplicationStatusRequest(String status, String feedback) {
        this.status = status;
        this.feedback = feedback;
    }

    public ApplicationStatusRequest(String status, String feedback, String reviewNotes) {
        this.status = status;
        this.feedback = feedback;
        this.reviewNotes = reviewNotes;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
