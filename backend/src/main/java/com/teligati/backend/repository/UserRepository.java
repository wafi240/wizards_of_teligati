package com.teligati.backend.repository;

import com.teligati.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByKuetId(String kuetId);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:identifier) OR u.kuetId = :identifier")
    Optional<User> findByIdentifier(String identifier);

    boolean existsByEmail(String email);
    boolean existsByKuetId(String kuetId);
}
