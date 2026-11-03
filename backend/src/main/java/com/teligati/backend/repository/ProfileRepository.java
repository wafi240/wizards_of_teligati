package com.teligati.backend.repository;

import com.teligati.backend.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    List<Profile> findByCountryIgnoreCase(String country);
    List<Profile> findByDepartmentIgnoreCase(String department);
}
