package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reference_requests")
public class ReferenceRequestEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String studentRoll;
    private String studentName;
    private String studentDept;
    private String targetCompany;
    private String message;
    private String status; // "PENDING", "ACCEPTED", "DECLINED", "COMPLETED", "CANCELLED", "Endorsed"
    private String alumniId;
    private long requestedAt;

    private String recipientRole;
    private String recipientName;
    private String purpose;
    private String targetProgram;
    private String deadline;
    private String recommendationText;
    private String responseNotes;

    public ReferenceRequestEntity(long id, String studentRoll, String studentName, String studentDept,
                                  String targetCompany, String message, String status,
                                  String alumniId, long requestedAt,
                                  String recipientRole, String recipientName, String purpose,
                                  String targetProgram, String deadline,
                                  String recommendationText, String responseNotes) {
        this.id = id;
        this.studentRoll = studentRoll;
        this.studentName = studentName;
        this.studentDept = studentDept;
        this.targetCompany = targetCompany;
        this.message = message;
        this.status = status;
        this.alumniId = alumniId;
        this.requestedAt = requestedAt;
        this.recipientRole = recipientRole;
        this.recipientName = recipientName;
        this.purpose = purpose;
        this.targetProgram = targetProgram;
        this.deadline = deadline;
        this.recommendationText = recommendationText;
        this.responseNotes = responseNotes;
    }

    @Ignore
    public ReferenceRequestEntity(long id, String studentRoll, String studentName, String studentDept,
                                  String targetCompany, String message, String status,
                                  String alumniId, long requestedAt) {
        this(id, studentRoll, studentName, studentDept, targetCompany, message, status,
             alumniId, requestedAt, "ROLE_ALUMNI", alumniId, "JOB_REFERRAL", "", "", null, null);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentDept() { return studentDept; }
    public void setStudentDept(String studentDept) { this.studentDept = studentDept; }

    public String getTargetCompany() { return targetCompany; }
    public void setTargetCompany(String targetCompany) { this.targetCompany = targetCompany; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAlumniId() { return alumniId; }
    public void setAlumniId(String alumniId) { this.alumniId = alumniId; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }

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
