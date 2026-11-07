package com.teligati.app.network.model;

public class RecommendationNetworkRequest {
    private String recommendationText;
    private String responseNotes;

    public RecommendationNetworkRequest() {}

    public RecommendationNetworkRequest(String recommendationText, String responseNotes) {
        this.recommendationText = recommendationText;
        this.responseNotes = responseNotes;
    }

    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }

    public String getResponseNotes() { return responseNotes; }
    public void setResponseNotes(String responseNotes) { this.responseNotes = responseNotes; }
}
