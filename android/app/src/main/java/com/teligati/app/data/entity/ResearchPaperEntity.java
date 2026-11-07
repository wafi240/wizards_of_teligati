package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "research_papers")
public class ResearchPaperEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String type; // e.g. "Research Paper", "Open Source Software", "Call for Co-Authors"
    private String description;
    private String technologies;
    private String ownerName;
    private String ownerDept;
    private String ownerId;
    private int contributorsCount;
    private boolean isOpenForContribution;
    private String githubUrl;
    private String paperDoi;
    private long createdAt;

    public ResearchPaperEntity(long id, String title, String type, String description,
                               String technologies, String ownerName, String ownerDept,
                               String ownerId, int contributorsCount, boolean isOpenForContribution,
                               String githubUrl, String paperDoi, long createdAt) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.description = description;
        this.technologies = technologies;
        this.ownerName = ownerName;
        this.ownerDept = ownerDept;
        this.ownerId = ownerId;
        this.contributorsCount = contributorsCount;
        this.isOpenForContribution = isOpenForContribution;
        this.githubUrl = githubUrl;
        this.paperDoi = paperDoi;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerDept() { return ownerDept; }
    public void setOwnerDept(String ownerDept) { this.ownerDept = ownerDept; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public int getContributorsCount() { return contributorsCount; }
    public void setContributorsCount(int contributorsCount) { this.contributorsCount = contributorsCount; }

    public boolean isOpenForContribution() { return isOpenForContribution; }
    public void setIsOpenForContribution(boolean openForContribution) { isOpenForContribution = openForContribution; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getPaperDoi() { return paperDoi; }
    public void setPaperDoi(String paperDoi) { this.paperDoi = paperDoi; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
