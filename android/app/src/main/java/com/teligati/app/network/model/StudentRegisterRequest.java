package com.teligati.app.network.model;

public class StudentRegisterRequest {
    private String roll;
    private String name;
    private String email;
    private String department;
    private String password;

    public StudentRegisterRequest() {}

    public StudentRegisterRequest(String roll, String name, String email, String department, String password) {
        this.roll = roll;
        this.name = name;
        this.email = email;
        this.department = department;
        this.password = password;
    }

    public String getRoll() { return roll; }
    public void setRoll(String roll) { this.roll = roll; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
