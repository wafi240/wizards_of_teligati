package com.teligati.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "student_profiles")
public class StudentProfileEntity {
    @PrimaryKey
    @NonNull
    private String roll; // e.g. "2307003"
    private String name;
    private String department;
    private String batch;
    private double cgpa;
    private String skills;
    private String projects;
    private String githubUrl;
    private String linkedinUrl;
    private String specialization;
    private String clubs;
    private String bio;

    public StudentProfileEntity(@NonNull String roll, String name, String department, String batch,
                                double cgpa, String skills, String projects, String githubUrl,
                                String linkedinUrl, String specialization, String clubs, String bio) {
        this.roll = roll;
        this.name = name;
        this.department = department;
        this.batch = batch;
        this.cgpa = cgpa;
        this.skills = skills;
        this.projects = projects;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.specialization = specialization;
        this.clubs = clubs;
        this.bio = bio;
    }

    @NonNull
    public String getRoll() { return roll; }
    public void setRoll(@NonNull String roll) { this.roll = roll; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getClubs() { return clubs; }
    public void setClubs(String clubs) { this.clubs = clubs; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
