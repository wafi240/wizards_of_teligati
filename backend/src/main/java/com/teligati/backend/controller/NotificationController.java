package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.NotificationResponse;
import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getMyNotifications(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal.getName());
        PagedResponse<NotificationResponse> response = notificationService.getUserNotifications(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Notifications retrieved successfully"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal.getName());
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }
}
