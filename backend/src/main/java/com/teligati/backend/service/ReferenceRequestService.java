package com.teligati.backend.service;

import com.teligati.backend.dto.*;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.ReferenceRequest;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.ReferenceRequestRepository;
import com.teligati.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceRequestService {

    private final ReferenceRequestRepository referenceRequestRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "ACCEPTED");

    @Transactional
    public ReferenceRequestResponse createRequest(String principalName, ReferenceRequestCreateRequest request) {
        User student = getUserByPrincipal(principalName);

        // 1. Authorization: Only verified students can submit reference requests
        if (!isStudent(student)) {
            throw new AccessDeniedException("Only students can request references or recommendations");
        }
        if (!student.isVerified()) {
            throw new BadRequestException("Only verified students can submit reference requests. Complete institutional email verification first.");
        }

        // 2. Validate recipient
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Referee user not found with id: " + request.getRecipientId()));

        if (recipient.getId().equals(student.getId())) {
            throw new BadRequestException("You cannot request a reference from yourself");
        }

        if (!isTeacherOrAlumni(recipient)) {
            throw new BadRequestException("References can only be requested from Faculty/Teachers or Alumni");
        }

        // 3. Prevent duplicate active requests for the same institution/organization
        boolean hasActive = referenceRequestRepository.existsActiveRequest(
                student.getId(),
                recipient.getId(),
                request.getTargetInstitution().trim(),
                ACTIVE_STATUSES
        );
        if (hasActive) {
            throw new BadRequestException("An active reference request for this referee and institution already exists.");
        }

        ReferenceRequest referenceRequest = ReferenceRequest.builder()
                .student(student)
                .recipient(recipient)
                .recipientRole(recipient.getRole())
                .purpose(request.getPurpose().trim())
                .targetInstitution(request.getTargetInstitution().trim())
                .targetProgram(request.getTargetProgram() != null ? request.getTargetProgram().trim() : null)
                .deadline(request.getDeadline())
                .personalMessage(request.getPersonalMessage().trim())
                .supportingInfo(request.getSupportingInfo() != null ? request.getSupportingInfo().trim() : null)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReferenceRequest saved = referenceRequestRepository.save(referenceRequest);
        log.info("Reference request #{} created by student {} for referee {}", saved.getId(), student.getId(), recipient.getId());

        // 4. Notify recipient
        notificationService.createNotification(
                recipient,
                "New Reference Request: " + saved.getTargetInstitution(),
                student.getName() + " requested a recommendation/referral for " + saved.getTargetInstitution(),
                "REFERENCE_REQUEST",
                saved.getId().toString()
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReferenceRequestResponse> getSentRequests(String principalName, int page, int size) {
        User student = getUserByPrincipal(principalName);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReferenceRequest> requestPage = referenceRequestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId(), pageable);

        return PagedResponse.<ReferenceRequestResponse>builder()
                .content(requestPage.getContent().stream().map(this::mapToResponse).toList())
                .page(requestPage.getNumber())
                .size(requestPage.getSize())
                .totalElements(requestPage.getTotalElements())
                .totalPages(requestPage.getTotalPages())
                .last(requestPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReferenceRequestResponse> getIncomingRequests(String principalName, String status, int page, int size) {
        User recipient = getUserByPrincipal(principalName);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReferenceRequest> requestPage;

        if (status != null && !status.isBlank()) {
            requestPage = referenceRequestRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(
                    recipient.getId(), status.toUpperCase().trim(), pageable
            );
        } else {
            requestPage = referenceRequestRepository.findByRecipientIdOrderByCreatedAtDesc(recipient.getId(), pageable);
        }

        return PagedResponse.<ReferenceRequestResponse>builder()
                .content(requestPage.getContent().stream().map(this::mapToResponse).toList())
                .page(requestPage.getNumber())
                .size(requestPage.getSize())
                .totalElements(requestPage.getTotalElements())
                .totalPages(requestPage.getTotalPages())
                .last(requestPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ReferenceRequestResponse getRequestById(Long id, String principalName) {
        User caller = getUserByPrincipal(principalName);
        ReferenceRequest ref = referenceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reference request not found with id: " + id));

        boolean isStudentOwner = ref.getStudent().getId().equals(caller.getId());
        boolean isRecipient = ref.getRecipient().getId().equals(caller.getId());
        boolean isAdmin = isAdmin(caller);

        if (!isStudentOwner && !isRecipient && !isAdmin) {
            throw new AccessDeniedException("Unauthorized: You do not have permission to view this reference request");
        }

        return mapToResponse(ref);
    }

    @Transactional
    public ReferenceRequestResponse decideRequest(Long id, String principalName, ReferenceDecisionRequest request) {
        User recipient = getUserByPrincipal(principalName);
        ReferenceRequest ref = referenceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reference request not found with id: " + id));

        if (!ref.getRecipient().getId().equals(recipient.getId()) && !isAdmin(recipient)) {
            throw new AccessDeniedException("Only the designated referee can accept or decline this request");
        }

        if (!"PENDING".equalsIgnoreCase(ref.getStatus())) {
            throw new BadRequestException("Cannot make a decision on request with status: " + ref.getStatus() + ". Only PENDING requests can be decided.");
        }

        String newStatus = request.getStatus().toUpperCase().trim();
        if (!"ACCEPTED".equals(newStatus) && !"DECLINED".equals(newStatus)) {
            throw new BadRequestException("Decision must be either ACCEPTED or DECLINED");
        }

        ref.setStatus(newStatus);
        if (request.getResponseNotes() != null) {
            ref.setResponseNotes(request.getResponseNotes().trim());
        }
        ref.setUpdatedAt(LocalDateTime.now());
        ReferenceRequest updated = referenceRequestRepository.save(ref);

        // Notify Student
        if ("ACCEPTED".equals(newStatus)) {
            notificationService.createNotification(
                    ref.getStudent(),
                    "Reference Request Accepted",
                    recipient.getName() + " accepted your reference request for " + ref.getTargetInstitution() + ". Recommendation pending completion.",
                    "REFERENCE_ACCEPTED",
                    ref.getId().toString()
            );
        } else {
            notificationService.createNotification(
                    ref.getStudent(),
                    "Reference Request Declined",
                    recipient.getName() + " was unable to provide a reference for " + ref.getTargetInstitution() + ".",
                    "REFERENCE_DECLINED",
                    ref.getId().toString()
            );
        }

        return mapToResponse(updated);
    }

    @Transactional
    public ReferenceRequestResponse completeRecommendation(Long id, String principalName, RecommendationSubmitRequest request) {
        User recipient = getUserByPrincipal(principalName);
        ReferenceRequest ref = referenceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reference request not found with id: " + id));

        if (!ref.getRecipient().getId().equals(recipient.getId()) && !isAdmin(recipient)) {
            throw new AccessDeniedException("Only the designated referee can submit the completed recommendation");
        }

        if (!"ACCEPTED".equalsIgnoreCase(ref.getStatus())) {
            throw new BadRequestException("Recommendation can only be submitted for an ACCEPTED request. Current status: " + ref.getStatus());
        }

        ref.setRecommendationText(request.getRecommendationText().trim());
        if (request.getResponseNotes() != null && !request.getResponseNotes().isBlank()) {
            ref.setResponseNotes(request.getResponseNotes().trim());
        }
        ref.setStatus("COMPLETED");
        ref.setUpdatedAt(LocalDateTime.now());
        ReferenceRequest saved = referenceRequestRepository.save(ref);

        // Notify Student
        notificationService.createNotification(
                ref.getStudent(),
                "Recommendation Completed!",
                recipient.getName() + " completed your recommendation for " + ref.getTargetInstitution() + ".",
                "RECOMMENDATION_COMPLETED",
                ref.getId().toString()
        );

        return mapToResponse(saved);
    }

    @Transactional
    public ReferenceRequestResponse cancelRequest(Long id, String principalName) {
        User student = getUserByPrincipal(principalName);
        ReferenceRequest ref = referenceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reference request not found with id: " + id));

        if (!ref.getStudent().getId().equals(student.getId()) && !isAdmin(student)) {
            throw new AccessDeniedException("Only the requesting student can cancel this reference request");
        }

        if (!"PENDING".equalsIgnoreCase(ref.getStatus())) {
            throw new BadRequestException("Cannot cancel reference request with status: " + ref.getStatus() + ". Only PENDING requests can be cancelled.");
        }

        ref.setStatus("CANCELLED");
        ref.setUpdatedAt(LocalDateTime.now());
        ReferenceRequest saved = referenceRequestRepository.save(ref);

        // Notify Recipient
        notificationService.createNotification(
                ref.getRecipient(),
                "Reference Request Cancelled",
                student.getName() + " cancelled their reference request for " + ref.getTargetInstitution() + ".",
                "REFERENCE_CANCELLED",
                ref.getId().toString()
        );

        return mapToResponse(saved);
    }

    private ReferenceRequestResponse mapToResponse(ReferenceRequest r) {
        User student = r.getStudent();
        User recipient = r.getRecipient();

        Double cgpa = null;
        String skills = null;
        if (student != null) {
            Profile studentProfile = profileRepository.findByUserId(student.getId()).orElse(null);
            if (studentProfile != null) {
                cgpa = studentProfile.getCgpa();
                skills = studentProfile.getSkills();
            }
        }

        return ReferenceRequestResponse.builder()
                .id(r.getId())
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getName() : null)
                .studentRoll(student != null ? student.getKuetId() : null)
                .studentDept(student != null ? student.getDepartment() : null)
                .studentEmail(student != null ? student.getEmail() : null)
                .studentCgpa(cgpa)
                .studentSkills(skills)
                .recipientId(recipient != null ? recipient.getId() : null)
                .recipientName(recipient != null ? recipient.getName() : null)
                .recipientRole(r.getRecipientRole())
                .recipientDept(recipient != null ? recipient.getDepartment() : null)
                .recipientEmail(recipient != null ? recipient.getEmail() : null)
                .purpose(r.getPurpose())
                .targetInstitution(r.getTargetInstitution())
                .targetProgram(r.getTargetProgram())
                .deadline(r.getDeadline())
                .personalMessage(r.getPersonalMessage())
                .supportingInfo(r.getSupportingInfo())
                .status(r.getStatus())
                .recommendationText(r.getRecommendationText())
                .responseNotes(r.getResponseNotes())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }

    private boolean isStudent(User user) {
        String role = user.getRole();
        return "ROLE_STUDENT".equalsIgnoreCase(role) || "STUDENT".equalsIgnoreCase(role);
    }

    private boolean isTeacherOrAlumni(User user) {
        String role = user.getRole();
        return "ROLE_TEACHER".equalsIgnoreCase(role) || "TEACHER".equalsIgnoreCase(role)
                || "ROLE_FACULTY".equalsIgnoreCase(role) || "FACULTY".equalsIgnoreCase(role)
                || "ROLE_ALUMNI".equalsIgnoreCase(role) || "ALUMNI".equalsIgnoreCase(role);
    }

    private boolean isAdmin(User user) {
        String role = user.getRole();
        return "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }
}
