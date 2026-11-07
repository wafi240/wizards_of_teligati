package com.teligati.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false)
    private User participant2;

    @Column(name = "last_message_text", columnDefinition = "TEXT")
    private String lastMessageText;

    @Builder.Default
    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public boolean hasParticipant(Long userId) {
        if (userId == null) return false;
        return (participant1 != null && userId.equals(participant1.getId()))
                || (participant2 != null && userId.equals(participant2.getId()));
    }

    public User getOtherParticipant(Long userId) {
        if (participant1 != null && participant1.getId().equals(userId)) {
            return participant2;
        }
        return participant1;
    }
}
