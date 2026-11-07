package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.*;
import com.teligati.backend.service.ResearchAcademicService;
import com.teligati.backend.service.ResearchPaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchAcademicService researchAcademicService;
    private final ResearchPaperService researchPaperService;

    // =========================================================================
    // 1. Research Opportunities
    // =========================================================================

    @GetMapping("/opportunities")
    public ResponseEntity<ApiResponse<PagedResponse<ResearchOpportunityResponse>>> getOpportunities(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false, defaultValue = "OPEN") String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        PagedResponse<ResearchOpportunityResponse> response = researchAcademicService.getOpportunities(category, status, search, principalName, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Research opportunities retrieved successfully"));
    }

    @GetMapping("/opportunities/{id}")
    public ResponseEntity<ApiResponse<ResearchOpportunityResponse>> getOpportunityById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        ResearchOpportunityResponse response = researchAcademicService.getOpportunityById(id, principalName);
        return ResponseEntity.ok(ApiResponse.success(response, "Research opportunity details retrieved successfully"));
    }

    @PostMapping("/opportunities")
    public ResponseEntity<ApiResponse<ResearchOpportunityResponse>> createOpportunity(
            @Valid @RequestBody ResearchOpportunityRequest request,
            Principal principal
    ) {
        ResearchOpportunityResponse response = researchAcademicService.createOpportunity(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Research opportunity created successfully"));
    }

    @PutMapping("/opportunities/{id}")
    public ResponseEntity<ApiResponse<ResearchOpportunityResponse>> updateOpportunity(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResearchOpportunityRequest request,
            Principal principal
    ) {
        ResearchOpportunityResponse response = researchAcademicService.updateOpportunity(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Research opportunity updated successfully"));
    }

    @DeleteMapping("/opportunities/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOpportunity(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        researchAcademicService.deleteOpportunity(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Research opportunity deleted successfully"));
    }

    @PatchMapping("/opportunities/{id}/close")
    public ResponseEntity<ApiResponse<ResearchOpportunityResponse>> closeOpportunity(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ResearchOpportunityResponse response = researchAcademicService.closeOpportunity(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Research opportunity closed successfully"));
    }

    // =========================================================================
    // 2. Research Applications
    // =========================================================================

    @PostMapping("/opportunities/{id}/applications")
    public ResponseEntity<ApiResponse<ResearchApplicationResponse>> submitApplication(
            @PathVariable("id") Long opportunityId,
            @Valid @RequestBody ResearchApplicationSubmitRequest request,
            Principal principal
    ) {
        ResearchApplicationResponse response = researchAcademicService.submitApplication(opportunityId, principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Research application submitted successfully"));
    }

    @GetMapping("/applications/me")
    public ResponseEntity<ApiResponse<PagedResponse<ResearchApplicationResponse>>> getMyApplications(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ResearchApplicationResponse> response = researchAcademicService.getMyApplications(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "My research applications retrieved successfully"));
    }

    @GetMapping("/opportunities/{id}/applications")
    public ResponseEntity<ApiResponse<PagedResponse<ResearchApplicationResponse>>> getApplicationsForOpportunity(
            @PathVariable("id") Long opportunityId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ResearchApplicationResponse> response = researchAcademicService.getApplicationsForOpportunity(opportunityId, principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Applications for research opportunity retrieved successfully"));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<ResearchApplicationResponse>> getApplicationById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ResearchApplicationResponse response = researchAcademicService.getApplicationById(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Application details retrieved successfully"));
    }

    @PatchMapping("/applications/{id}/review")
    public ResponseEntity<ApiResponse<ResearchApplicationResponse>> reviewApplication(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResearchApplicationReviewRequest request,
            Principal principal
    ) {
        ResearchApplicationResponse response = researchAcademicService.reviewApplication(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application reviewed successfully"));
    }

    @PatchMapping("/applications/{id}/withdraw")
    public ResponseEntity<ApiResponse<ResearchApplicationResponse>> withdrawApplication(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ResearchApplicationResponse response = researchAcademicService.withdrawApplication(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Application withdrawn successfully"));
    }

    @PatchMapping("/applications/{id}/resubmit")
    public ResponseEntity<ApiResponse<ResearchApplicationResponse>> resubmitApplication(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResearchApplicationResubmitRequest request,
            Principal principal
    ) {
        ResearchApplicationResponse response = researchAcademicService.resubmitApplication(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application resubmitted successfully"));
    }

    // =========================================================================
    // 3. Research Papers
    // =========================================================================

    @GetMapping("/papers")
    public ResponseEntity<ApiResponse<PagedResponse<ResearchPaperResponse>>> getPapers(
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        PagedResponse<ResearchPaperResponse> response = researchPaperService.getPapers(field, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Research papers retrieved successfully"));
    }

    @GetMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<ResearchPaperResponse>> getPaperById(
            @PathVariable("id") Long id
    ) {
        ResearchPaperResponse response = researchPaperService.getPaperById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Research paper details retrieved successfully"));
    }

    @PostMapping("/papers")
    public ResponseEntity<ApiResponse<ResearchPaperResponse>> publishPaper(
            @Valid @RequestBody ResearchPaperRequest request,
            Principal principal
    ) {
        ResearchPaperResponse response = researchPaperService.publishPaper(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Research paper published successfully"));
    }

    @PutMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<ResearchPaperResponse>> updatePaper(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResearchPaperRequest request,
            Principal principal
    ) {
        ResearchPaperResponse response = researchPaperService.updatePaper(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Research paper updated successfully"));
    }

    @DeleteMapping("/papers/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePaper(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        researchPaperService.deletePaper(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Research paper deleted successfully"));
    }
}
