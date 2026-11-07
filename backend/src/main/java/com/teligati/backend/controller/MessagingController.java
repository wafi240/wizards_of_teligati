package com.teligati.backend.controller;

import com.teligati.backend.common.ApiResponse;
import com.teligati.backend.dto.*;
import com.teligati.backend.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService messagingService;

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationResponse>>> getUserConversations(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            Principal principal
    ) {
        PagedResponse<ConversationResponse> response = messagingService.getUserConversations(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Conversations retrieved successfully"));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateConversation(
            @Valid @RequestBody ConversationCreateRequest request,
            Principal principal
    ) {
        ConversationResponse response = messagingService.getOrCreateConversation(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Conversation ready"));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationById(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        ConversationResponse response = messagingService.getConversationById(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.success(response, "Conversation retrieved successfully"));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<PagedResponse<MessageResponse>>> getConversationMessages(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "50") int size,
            Principal principal
    ) {
        PagedResponse<MessageResponse> response = messagingService.getConversationMessages(principal.getName(), id, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Messages retrieved successfully"));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable("id") Long id,
            @Valid @RequestBody MessageSendRequest request,
            Principal principal
    ) {
        MessageResponse response = messagingService.sendMessage(principal.getName(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Message sent successfully"));
    }

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> markMessageAsRead(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        MessageResponse response = messagingService.markMessageAsRead(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(response, "Message marked as read"));
    }

    @PatchMapping("/conversations/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        messagingService.markConversationAsRead(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "All messages in conversation marked as read"));
    }
}
