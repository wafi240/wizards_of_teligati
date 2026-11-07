package com.teligati.backend.service;

import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.dto.ResearchPaperRequest;
import com.teligati.backend.dto.ResearchPaperResponse;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.ResearchPaper;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.ResearchPaperRepository;
import com.teligati.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchPaperService {

    private final ResearchPaperRepository researchPaperRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public ResearchPaperResponse publishPaper(String principalName, ResearchPaperRequest request) {
        User teacher = getUserByPrincipal(principalName);

        // Only teachers/faculty or admin can publish research papers
        if (!isTeacherOrAdmin(teacher)) {
            throw new BadRequestException("Only faculty members are authorized to publish academic research papers");
        }

        ResearchPaper paper = ResearchPaper.builder()
                .title(request.getTitle())
                .abstractText(request.getAbstractText())
                .researchField(request.getResearchField())
                .authors(request.getAuthors())
                .publicationDate(request.getPublicationDate())
                .doi(request.getDoi())
                .referenceUrl(request.getReferenceUrl())
                .attachmentUrl(request.getAttachmentUrl())
                .teacher(teacher)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paper = researchPaperRepository.save(paper);
        log.info("Research paper published: id={}, title='{}', teacherId={}", paper.getId(), paper.getTitle(), teacher.getId());
        return mapToResponse(paper);
    }

    @Transactional
    public ResearchPaperResponse updatePaper(Long id, String principalName, ResearchPaperRequest request) {
        User caller = getUserByPrincipal(principalName);
        ResearchPaper paper = researchPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research paper not found with id: " + id));

        verifyPaperOwnership(paper, caller);

        paper.setTitle(request.getTitle());
        paper.setAbstractText(request.getAbstractText());
        paper.setResearchField(request.getResearchField());
        paper.setAuthors(request.getAuthors());
        if (request.getPublicationDate() != null) paper.setPublicationDate(request.getPublicationDate());
        if (request.getDoi() != null) paper.setDoi(request.getDoi());
        if (request.getReferenceUrl() != null) paper.setReferenceUrl(request.getReferenceUrl());
        if (request.getAttachmentUrl() != null) paper.setAttachmentUrl(request.getAttachmentUrl());

        paper = researchPaperRepository.save(paper);
        log.info("Research paper updated: id={}, callerId={}", id, caller.getId());
        return mapToResponse(paper);
    }

    @Transactional
    public void deletePaper(Long id, String principalName) {
        User caller = getUserByPrincipal(principalName);
        ResearchPaper paper = researchPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research paper not found with id: " + id));

        verifyPaperOwnership(paper, caller);

        researchPaperRepository.delete(paper);
        log.info("Research paper deleted: id={}, callerId={}", id, caller.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResearchPaperResponse> getPapers(String field, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ResearchPaper> paperPage = researchPaperRepository.findByFilter(field, search, pageable);

        return PagedResponse.<ResearchPaperResponse>builder()
                .content(paperPage.getContent().stream().map(this::mapToResponse).toList())
                .page(paperPage.getNumber())
                .size(paperPage.getSize())
                .totalElements(paperPage.getTotalElements())
                .totalPages(paperPage.getTotalPages())
                .last(paperPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ResearchPaperResponse getPaperById(Long id) {
        ResearchPaper paper = researchPaperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research paper not found with id: " + id));
        return mapToResponse(paper);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResearchPaperResponse> getMyPapers(String principalName, int page, int size) {
        User teacher = getUserByPrincipal(principalName);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ResearchPaper> paperPage = researchPaperRepository.findByTeacherId(teacher.getId(), pageable);

        return PagedResponse.<ResearchPaperResponse>builder()
                .content(paperPage.getContent().stream().map(this::mapToResponse).toList())
                .page(paperPage.getNumber())
                .size(paperPage.getSize())
                .totalElements(paperPage.getTotalElements())
                .totalPages(paperPage.getTotalPages())
                .last(paperPage.isLast())
                .build();
    }

    private void verifyPaperOwnership(ResearchPaper paper, User caller) {
        boolean isOwner = paper.getTeacher().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Unauthorized: You can only modify your own published research papers");
        }
    }

    private boolean isTeacherOrAdmin(User user) {
        String role = user.getRole();
        return "ROLE_TEACHER".equalsIgnoreCase(role) || "ROLE_FACULTY".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }

    private ResearchPaperResponse mapToResponse(ResearchPaper paper) {
        String teacherDept = profileRepository.findByUserId(paper.getTeacher().getId())
                .map(Profile::getDepartment)
                .orElse("Dept of CSE");

        return ResearchPaperResponse.builder()
                .id(paper.getId())
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .researchField(paper.getResearchField())
                .authors(paper.getAuthors())
                .publicationDate(paper.getPublicationDate())
                .doi(paper.getDoi())
                .referenceUrl(paper.getReferenceUrl())
                .attachmentUrl(paper.getAttachmentUrl())
                .teacherId(paper.getTeacher().getId())
                .teacherName(paper.getTeacher().getName())
                .teacherDept(teacherDept)
                .createdAt(paper.getCreatedAt())
                .updatedAt(paper.getUpdatedAt())
                .build();
    }
}
