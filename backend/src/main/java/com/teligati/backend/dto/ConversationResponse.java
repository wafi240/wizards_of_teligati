package com.teligati.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;
    private Long otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantEmail;
    private String otherParticipantRole;
    private String otherParticipantDept;
    private String lastMessageText;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private LocalDateTime createdAt;
}
