package com.teligati.app.network.model;

public class AlumniRegisterRequest {
    private String name;
    private String email;
    private String department;
    private String password;
    private String invitationToken;

    public AlumniRegisterRequest() {}

    public AlumniRegisterRequest(String name, String email, String department, String password, String invitationToken) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.password = password;
        this.invitationToken = invitationToken;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getInvitationToken() { return invitationToken; }
    public void setInvitationToken(String invitationToken) { this.invitationToken = invitationToken; }
}
