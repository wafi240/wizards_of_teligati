package com.teligati.app.model;

public class StudentApplicant {
    private long id;
    private String name;
    private String roll;
    private String department;
    private double cgpa;
    private String skills;
    private String labInterest;
    private String status; // "Applicant", "Accepted"

    public StudentApplicant(long id, String name, String roll, String department, double cgpa, String skills, String labInterest, String status) {
        this.id = id;
        this.name = name;
        this.roll = roll;
        this.department = department;
        this.cgpa = cgpa;
        this.skills = skills;
        this.labInterest = labInterest;
        this.status = status;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getRoll() { return roll; }
    public String getDepartment() { return department; }
    public double getCgpa() { return cgpa; }
    public String getSkills() { return skills; }
    public String getLabInterest() { return labInterest; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
