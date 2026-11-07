package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.*;
import com.teligati.backend.service.ReferenceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/references")
@RequiredArgsConstructor
public class ReferenceRequestController {

    private final ReferenceRequestService referenceRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReferenceRequestResponse>> createRequest(
            @Valid @RequestBody ReferenceRequestCreateRequest request,
            Principal principal
    ) {
        ReferenceRequestResponse response = referenceRequestService.createRequest(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reference request submitted successfully"));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PagedResponse<ReferenceRequestResponse>>> getSentRequests(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ReferenceRequestResponse> response = referenceRequestService.getSentRequests(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Sent reference requests retrieved successfully"));
    }

    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<PagedResponse<ReferenceRequestResponse>>> getIncomingRequests(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ReferenceRequestResponse> response = referenceRequestService.getIncomingRequests(principal.getName(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Incoming reference requests retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferenceRequestResponse>> getRequestById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ReferenceRequestResponse response = referenceRequestService.getRequestById(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Reference request retrieved successfully"));
    }

    @PatchMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<ReferenceRequestResponse>> decideRequest(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReferenceDecisionRequest request,
            Principal principal
    ) {
        ReferenceRequestResponse response = referenceRequestService.decideRequest(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Reference request decision processed successfully"));
    }

    @PatchMapping("/{id}/recommendation")
    public ResponseEntity<ApiResponse<ReferenceRequestResponse>> completeRecommendation(
            @PathVariable("id") Long id,
            @Valid @RequestBody RecommendationSubmitRequest request,
            Principal principal
    ) {
        ReferenceRequestResponse response = referenceRequestService.completeRecommendation(id, principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Recommendation submitted and marked as completed"));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReferenceRequestResponse>> cancelRequest(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ReferenceRequestResponse response = referenceRequestService.cancelRequest(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Reference request cancelled successfully"));
    }
}
