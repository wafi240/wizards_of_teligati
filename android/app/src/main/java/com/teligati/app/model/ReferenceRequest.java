package com.teligati.app.model;

public class ReferenceRequest {
    private long id;
    private String studentName;
    private String studentRoll;
    private String studentDept;
    private String targetCompany;
    private String message;
    private String status; // "Pending", "Endorsed"

    public ReferenceRequest(long id, String studentName, String studentRoll, String studentDept, String targetCompany, String message, String status) {
        this.id = id;
        this.studentName = studentName;
        this.studentRoll = studentRoll;
        this.studentDept = studentDept;
        this.targetCompany = targetCompany;
        this.message = message;
        this.status = status;
    }

    public long getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getStudentRoll() { return studentRoll; }
    public String getStudentDept() { return studentDept; }
    public String getTargetCompany() { return targetCompany; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
