package com.teligati.backend.repository;

import com.teligati.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findByUserIdOrderByTimestampDesc(Long userId);
    List<Transaction> findByEscrowIdOrderByTimestampDesc(Long escrowId);
}
