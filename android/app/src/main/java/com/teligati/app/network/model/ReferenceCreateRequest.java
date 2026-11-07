package com.teligati.app.network.model;

public class ReferenceCreateRequest {
    private Long recipientId;
    private String recipientUid;
    private String purpose;
    private String targetInstitution;
    private String targetProgram;
    private String deadline;
    private String personalMessage;
    private String supportingInfo;

    public ReferenceCreateRequest() {}

    public ReferenceCreateRequest(Long recipientId, String purpose, String targetInstitution,
                                  String targetProgram, String deadline, String personalMessage,
                                  String supportingInfo) {
        this.recipientId = recipientId;
        this.recipientUid = recipientId != null ? String.valueOf(recipientId) : null;
        this.purpose = purpose;
        this.targetInstitution = targetInstitution;
        this.targetProgram = targetProgram;
        this.deadline = deadline;
        this.personalMessage = personalMessage;
        this.supportingInfo = supportingInfo;
    }

    public ReferenceCreateRequest(String recipientUid, String purpose, String targetInstitution,
                                  String targetProgram, String deadline, String personalMessage,
                                  String supportingInfo) {
        this.recipientUid = recipientUid;
        try {
            this.recipientId = Long.parseLong(recipientUid);
        } catch (Exception ignored) {
            this.recipientId = 0L;
        }
        this.purpose = purpose;
        this.targetInstitution = targetInstitution;
        this.targetProgram = targetProgram;
        this.deadline = deadline;
        this.personalMessage = personalMessage;
        this.supportingInfo = supportingInfo;
    }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getRecipientUid() { return recipientUid; }
    public void setRecipientUid(String recipientUid) { this.recipientUid = recipientUid; }

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
}
