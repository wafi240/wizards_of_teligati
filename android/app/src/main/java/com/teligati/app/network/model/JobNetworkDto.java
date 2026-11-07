package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class JobNetworkDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("organization")
    private String organization;

    @SerializedName("location")
    private String location;

    @SerializedName("type")
    private String type;

    @SerializedName("description")
    private String description;

    @SerializedName("compensation")
    private String compensation;

    @SerializedName("category")
    private String category;

    @SerializedName("duration")
    private String duration;

    @SerializedName("skillsRequired")
    private String skillsRequired;

    @SerializedName("minCgpa")
    private Double minCgpa;

    @SerializedName("status")
    private String status;

    @SerializedName("postedByUserId")
    private Long postedByUserId;

    @SerializedName("postedByName")
    private String postedByName;

    @SerializedName("posterRole")
    private String posterRole;

    @SerializedName("isApplied")
    private boolean isApplied;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() { return id != null ? id : 0L; }
    public String getTitle() { return title != null ? title : ""; }
    public String getCompany() { return company != null ? company : ""; }
    public String getOrganization() { return organization != null ? organization : company != null ? company : ""; }
    public String getLocation() { return location != null ? location : "Remote / KUET"; }
    public String getType() { return type != null ? type : "Full-time"; }
    public String getDescription() { return description != null ? description : ""; }
    public String getCompensation() { return compensation != null ? compensation : ""; }
    public String getCategory() { return category != null ? category : "FULLTIME_JOBS"; }
    public String getDuration() { return duration != null ? duration : ""; }
    public String getSkillsRequired() { return skillsRequired != null ? skillsRequired : ""; }
    public Double getMinCgpa() { return minCgpa != null ? minCgpa : 0.0; }
    public String getStatus() { return status != null ? status : "OPEN"; }
    public Long getPostedByUserId() { return postedByUserId != null ? postedByUserId : 0L; }
    public String getPostedByName() { return postedByName != null ? postedByName : ""; }
    public String getPosterRole() { return posterRole != null ? posterRole : ""; }
    public boolean isApplied() { return isApplied; }
    public String getCreatedAt() { return createdAt; }
}
