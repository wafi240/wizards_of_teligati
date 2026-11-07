package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.EscrowCreateRequest;
import com.teligati.backend.dto.EscrowResponse;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.service.EscrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/escrow")
@RequiredArgsConstructor
public class EscrowController {

    private final EscrowService escrowService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<EscrowResponse>> createEscrow(
            Principal principal,
            @Valid @RequestBody EscrowCreateRequest request
    ) {
        User user = getUserByPrincipal(principal.getName());
        EscrowResponse response = escrowService.createAndFundEscrow(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Demo escrow created and funded successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EscrowResponse>>> getMyEscrows(Principal principal) {
        User user = getUserByPrincipal(principal.getName());
        List<EscrowResponse> response = escrowService.getUserEscrows(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "User escrow contracts retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EscrowResponse>> getEscrowById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal.getName());
        EscrowResponse response = escrowService.getEscrow(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Escrow contract details retrieved"));
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<EscrowResponse>> startWork(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal.getName());
        EscrowResponse response = escrowService.startWork(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Escrow contract marked as in-progress"));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<EscrowResponse>> releaseEscrow(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal.getName());
        EscrowResponse response = escrowService.releaseEscrow(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Demo escrow funds successfully released to payee"));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<EscrowResponse>> refundEscrow(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal.getName());
        EscrowResponse response = escrowService.refundEscrow(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Demo escrow funds successfully refunded to payer"));
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<ApiResponse<EscrowResponse>> disputeEscrow(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal.getName());
        String reason = body != null ? body.getOrDefault("reason", "Disputed by party") : "Disputed by party";
        EscrowResponse response = escrowService.disputeEscrow(id, user.getId(), reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Escrow contract marked as disputed"));
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }
}
