package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ProjectCreateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("technologies")
    private String technologies;

    @SerializedName("githubUrl")
    private String githubUrl;

    @SerializedName("isOpenForContribution")
    private Boolean isOpenForContribution;

    @SerializedName("type")
    private String type;

    @SerializedName("ownerDept")
    private String ownerDept;

    @SerializedName("paperDoi")
    private String paperDoi;

    public ProjectCreateRequest(String name, String description, String technologies,
                                String githubUrl, Boolean isOpenForContribution,
                                String type, String ownerDept, String paperDoi) {
        this.name = name;
        this.description = description;
        this.technologies = technologies;
        this.githubUrl = githubUrl;
        this.isOpenForContribution = isOpenForContribution;
        this.type = type;
        this.ownerDept = ownerDept;
        this.paperDoi = paperDoi;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTechnologies() { return technologies; }
    public String getGithubUrl() { return githubUrl; }
    public Boolean getIsOpenForContribution() { return isOpenForContribution; }
    public String getType() { return type; }
    public String getOwnerDept() { return ownerDept; }
    public String getPaperDoi() { return paperDoi; }
}
