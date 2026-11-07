package com.teligati.backend.repository;

import com.teligati.backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByCategoryAndStatus(String category, String status, Pageable pageable);

    Page<Job> findByPostedById(Long postedById, Pageable pageable);

    List<Job> findByPostedById(Long postedById);

    @Query("SELECT j FROM Job j WHERE " +
           "(:category IS NULL OR j.category = :category) AND " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.company) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.organization) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(j.skillsRequired) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Job> searchJobs(@Param("category") String category,
                         @Param("status") String status,
                         @Param("search") String search,
                         Pageable pageable);
}
