package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.JobRequest;
import com.teligati.backend.dto.JobResponse;
import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JobResponse>>> getJobs(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false, defaultValue = "OPEN") String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        PagedResponse<JobResponse> response = jobService.getJobs(category, status, search, principalName, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Jobs retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        JobResponse response = jobService.getJobById(id, principalName);
        return ResponseEntity.ok(ApiResponse.success(response, "Job details retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobRequest request,
            Principal principal
    ) {
        JobResponse response = jobService.createJob(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Job posting created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable("id") Long id,
            @Valid @RequestBody JobRequest request,
            Principal principal
    ) {
        JobResponse response = jobService.updateJob(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Job posting updated successfully"));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<JobResponse>> closeJob(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        JobResponse response = jobService.closeJob(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Job posting closed successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        jobService.deleteJob(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Job posting deleted successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<JobResponse>>> getMyJobs(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<JobResponse> response = jobService.getMyJobs(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "User postings retrieved successfully"));
    }
}
