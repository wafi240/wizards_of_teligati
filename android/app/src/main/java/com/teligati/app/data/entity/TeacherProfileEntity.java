package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "teacher_profiles")
public class TeacherProfileEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String userId;
    private String name;
    private String designation;
    private String department;
    private String researchFocus;
    private String labName;
    private int publicationsCount;
    private String contactEmail;
    private String bio;

    public TeacherProfileEntity(long id, String userId, String name, String designation,
                                String department, String researchFocus, String labName,
                                int publicationsCount, String contactEmail, String bio) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.designation = designation;
        this.department = department;
        this.researchFocus = researchFocus;
        this.labName = labName;
        this.publicationsCount = publicationsCount;
        this.contactEmail = contactEmail;
        this.bio = bio;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getResearchFocus() { return researchFocus; }
    public void setResearchFocus(String researchFocus) { this.researchFocus = researchFocus; }

    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }

    public int getPublicationsCount() { return publicationsCount; }
    public void setPublicationsCount(int publicationsCount) { this.publicationsCount = publicationsCount; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
