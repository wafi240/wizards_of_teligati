package com.teligati.backend.repository;

import com.teligati.backend.entity.ReferenceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReferenceRequestRepository extends JpaRepository<ReferenceRequest, Long> {

    Page<ReferenceRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    Page<ReferenceRequest> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<ReferenceRequest> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, String status, Pageable pageable);

    List<ReferenceRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<ReferenceRequest> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ReferenceRequest r " +
           "WHERE r.student.id = :studentId AND r.recipient.id = :recipientId " +
           "AND LOWER(r.targetInstitution) = LOWER(:targetInstitution) " +
           "AND r.status IN :statuses")
    boolean existsActiveRequest(
            @Param("studentId") Long studentId,
            @Param("recipientId") Long recipientId,
            @Param("targetInstitution") String targetInstitution,
            @Param("statuses") Collection<String> statuses
    );
}
