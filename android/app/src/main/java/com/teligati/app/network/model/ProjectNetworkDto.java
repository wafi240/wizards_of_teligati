package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ProjectNetworkDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("technologies")
    private String technologies;

    @SerializedName("githubUrl")
    private String githubUrl;

    @SerializedName("isOpenForContribution")
    private boolean isOpenForContribution;

    @SerializedName("type")
    private String type;

    @SerializedName("ownerDept")
    private String ownerDept;

    @SerializedName("contributorsCount")
    private Integer contributorsCount;

    @SerializedName("paperDoi")
    private String paperDoi;

    @SerializedName("status")
    private String status;

    @SerializedName("ownerId")
    private Long ownerId;

    @SerializedName("ownerName")
    private String ownerName;

    @SerializedName("isApplied")
    private boolean isApplied;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() { return id != null ? id : 0L; }
    public String getName() { return name != null ? name : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public String getTechnologies() { return technologies != null ? technologies : ""; }
    public String getGithubUrl() { return githubUrl != null ? githubUrl : ""; }
    public boolean isOpenForContribution() { return isOpenForContribution; }
    public String getType() { return type != null ? type : "Research Opportunity"; }
    public String getOwnerDept() { return ownerDept != null ? ownerDept : ""; }
    public Integer getContributorsCount() { return contributorsCount != null ? contributorsCount : 1; }
    public String getPaperDoi() { return paperDoi != null ? paperDoi : ""; }
    public String getStatus() { return status != null ? status : "OPEN"; }
    public Long getOwnerId() { return ownerId != null ? ownerId : 0L; }
    public String getOwnerName() { return ownerName != null ? ownerName : ""; }
    public boolean isApplied() { return isApplied; }
    public String getCreatedAt() { return createdAt; }
}
