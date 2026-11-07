package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class JobCreateRequest {
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

    public JobCreateRequest(String title, String company, String organization, String location,
                            String type, String description, String compensation, String category,
                            String duration, String skillsRequired, Double minCgpa) {
        this.title = title;
        this.company = company;
        this.organization = organization;
        this.location = location;
        this.type = type;
        this.description = description;
        this.compensation = compensation;
        this.category = category;
        this.duration = duration;
        this.skillsRequired = skillsRequired;
        this.minCgpa = minCgpa;
    }

    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getOrganization() { return organization; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getCompensation() { return compensation; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public String getSkillsRequired() { return skillsRequired; }
    public Double getMinCgpa() { return minCgpa; }
}
