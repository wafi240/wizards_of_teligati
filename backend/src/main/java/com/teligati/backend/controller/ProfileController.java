package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.ProfileResponse;
import com.teligati.backend.dto.ProfileUpdateRequest;
import com.teligati.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(Principal principal) {
        ProfileResponse response = profileService.getMyProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Profile retrieved successfully"));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            Principal principal,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        ProfileResponse response = profileService.updateMyProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfileById(
            @PathVariable("userId") Long userId,
            Principal principal
    ) {
        ProfileResponse response = profileService.getProfileById(userId, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "User profile retrieved successfully"));
    }
}
