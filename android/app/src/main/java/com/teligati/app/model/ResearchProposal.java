package com.teligati.app.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResearchProposal {
    private long id;
    private String studentName;
    private String studentRoll;
    private String topic;
    private String proposalAbstract;
    private String degreeType; // "PhD Proposal", "Undergrad Research", "Edge AI Research"
    private String status;     // "Pending", "Accepted", "Rejected", "Revision Requested"
    private String teacherId;
    private String facultyName;
    private String feedback;
    private long submittedAt;

    public ResearchProposal(long id, String studentName, String studentRoll, String topic, String proposalAbstract, String degreeType, String status) {
        this(id, studentName, studentRoll, topic, proposalAbstract, degreeType, status, "", "", "", System.currentTimeMillis());
    }

    public ResearchProposal(long id, String studentName, String studentRoll, String topic,
                            String proposalAbstract, String degreeType, String status,
                            String teacherId, String facultyName, String feedback, long submittedAt) {
        this.id = id;
        this.studentName = studentName;
        this.studentRoll = studentRoll;
        this.topic = topic;
        this.proposalAbstract = proposalAbstract;
        this.degreeType = degreeType;
        this.status = status;
        this.teacherId = teacherId;
        this.facultyName = facultyName;
        this.feedback = feedback;
        this.submittedAt = submittedAt > 0 ? submittedAt : System.currentTimeMillis();
    }

    public long getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getStudentRoll() { return studentRoll; }
    public String getTopic() { return topic; }
    public String getProposalAbstract() { return proposalAbstract; }
    public String getDegreeType() { return degreeType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }

    public String getFormattedDate() {
        if (submittedAt <= 0) return "Recent";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(submittedAt));
        } catch (Exception e) {
            return "Recent";
        }
    }
}
