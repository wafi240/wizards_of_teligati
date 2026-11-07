package com.teligati.backend.repository;

import com.teligati.backend.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByStatus(String status, Pageable pageable);

    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);

    List<Project> findByOwnerId(Long ownerId);

    @Query("SELECT p FROM Project p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:type IS NULL OR LOWER(p.type) = LOWER(:type)) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.technologies) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.ownerDept) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> searchProjects(@Param("status") String status,
                                  @Param("type") String type,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("SELECT p FROM Project p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:type IS NULL OR LOWER(p.type) = LOWER(:type)) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.technologies) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.skillsRequired) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.ownerDept) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> searchResearchOpportunities(@Param("status") String status,
                                              @Param("category") String category,
                                              @Param("type") String type,
                                              @Param("search") String search,
                                              Pageable pageable);
}
