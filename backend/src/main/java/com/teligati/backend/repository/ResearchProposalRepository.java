package com.teligati.backend.repository;

import com.teligati.backend.entity.ResearchProposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResearchProposalRepository extends JpaRepository<ResearchProposal, Long> {

    Page<ResearchProposal> findByStudentId(Long studentId, Pageable pageable);

    List<ResearchProposal> findByStudentId(Long studentId);

    Page<ResearchProposal> findByFacultyId(Long facultyId, Pageable pageable);

    List<ResearchProposal> findByFacultyId(Long facultyId);

    Page<ResearchProposal> findByFacultyIdAndStatus(Long facultyId, String status, Pageable pageable);
}
