package com.teligati.app.model;

import java.util.List;

public class ProjectItem {
    private long id;
    private String name;
    private String type;
    private String description;
    private List<String> technologies;
    private String ownerName;
    private String ownerDept;
    private int contributorsCount;
    private boolean isOpenForContribution;
    private String githubUrl;
    private String paperDoi;

    public ProjectItem(long id, String name, String type, String description, List<String> technologies,
                       String ownerName, String ownerDept, int contributorsCount, boolean isOpenForContribution,
                       String githubUrl, String paperDoi) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.technologies = technologies;
        this.ownerName = ownerName;
        this.ownerDept = ownerDept;
        this.contributorsCount = contributorsCount;
        this.isOpenForContribution = isOpenForContribution;
        this.githubUrl = githubUrl;
        this.paperDoi = paperDoi;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public List<String> getTechnologies() { return technologies; }
    public String getOwnerName() { return ownerName; }
    public String getOwnerDept() { return ownerDept; }
    public int getContributorsCount() { return contributorsCount; }
    public boolean isOpenForContribution() { return isOpenForContribution; }
    public String getGithubUrl() { return githubUrl; }
    public String getPaperDoi() { return paperDoi; }
}
