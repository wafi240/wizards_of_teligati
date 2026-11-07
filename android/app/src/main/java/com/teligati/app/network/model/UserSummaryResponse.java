package com.teligati.app.network.model;

import com.google.gson.annotations.SerializedName;

public class UserSummaryResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("kuetId")
    private String kuetId;

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private String name;

    @SerializedName("role")
    private String role;

    @SerializedName("department")
    private String department;

    @SerializedName(value = "verified", alternate = {"isVerified"})
    private boolean isVerified;

    public UserSummaryResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKuetId() { return kuetId; }
    public void setKuetId(String kuetId) { this.kuetId = kuetId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}
