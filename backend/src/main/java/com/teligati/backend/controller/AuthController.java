package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.*;
import com.teligati.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/student")
    public ResponseEntity<ApiResponse<TokenResponse>> registerStudent(
            @Valid @RequestBody StudentRegisterRequest request
    ) {
        TokenResponse response = authService.registerStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Student registered successfully"));
    }

    @PostMapping("/register/alumni")
    public ResponseEntity<ApiResponse<TokenResponse>> registerAlumni(
            @Valid @RequestBody AlumniRegisterRequest request
    ) {
        TokenResponse response = authService.registerAlumni(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Alumni registered successfully"));
    }

    @PostMapping("/register/faculty")
    public ResponseEntity<ApiResponse<TokenResponse>> registerFaculty(
            @Valid @RequestBody FacultyRegisterRequest request
    ) {
        TokenResponse response = authService.registerFaculty(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Faculty registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        TokenResponse response = authService.login(request, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        TokenResponse response = authService.refreshToken(request, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            Principal principal
    ) {
        String token = request != null ? request.getRefreshToken() : null;
        String identifier = principal != null ? principal.getName() : null;
        authService.logout(token, identifier);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUser(Principal principal) {
        UserSummaryResponse response = authService.getCurrentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Current user retrieved successfully"));
    }

    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> getVerificationStatus(Principal principal) {
        VerificationStatusResponse response = authService.getVerificationStatus(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Verification status retrieved successfully"));
    }

    @GetMapping("/student/restricted-feature")
    @PreAuthorize("hasRole('ROLE_STUDENT') and @userSecurity.isVerified(authentication)")
    public ResponseEntity<ApiResponse<String>> accessRestrictedStudentFeature() {
        return ResponseEntity.ok(ApiResponse.success("Access granted to verified student resource", "Verification check passed"));
    }
}
