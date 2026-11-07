package com.teligati.app.network.model;

public class ReferenceDecisionNetworkRequest {
    private String status;
    private String responseNotes;

    public ReferenceDecisionNetworkRequest() {}

    public ReferenceDecisionNetworkRequest(String status, String responseNotes) {
        this.status = status;
        this.responseNotes = responseNotes;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponseNotes() { return responseNotes; }
    public void setResponseNotes(String responseNotes) { this.responseNotes = responseNotes; }
}
