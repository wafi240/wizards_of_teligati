package com.teligati.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String roll;
    private String department;
    private String batch;
    private String graduationYear;
    private String status;
    private String currentCompany;
    private String currentPosition;
    private String country;
    private String city;
    private boolean isAvailableForMentorship;

    @OneToOne
    private User user;
}
