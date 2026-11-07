package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class TeacherDirectoryResponse {
    @SerializedName("uid")
    private String uid;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("name")
    private String name;

    @SerializedName("department")
    private String department;

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

    @SerializedName("bio")
    private String bio;

    @SerializedName("isVerified")
    private boolean isVerified;

    public TeacherDirectoryResponse() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

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

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}
