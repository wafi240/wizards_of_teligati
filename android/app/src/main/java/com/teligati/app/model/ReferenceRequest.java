package com.teligati.app.model;

public class ReferenceRequest {
    private long id;
    private String studentName;
    private String studentRoll;
    private String studentDept;
    private String targetCompany;
    private String message;
    private String status; // "PENDING", "ACCEPTED", "DECLINED", "COMPLETED", "CANCELLED", "Endorsed"

    private String recipientRole;
    private String recipientName;
    private String purpose;
    private String targetProgram;
    private String deadline;
    private String recommendationText;
    private String responseNotes;

    public ReferenceRequest(long id, String studentName, String studentRoll, String studentDept,
                            String targetCompany, String message, String status) {
        this.id = id;
        this.studentName = studentName;
        this.studentRoll = studentRoll;
        this.studentDept = studentDept;
        this.targetCompany = targetCompany;
        this.message = message;
        this.status = status;
    }

    public ReferenceRequest(long id, String studentName, String studentRoll, String studentDept,
                            String targetCompany, String message, String status,
                            String recipientRole, String recipientName, String purpose,
                            String targetProgram, String deadline,
                            String recommendationText, String responseNotes) {
        this(id, studentName, studentRoll, studentDept, targetCompany, message, status);
        this.recipientRole = recipientRole;
        this.recipientName = recipientName;
        this.purpose = purpose;
        this.targetProgram = targetProgram;
        this.deadline = deadline;
        this.recommendationText = recommendationText;
        this.responseNotes = responseNotes;
    }

    public long getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getStudentRoll() { return studentRoll; }
    public String getStudentDept() { return studentDept; }
    public String getTargetCompany() { return targetCompany; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getTargetProgram() { return targetProgram; }
    public void setTargetProgram(String targetProgram) { this.targetProgram = targetProgram; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }

    public String getResponseNotes() { return responseNotes; }
    public void setResponseNotes(String responseNotes) { this.responseNotes = responseNotes; }
}
