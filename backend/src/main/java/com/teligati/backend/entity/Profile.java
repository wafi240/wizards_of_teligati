package com.teligati.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profiles")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String roll;
    private String department;
    private String batch;

    @Column(name = "graduation_year")
    private String graduationYear;

    private String status;

    @Column(name = "current_company")
    private String currentCompany;

    @Column(name = "current_position")
    private String currentPosition;

    private String country;
    private String city;

    @Column(name = "is_available_for_mentorship")
    private boolean isAvailableForMentorship;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String projects;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    private Double cgpa;

    private String specialization;
    private String clubs;
    private String designation;

    @Column(name = "research_focus")
    private String researchFocus;

    @Column(name = "lab_name")
    private String labName;

    @Builder.Default
    @Column(name = "publications_count")
    private Integer publicationsCount = 0;

    @Builder.Default
    @Column(name = "is_actively_hiring")
    private boolean isActivelyHiring = false;

    @Column(name = "contact_email")
    private String contactEmail;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
