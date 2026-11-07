package com.teligati.backend.repository;

import com.teligati.backend.entity.InvitationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {
    Optional<InvitationToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM InvitationToken t WHERE t.tokenHash = :tokenHash")
    Optional<InvitationToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
