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
@RequestMapping("/api/v1/assistant-jobs")
public class AssistantJobController {

    private final JobService jobService;

    public AssistantJobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JobResponse>>> getAssistantJobs(
            @RequestParam(name = "status", required = false, defaultValue = "OPEN") String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        PagedResponse<JobResponse> response = jobService.getJobs("ASSISTANT_WORK", status, search, principalName, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Assistant jobs retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getAssistantJobById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        JobResponse response = jobService.getJobById(id, principalName);
        return ResponseEntity.ok(ApiResponse.success(response, "Assistant job details retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createAssistantJob(
            @Valid @RequestBody JobRequest request,
            Principal principal
    ) {
        request.setCategory("ASSISTANT_WORK");
        JobResponse response = jobService.createJob(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Assistant job created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> updateAssistantJob(
            @PathVariable("id") Long id,
            @Valid @RequestBody JobRequest request,
            Principal principal
    ) {
        JobResponse response = jobService.updateJob(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Assistant job updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAssistantJob(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        jobService.deleteJob(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Assistant job deleted successfully"));
    }
}
