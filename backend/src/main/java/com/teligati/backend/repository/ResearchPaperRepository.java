package com.teligati.backend.repository;

import com.teligati.backend.entity.ResearchPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, Long> {
    Page<ResearchPaper> findByTeacherId(Long teacherId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE " +
           "(:field IS NULL OR LOWER(p.researchField) = LOWER(:field)) AND " +
           "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.authors) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ResearchPaper> findByFilter(@Param("field") String field,
                                     @Param("search") String search,
                                     Pageable pageable);

    List<ResearchPaper> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);
}
