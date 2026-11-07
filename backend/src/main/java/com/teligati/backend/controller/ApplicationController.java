package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.ApplicationRequest;
import com.teligati.backend.dto.ApplicationResponse;
import com.teligati.backend.dto.ApplicationStatusUpdateRequest;
import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(
            @Valid @RequestBody ApplicationRequest request,
            Principal principal
    ) {
        ApplicationResponse response = applicationService.submitApplication(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Application submitted successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getMyApplications(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ApplicationResponse> response = applicationService.getMyApplications(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "My applications retrieved successfully"));
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getReceivedApplications(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ApplicationResponse> response = applicationService.getReceivedApplications(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Received applications retrieved successfully"));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getApplicationsForJob(
            @PathVariable("jobId") Long jobId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ApplicationResponse> response = applicationService.getApplicationsForJob(jobId, principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Applications for job retrieved successfully"));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getApplicationsForProject(
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ApplicationResponse> response = applicationService.getApplicationsForProject(projectId, principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Applications for research project retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ApplicationResponse response = applicationService.getApplicationById(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Application details retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request,
            Principal principal
    ) {
        ApplicationResponse response = applicationService.updateApplicationStatus(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application status updated successfully"));
    }
}
