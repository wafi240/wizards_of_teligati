package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class AlumniDirectoryResponse {
    @SerializedName("userId")
    private Long userId;

    @SerializedName("name")
    private String name;

    @SerializedName("department")
    private String department;

    @SerializedName("batch")
    private String batch;

    @SerializedName("graduationYear")
    private String graduationYear;

    @SerializedName("currentCompany")
    private String currentCompany;

    @SerializedName("currentPosition")
    private String currentPosition;

    @SerializedName("country")
    private String country;

    @SerializedName("city")
    private String city;

    @SerializedName("skills")
    private String skills;

    @SerializedName("linkedinUrl")
    private String linkedinUrl;

    @SerializedName("bio")
    private String bio;

    @SerializedName("isAvailableForMentorship")
    private boolean isAvailableForMentorship;

    @SerializedName("isActivelyHiring")
    private boolean isActivelyHiring;

    @SerializedName("isVerified")
    private boolean isVerified;

    public AlumniDirectoryResponse() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getGraduationYear() { return graduationYear; }
    public void setGraduationYear(String graduationYear) { this.graduationYear = graduationYear; }

    public String getCurrentCompany() { return currentCompany; }
    public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany; }

    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isAvailableForMentorship() { return isAvailableForMentorship; }
    public void setAvailableForMentorship(boolean availableForMentorship) { isAvailableForMentorship = availableForMentorship; }

    public boolean isActivelyHiring() { return isActivelyHiring; }
    public void setActivelyHiring(boolean activelyHiring) { isActivelyHiring = activelyHiring; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}
