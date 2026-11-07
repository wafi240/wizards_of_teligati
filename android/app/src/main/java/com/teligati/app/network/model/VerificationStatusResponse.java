package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class VerificationStatusResponse {
    @SerializedName(value = "verified", alternate = {"isVerified"})
    private boolean isVerified;

    @SerializedName("identifier")
    private String identifier;

    @SerializedName("role")
    private String role;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("restrictedActions")
    private List<String> restrictedActions;

    public VerificationStatusResponse() {}

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getRestrictedActions() { return restrictedActions; }
    public void setRestrictedActions(List<String> restrictedActions) { this.restrictedActions = restrictedActions; }
}
