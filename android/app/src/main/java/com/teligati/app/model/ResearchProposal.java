package com.teligati.app.model;

public class ResearchProposal {
    private long id;
    private String studentName;
    private String studentRoll;
    private String topic;
    private String proposalAbstract;
    private String degreeType; // "PhD Proposal", "Undergrad Research"
    private String status;     // "Pending", "Accepted"

    public ResearchProposal(long id, String studentName, String studentRoll, String topic, String proposalAbstract, String degreeType, String status) {
        this.id = id;
        this.studentName = studentName;
        this.studentRoll = studentRoll;
        this.topic = topic;
        this.proposalAbstract = proposalAbstract;
        this.degreeType = degreeType;
        this.status = status;
    }

    public long getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getStudentRoll() { return studentRoll; }
    public String getTopic() { return topic; }
    public String getProposalAbstract() { return proposalAbstract; }
    public String getDegreeType() { return degreeType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
