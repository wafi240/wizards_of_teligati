package com.teligati.backend.repository;

import com.teligati.backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId AND m.recipient.id = :userId AND m.isRead = false")
    long countUnreadMessages(@Param("convId") Long convId, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId AND m.recipient.id = :userId AND m.isRead = false")
    List<Message> findUnreadInConversation(@Param("convId") Long convId, @Param("userId") Long userId);
}
