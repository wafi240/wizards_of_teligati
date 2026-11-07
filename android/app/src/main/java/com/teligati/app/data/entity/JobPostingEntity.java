package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "job_postings")
public class JobPostingEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String category; // "ASSISTANT_WORK", "FULLTIME_JOBS", "RESEARCH_COLLABORATION", "MICRO_TASKS"
    private String organization;
    private String location;
    private String postedByName;
    private String posterRole;
    private String compensation;
    private String duration;
    private String description;
    private String skillsRequired; // comma-separated
    private Double minCgpa;
    private boolean isApplied;
    private String postedByUserId;
    private long createdAt;

    public JobPostingEntity(long id, String title, String category, String organization,
                            String location, String postedByName, String posterRole,
                            String compensation, String duration, String description,
                            String skillsRequired, Double minCgpa, boolean isApplied,
                            String postedByUserId, long createdAt) {
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
        this.isApplied = isApplied;
        this.postedByUserId = postedByUserId;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPostedByName() { return postedByName; }
    public void setPostedByName(String postedByName) { this.postedByName = postedByName; }

    public String getPosterRole() { return posterRole; }
    public void setPosterRole(String posterRole) { this.posterRole = posterRole; }

    public String getCompensation() { return compensation; }
    public void setCompensation(String compensation) { this.compensation = compensation; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSkillsRequired() { return skillsRequired; }
    public void setSkillsRequired(String skillsRequired) { this.skillsRequired = skillsRequired; }

    public Double getMinCgpa() { return minCgpa; }
    public void setMinCgpa(Double minCgpa) { this.minCgpa = minCgpa; }

    public boolean isApplied() { return isApplied; }
    public void setApplied(boolean applied) { isApplied = applied; }

    public String getPostedByUserId() { return postedByUserId; }
    public void setPostedByUserId(String postedByUserId) { this.postedByUserId = postedByUserId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
