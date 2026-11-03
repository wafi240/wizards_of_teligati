package com.teligati.backend.controller;

import com.teligati.backend.model.*;
import com.teligati.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ApiController {

    @Autowired private ProfileRepository profileRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ProjectRepository projectRepository;

    @GetMapping("/profiles")
    public List<Profile> getProfiles() {
        return profileRepository.findAll();
    }

    @GetMapping("/jobs")
    public List<Job> getJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/projects")
    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    @PostMapping("/ai/match")
    public String matchOpportunity() {
        return "{\"match_score\": 89, \"role\": \"Android Developer Assistant\", \"reason\": \"Matches your Kotlin and Compose profile skills.\"}";
    }
}
