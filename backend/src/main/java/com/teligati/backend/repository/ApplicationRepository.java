package com.teligati.backend.repository;

import com.teligati.backend.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Page<Application> findByJobId(Long jobId, Pageable pageable);

    List<Application> findByJobId(Long jobId);

    Page<Application> findByProjectId(Long projectId, Pageable pageable);

    List<Application> findByProjectId(Long projectId);

    Page<Application> findByApplicantId(Long applicantId, Pageable pageable);

    List<Application> findByApplicantId(Long applicantId);

    Page<Application> findByApplicantIdAndProjectIdIsNotNull(Long applicantId, Pageable pageable);

    boolean existsByJobIdAndApplicantId(Long jobId, Long applicantId);

    boolean existsByProjectIdAndApplicantId(Long projectId, Long applicantId);

    boolean existsByProjectIdAndApplicantIdAndStatusNotIn(Long projectId, Long applicantId, Collection<String> statuses);

    Optional<Application> findByJobIdAndApplicantId(Long jobId, Long applicantId);

    Optional<Application> findByProjectIdAndApplicantId(Long projectId, Long applicantId);

    @Query("SELECT a FROM Application a WHERE a.job.postedBy.id = :ownerId")
    Page<Application> findReceivedJobApplications(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.project.owner.id = :ownerId")
    Page<Application> findReceivedProjectApplications(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE (a.job IS NOT NULL AND a.job.postedBy.id = :ownerId) OR " +
           "(a.project IS NOT NULL AND a.project.owner.id = :ownerId)")
    Page<Application> findAllReceivedApplications(@Param("ownerId") Long ownerId, Pageable pageable);
}
