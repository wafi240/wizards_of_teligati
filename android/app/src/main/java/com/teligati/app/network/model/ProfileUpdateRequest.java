package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class ProfileUpdateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("department")
    private String department;

    @SerializedName("batch")
    private String batch;

    @SerializedName("graduationYear")
    private String graduationYear;

    @SerializedName("status")
    private String status;

    @SerializedName("currentCompany")
    private String currentCompany;

    @SerializedName("currentPosition")
    private String currentPosition;

    @SerializedName("country")
    private String country;

    @SerializedName("city")
    private String city;

    @SerializedName("isAvailableForMentorship")
    private Boolean isAvailableForMentorship;

    @SerializedName("bio")
    private String bio;

    @SerializedName("skills")
    private String skills;

    @SerializedName("projects")
    private String projects;

    @SerializedName("githubUrl")
    private String githubUrl;

    @SerializedName("linkedinUrl")
    private String linkedinUrl;

    @SerializedName("cgpa")
    private Double cgpa;

    @SerializedName("specialization")
    private String specialization;

    @SerializedName("clubs")
    private String clubs;

    @SerializedName("designation")
    private String designation;

    @SerializedName("researchFocus")
    private String researchFocus;

    @SerializedName("labName")
    private String labName;

    @SerializedName("publicationsCount")
    private Integer publicationsCount;

    @SerializedName("contactEmail")
    private String contactEmail;

    @SerializedName("isActivelyHiring")
    private Boolean isActivelyHiring;

    public ProfileUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getGraduationYear() { return graduationYear; }
    public void setGraduationYear(String graduationYear) { this.graduationYear = graduationYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentCompany() { return currentCompany; }
    public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany; }

    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Boolean getIsAvailableForMentorship() { return isAvailableForMentorship; }
    public void setIsAvailableForMentorship(Boolean availableForMentorship) { isAvailableForMentorship = availableForMentorship; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getClubs() { return clubs; }
    public void setClubs(String clubs) { this.clubs = clubs; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getResearchFocus() { return researchFocus; }
    public void setResearchFocus(String researchFocus) { this.researchFocus = researchFocus; }

    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }

    public Integer getPublicationsCount() { return publicationsCount; }
    public void setPublicationsCount(Integer publicationsCount) { this.publicationsCount = publicationsCount; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public Boolean getIsActivelyHiring() { return isActivelyHiring; }
    public void setIsActivelyHiring(Boolean activelyHiring) { isActivelyHiring = activelyHiring; }
}
