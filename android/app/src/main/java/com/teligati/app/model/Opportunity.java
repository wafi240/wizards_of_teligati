package com.teligati.app.model;

import java.util.List;

public class Opportunity {
    private long id;
    private String title;
    private OpportunityCategory category;
    private String organization;
    private String location;
    private String postedByName;
    private String posterRole;
    private String compensation;
    private String duration;
    private String description;
    private List<String> skillsRequired;
    private Double minCgpa;
    private boolean isApplied;
    private String postedByUserId;

    public Opportunity(long id, String title, OpportunityCategory category, String organization,
                       String location, String postedByName, String posterRole, String compensation,
                       String duration, String description, List<String> skillsRequired, Double minCgpa) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.organization = organization;
        this.location = location;
        this.postedByName = postedByName;
        this.posterRole = posterRole;
        this.compensation = compensation;
        this.duration = duration;
        this.description = description;
        this.skillsRequired = skillsRequired;
        this.minCgpa = minCgpa;
        this.isApplied = false;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public OpportunityCategory getCategory() { return category; }
    public String getOrganization() { return organization; }
    public String getLocation() { return location; }
    public String getPostedByName() { return postedByName; }
    public String getPosterRole() { return posterRole; }
    public String getCompensation() { return compensation; }
    public String getDuration() { return duration; }
    public String getDescription() { return description; }
    public List<String> getSkillsRequired() { return skillsRequired; }
    public Double getMinCgpa() { return minCgpa; }
    public boolean isApplied() { return isApplied; }
    public void setApplied(boolean applied) { isApplied = applied; }

    public String getPostedByUserId() {
        return postedByUserId != null && !postedByUserId.isEmpty() ? postedByUserId : postedByName;
    }
    public void setPostedByUserId(String postedByUserId) {
        this.postedByUserId = postedByUserId;
    }
}
