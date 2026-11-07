package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "research_applications")
public class ResearchApplicationEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long opportunityId;
    private String opportunityTitle;
    private String studentRoll;
    private String studentName;
    private String department;
    private double cgpa;
    private String skills;
    private String labInterest;
    private String status; // "Applicant", "Accepted", "Rejected"
    private long appliedAt;

    public ResearchApplicationEntity(long id, long opportunityId, String opportunityTitle,
                                     String studentRoll, String studentName, String department,
                                     double cgpa, String skills, String labInterest,
                                     String status, long appliedAt) {
        this.id = id;
        this.opportunityId = opportunityId;
        this.opportunityTitle = opportunityTitle;
        this.studentRoll = studentRoll;
        this.studentName = studentName;
        this.department = department;
        this.cgpa = cgpa;
        this.skills = skills;
        this.labInterest = labInterest;
        this.status = status;
        this.appliedAt = appliedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(long opportunityId) { this.opportunityId = opportunityId; }

    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }

    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getLabInterest() { return labInterest; }
    public void setLabInterest(String labInterest) { this.labInterest = labInterest; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getAppliedAt() { return appliedAt; }
    public void setAppliedAt(long appliedAt) { this.appliedAt = appliedAt; }
}
