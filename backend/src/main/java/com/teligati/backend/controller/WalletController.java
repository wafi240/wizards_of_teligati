package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.TopUpRequest;
import com.teligati.backend.dto.TransactionResponse;
import com.teligati.backend.dto.WalletResponse;
import com.teligati.backend.dto.WithdrawRequest;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(Principal principal) {
        User user = getUserByPrincipal(principal.getName());
        WalletResponse response = walletService.getWallet(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Demo wallet balance retrieved"));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<WalletResponse>> topUp(
            Principal principal,
            @Valid @RequestBody TopUpRequest request
    ) {
        User user = getUserByPrincipal(principal.getName());
        WalletResponse response = walletService.simulateTopUp(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Demo credits successfully added to wallet"));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(
            Principal principal,
            @Valid @RequestBody WithdrawRequest request
    ) {
        User user = getUserByPrincipal(principal.getName());
        WalletResponse response = walletService.simulateWithdraw(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Simulated demo withdrawal processed"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(Principal principal) {
        User user = getUserByPrincipal(principal.getName());
        List<TransactionResponse> response = walletService.getTransactions(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Demo transaction ledger retrieved"));
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }
}
