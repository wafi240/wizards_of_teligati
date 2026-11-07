package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class StudentDirectoryResponse {
    @SerializedName("userId")
    private Long userId;

    @SerializedName("roll")
    private String roll;

    @SerializedName("name")
    private String name;

    @SerializedName("department")
    private String department;

    @SerializedName("batch")
    private String batch;

    @SerializedName("skills")
    private String skills;

    @SerializedName("projects")
    private String projects;

    @SerializedName("specialization")
    private String specialization;

    @SerializedName("clubs")
    private String clubs;

    @SerializedName("githubUrl")
    private String githubUrl;

    @SerializedName("linkedinUrl")
    private String linkedinUrl;

    @SerializedName("bio")
    private String bio;

    @SerializedName("isVerified")
    private boolean isVerified;

    public StudentDirectoryResponse() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRoll() { return roll; }
    public void setRoll(String roll) { this.roll = roll; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getClubs() { return clubs; }
    public void setClubs(String clubs) { this.clubs = clubs; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}
