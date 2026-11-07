package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "job_applications")
public class JobApplicationEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long jobId;
    private String jobTitle;
    private String applicantId; // student roll
    private String applicantName;
    private String applicantRoll;
    private String applicantDept;
    private double applicantCgpa;
    private String applicantSkills;
    private String statement;
    private String status; // "Applicant", "Shortlisted", "Accepted", "Rejected"
    private long appliedAt;

    public JobApplicationEntity(long id, long jobId, String jobTitle, String applicantId,
                                String applicantName, String applicantRoll, String applicantDept,
                                double applicantCgpa, String applicantSkills, String statement,
                                String status, long appliedAt) {
        this.id = id;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.applicantRoll = applicantRoll;
        this.applicantDept = applicantDept;
        this.applicantCgpa = applicantCgpa;
        this.applicantSkills = applicantSkills;
        this.statement = statement;
        this.status = status;
        this.appliedAt = appliedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getJobId() { return jobId; }
    public void setJobId(long jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantRoll() { return applicantRoll; }
    public void setApplicantRoll(String applicantRoll) { this.applicantRoll = applicantRoll; }

    public String getApplicantDept() { return applicantDept; }
    public void setApplicantDept(String applicantDept) { this.applicantDept = applicantDept; }

    public double getApplicantCgpa() { return applicantCgpa; }
    public void setApplicantCgpa(double applicantCgpa) { this.applicantCgpa = applicantCgpa; }

    public String getApplicantSkills() { return applicantSkills; }
    public void setApplicantSkills(String applicantSkills) { this.applicantSkills = applicantSkills; }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getAppliedAt() { return appliedAt; }
    public void setAppliedAt(long appliedAt) { this.appliedAt = appliedAt; }
}
