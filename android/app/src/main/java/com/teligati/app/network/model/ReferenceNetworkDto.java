package com.teligati.app.network.model;

public class ReferenceNetworkDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentRoll;
    private String studentDept;
    private String studentEmail;
    private Double studentCgpa;
    private String studentSkills;
    private Long recipientId;
    private String recipientName;
    private String recipientRole;
    private String recipientDept;
    private String recipientEmail;
    private String purpose;
    private String targetInstitution;
    private String targetProgram;
    private String deadline;
    private String personalMessage;
    private String supportingInfo;
    private String status;
    private String recommendationText;
    private String responseNotes;
    private String createdAt;
    private String updatedAt;

    public ReferenceNetworkDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }

    public String getStudentDept() { return studentDept; }
    public void setStudentDept(String studentDept) { this.studentDept = studentDept; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Double getStudentCgpa() { return studentCgpa; }
    public void setStudentCgpa(Double studentCgpa) { this.studentCgpa = studentCgpa; }

    public String getStudentSkills() { return studentSkills; }
    public void setStudentSkills(String studentSkills) { this.studentSkills = studentSkills; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

    public String getRecipientDept() { return recipientDept; }
    public void setRecipientDept(String recipientDept) { this.recipientDept = recipientDept; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getTargetInstitution() { return targetInstitution; }
    public void setTargetInstitution(String targetInstitution) { this.targetInstitution = targetInstitution; }

    public String getTargetProgram() { return targetProgram; }
    public void setTargetProgram(String targetProgram) { this.targetProgram = targetProgram; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getPersonalMessage() { return personalMessage; }
    public void setPersonalMessage(String personalMessage) { this.personalMessage = personalMessage; }

    public String getSupportingInfo() { return supportingInfo; }
    public void setSupportingInfo(String supportingInfo) { this.supportingInfo = supportingInfo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }

    public String getResponseNotes() { return responseNotes; }
    public void setResponseNotes(String responseNotes) { this.responseNotes = responseNotes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
