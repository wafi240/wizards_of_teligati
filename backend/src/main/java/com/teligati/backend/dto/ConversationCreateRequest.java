package com.teligati.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCreateRequest {

    @NotNull(message = "Recipient ID is required to start a conversation")
    private Long recipientId;

    private String initialMessage;
}
