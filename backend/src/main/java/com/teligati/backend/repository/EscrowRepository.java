package com.teligati.backend.repository;

import com.teligati.backend.entity.Escrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscrowRepository extends JpaRepository<Escrow, Long> {
    Optional<Escrow> findByEscrowReference(String escrowReference);
    List<Escrow> findByPayerIdOrderByCreatedAtDesc(Long payerId);
    List<Escrow> findByPayeeIdOrderByCreatedAtDesc(Long payeeId);

    @Query("SELECT e FROM Escrow e WHERE e.payer.id = :userId OR e.payee.id = :userId ORDER BY e.createdAt DESC")
    List<Escrow> findAllByUserId(@Param("userId") Long userId);
}
