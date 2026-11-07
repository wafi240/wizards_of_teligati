package com.teligati.backend.repository;

import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long>, JpaSpecificationExecutor<Profile> {
    Optional<Profile> findByRoll(String roll);
    Optional<Profile> findByUserId(Long userId);
    Optional<Profile> findByUser(User user);
}
