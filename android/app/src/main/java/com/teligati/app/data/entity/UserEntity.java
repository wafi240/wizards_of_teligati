package com.teligati.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String id; // e.g. "2307003", "tanvir.munich@kuet.ac.bd", "azharul@cse.kuet.ac.bd"
    private String role; // "STUDENT", "ALUMNI", "TEACHER" / "FACULTY"
    private String name;
    private String email;
    private String department;
    private String rollOrBatch;
    private String designationOrCompany;
    private boolean isVerified;
    private double availableBalance;
    private double escrowBalance;

    public UserEntity(@NonNull String id, String role, String name, String email,
                      String department, String rollOrBatch, String designationOrCompany,
                      boolean isVerified, double availableBalance, double escrowBalance) {
        this.id = id;
        this.role = role;
        this.name = name;
        this.email = email;
        this.department = department;
        this.rollOrBatch = rollOrBatch;
        this.designationOrCompany = designationOrCompany;
        this.isVerified = isVerified;
        this.availableBalance = availableBalance;
        this.escrowBalance = escrowBalance;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getRollOrBatch() { return rollOrBatch; }
    public void setRollOrBatch(String rollOrBatch) { this.rollOrBatch = rollOrBatch; }

    public String getDesignationOrCompany() { return designationOrCompany; }
    public void setDesignationOrCompany(String designationOrCompany) { this.designationOrCompany = designationOrCompany; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public double getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }

    public double getEscrowBalance() { return escrowBalance; }
    public void setEscrowBalance(double escrowBalance) { this.escrowBalance = escrowBalance; }
}
