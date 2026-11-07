package com.teligati.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private Long recipientId;
    private String recipientName;
    private String content;

    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
