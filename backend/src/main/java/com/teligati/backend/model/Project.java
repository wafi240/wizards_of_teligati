package com.teligati.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String technologies;
    private String githubUrl;
    private boolean isOpenForContribution;

    @ManyToOne
    private User owner;
}
