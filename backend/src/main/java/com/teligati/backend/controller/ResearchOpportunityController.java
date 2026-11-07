package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.dto.ProjectRequest;
import com.teligati.backend.dto.ProjectResponse;
import com.teligati.backend.dto.ProposalDecisionRequest;
import com.teligati.backend.dto.ResearchProposalRequest;
import com.teligati.backend.dto.ResearchProposalResponse;
import com.teligati.backend.service.ResearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/research-opportunities")
public class ResearchOpportunityController {

    private final ResearchService researchService;

    public ResearchOpportunityController(ResearchService researchService) {
        this.researchService = researchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProjectResponse>>> getResearchOpportunities(
            @RequestParam(name = "status", required = false, defaultValue = "OPEN") String status,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        PagedResponse<ProjectResponse> response = researchService.getProjects(status, type, search, principalName, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Research opportunities retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getResearchOpportunityById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        String principalName = principal != null ? principal.getName() : null;
        ProjectResponse response = researchService.getProjectById(id, principalName);
        return ResponseEntity.ok(ApiResponse.success(response, "Research opportunity details retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createResearchOpportunity(
            @Valid @RequestBody ProjectRequest request,
            Principal principal
    ) {
        ProjectResponse response = researchService.createProject(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Research opportunity created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateResearchOpportunity(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProjectRequest request,
            Principal principal
    ) {
        ProjectResponse response = researchService.updateProject(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Research opportunity updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResearchOpportunity(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        researchService.deleteProject(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Research opportunity deleted successfully"));
    }

    // ==========================================
    // Research Proposals (Student -> Faculty)
    // ==========================================

    @PostMapping("/proposals")
    public ResponseEntity<ApiResponse<ResearchProposalResponse>> submitProposal(
            @Valid @RequestBody ResearchProposalRequest request,
            Principal principal
    ) {
        ResearchProposalResponse response = researchService.submitProposal(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Research proposal submitted successfully"));
    }

    @GetMapping({"/proposals/me", "/proposals/faculty"})
    public ResponseEntity<ApiResponse<PagedResponse<ResearchProposalResponse>>> getMyProposals(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ResearchProposalResponse> response = researchService.getMyProposals(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Proposals retrieved successfully"));
    }

    @GetMapping("/proposals/{id}")
    public ResponseEntity<ApiResponse<ResearchProposalResponse>> getProposalById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ResearchProposalResponse response = researchService.getProposalById(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Proposal details retrieved successfully"));
    }

    @PatchMapping("/proposals/{id}/decision")
    public ResponseEntity<ApiResponse<ResearchProposalResponse>> decideProposal(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProposalDecisionRequest decision,
            Principal principal
    ) {
        ResearchProposalResponse response = researchService.decideProposal(id, principal.getName(), decision);
        return ResponseEntity.ok(ApiResponse.success(response, "Proposal decision recorded successfully"));
    }
}
