package com.teligati.backend.controller;

import com.teligati.backend.entity.Job;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.Project;
import com.teligati.backend.repository.JobRepository;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
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
