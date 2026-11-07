package com.teligati.backend.repository;

import com.teligati.backend.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByParticipant1IdAndParticipant2Id(Long p1, Long p2);

    @Query("SELECT c FROM Conversation c WHERE c.participant1.id = :userId OR c.participant2.id = :userId ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE (c.participant1.id = :u1 AND c.participant2.id = :u2) OR (c.participant1.id = :u2 AND c.participant2.id = :u1)")
    Optional<Conversation> findBetweenUsers(@Param("u1") Long u1, @Param("u2") Long u2);
}
