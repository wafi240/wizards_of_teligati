package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alumni_profiles")
public class AlumniProfileEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String userId;
    private String name;
    private String department;
    private String batch;
    private String currentCompany;
    private String currentPosition;
    private String country;
    private String city;
    private boolean isVerified;
    private boolean isAvailableForMentorship;
    private boolean isActivelyHiring;
    private String skills;
    private String bio;

    public AlumniProfileEntity(long id, String userId, String name, String department, String batch,
                               String currentCompany, String currentPosition, String country,
                               String city, boolean isVerified, boolean isAvailableForMentorship,
                               boolean isActivelyHiring, String skills, String bio) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.department = department;
        this.batch = batch;
        this.currentCompany = currentCompany;
        this.currentPosition = currentPosition;
        this.country = country;
        this.city = city;
        this.isVerified = isVerified;
        this.isAvailableForMentorship = isAvailableForMentorship;
        this.isActivelyHiring = isActivelyHiring;
        this.skills = skills;
        this.bio = bio;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getCurrentCompany() { return currentCompany; }
    public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany; }

    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isAvailableForMentorship() { return isAvailableForMentorship; }
    public void setAvailableForMentorship(boolean availableForMentorship) { isAvailableForMentorship = availableForMentorship; }

    public boolean isActivelyHiring() { return isActivelyHiring; }
    public void setActivelyHiring(boolean activelyHiring) { isActivelyHiring = activelyHiring; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
