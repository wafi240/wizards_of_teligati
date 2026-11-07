package com.teligati.app.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "research_proposals")
public class ResearchProposalEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String studentRoll;
    private String studentName;
    private String topic;
    private String proposalAbstract;
    private String degreeType; // "PhD Proposal", "Undergrad Thesis", "Master Thesis"
    private String status;     // "Pending", "Accepted", "Rejected", "Revision Requested"
    private String teacherId;
    private String feedback;
    private long submittedAt;

    public ResearchProposalEntity(long id, String studentRoll, String studentName, String topic,
                                  String proposalAbstract, String degreeType, String status,
                                  String teacherId, String feedback, long submittedAt) {
        this.id = id;
        this.studentRoll = studentRoll;
        this.studentName = studentName;
        this.topic = topic;
        this.proposalAbstract = proposalAbstract;
        this.degreeType = degreeType;
        this.status = status;
        this.teacherId = teacherId;
        this.feedback = feedback;
        this.submittedAt = submittedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getProposalAbstract() { return proposalAbstract; }
    public void setProposalAbstract(String proposalAbstract) { this.proposalAbstract = proposalAbstract; }

    public String getDegreeType() { return degreeType; }
    public void setDegreeType(String degreeType) { this.degreeType = degreeType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }
}
