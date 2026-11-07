package com.teligati.backend.service;

import com.teligati.backend.dto.*;
import com.teligati.backend.entity.Application;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.Project;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ApplicationRepository;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.ProjectRepository;
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
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchAcademicService {

    private final ProjectRepository projectRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    private static final List<String> ACTIVE_APPLICATION_STATUSES = List.of(
            "PENDING", "APPLIED", "UNDER_REVIEW", "REVISION_REQUESTED", "ACCEPTED"
    );

    // =========================================================================
    // 1. Research Opportunities Management (Teacher & Discovery)
    // =========================================================================

    @Transactional
    public ResearchOpportunityResponse createOpportunity(String principalName, ResearchOpportunityRequest request) {
        User teacher = getUserByPrincipal(principalName);

        if (!isTeacherOrAdmin(teacher)) {
            throw new BadRequestException("Only faculty members can publish research opportunities");
        }

        String title = request.getResolvedTitle();
        String category = request.getCategory() != null ? request.getCategory() : "COMPUTER_SCIENCE";
        String researchType = request.getResearchType() != null ? request.getResearchType() : "Research Opportunity";

        Project project = Project.builder()
                .name(title)
                .description(request.getDescription())
                .technologies(request.getRequiredSkills())
                .skillsRequired(request.getRequiredSkills())
                .preferredBackground(request.getPreferredBackground())
                .positionsCount(request.getPositionsCount() != null ? request.getPositionsCount() : 1)
                .deadline(request.getDeadline())
                .eligibilityCriteria(request.getEligibilityCriteria())
                .duration(request.getDuration())
                .category(category)
                .type(researchType)
                .status("OPEN")
                .minCgpa(request.getMinCgpa())
                .ownerDept(request.getOwnerDept())
                .paperDoi(request.getPaperDoi())
                .owner(teacher)
                .isOpenForContribution(true)
                .contributorsCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        project = projectRepository.save(project);
        log.info("Research opportunity created: id={}, title='{}', teacherId={}", project.getId(), project.getName(), teacher.getId());
        return mapToOpportunityResponse(project, teacher.getId());
    }

    @Transactional
    public ResearchOpportunityResponse updateOpportunity(Long id, String principalName, ResearchOpportunityRequest request) {
        User caller = getUserByPrincipal(principalName);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research opportunity not found with id: " + id));

        verifyOpportunityOwnership(project, caller);

        if (request.getTitle() != null || request.getName() != null) {
            project.setName(request.getResolvedTitle());
        }
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getRequiredSkills() != null) {
            project.setSkillsRequired(request.getRequiredSkills());
            project.setTechnologies(request.getRequiredSkills());
        }
        if (request.getPreferredBackground() != null) project.setPreferredBackground(request.getPreferredBackground());
        if (request.getPositionsCount() != null) project.setPositionsCount(request.getPositionsCount());
        if (request.getDeadline() != null) project.setDeadline(request.getDeadline());
        if (request.getEligibilityCriteria() != null) project.setEligibilityCriteria(request.getEligibilityCriteria());
        if (request.getDuration() != null) project.setDuration(request.getDuration());
        if (request.getCategory() != null) project.setCategory(request.getCategory());
        if (request.getResearchType() != null) project.setType(request.getResearchType());
        if (request.getMinCgpa() != null) project.setMinCgpa(request.getMinCgpa());
        if (request.getStatus() != null) project.setStatus(request.getStatus().toUpperCase());

        project = projectRepository.save(project);
        log.info("Research opportunity updated: id={}, callerId={}", id, caller.getId());
        return mapToOpportunityResponse(project, caller.getId());
    }

    @Transactional
    public void deleteOpportunity(Long id, String principalName) {
        User caller = getUserByPrincipal(principalName);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research opportunity not found with id: " + id));

        verifyOpportunityOwnership(project, caller);
        projectRepository.delete(project);
        log.info("Research opportunity deleted: id={}, callerId={}", id, caller.getId());
    }

    @Transactional
    public ResearchOpportunityResponse closeOpportunity(Long id, String principalName) {
        User caller = getUserByPrincipal(principalName);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research opportunity not found with id: " + id));

        verifyOpportunityOwnership(project, caller);
        project.setStatus("CLOSED");
        project.setOpenForContribution(false);
        project = projectRepository.save(project);
        log.info("Research opportunity closed: id={}, callerId={}", id, caller.getId());
        return mapToOpportunityResponse(project, caller.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResearchOpportunityResponse> getOpportunities(String category, String status, String search, String principalName, int page, int size) {
        Long callerId = resolveUserIdSilently(principalName);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Project> projectPage = projectRepository.searchResearchOpportunities(status, category, null, search, pageable);

        return PagedResponse.<ResearchOpportunityResponse>builder()
                .content(projectPage.getContent().stream().map(p -> mapToOpportunityResponse(p, callerId)).toList())
                .page(projectPage.getNumber())
                .size(projectPage.getSize())
                .totalElements(projectPage.getTotalElements())
                .totalPages(projectPage.getTotalPages())
                .last(projectPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ResearchOpportunityResponse getOpportunityById(Long id, String principalName) {
        Long callerId = resolveUserIdSilently(principalName);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research opportunity not found with id: " + id));
        return mapToOpportunityResponse(project, callerId);
    }

    // =========================================================================
    // 2. Research Applications Workflow
    // =========================================================================

    @Transactional
    public ResearchApplicationResponse submitApplication(Long opportunityId, String principalName, ResearchApplicationSubmitRequest request) {
        User student = getUserByPrincipal(principalName);

        if (!"ROLE_STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new BadRequestException("Only students can apply for research opportunities");
        }

        if (!student.isVerified()) {
            throw new BadRequestException("Student account must be institutional email verified to apply for research opportunities");
        }

        Project opportunity = projectRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Research opportunity not found with id: " + opportunityId));

        if (!"OPEN".equalsIgnoreCase(opportunity.getStatus())) {
            throw new BadRequestException("Cannot apply: Research opportunity is currently " + opportunity.getStatus());
        }

        // Deadline validation
        if (opportunity.getDeadline() != null && LocalDateTime.now().isAfter(opportunity.getDeadline())) {
            throw new BadRequestException("The application deadline for this research opportunity has passed");
        }

        Profile profile = profileRepository.findByUserId(student.getId()).orElse(null);
        Double cgpa = request.getCgpa() != null ? request.getCgpa() : (profile != null ? profile.getCgpa() : null);

        // CGPA eligibility check
        if (opportunity.getMinCgpa() != null && cgpa != null && cgpa < opportunity.getMinCgpa()) {
            throw new BadRequestException("Applicant CGPA (" + cgpa + ") is below the minimum required CGPA (" + opportunity.getMinCgpa() + ")");
        }

        String dept = request.getDepartment() != null ? request.getDepartment() : (profile != null ? profile.getDepartment() : "Dept of CSE");
        String skills = request.getSkills() != null ? request.getSkills() : (profile != null ? profile.getSkills() : "");
        String roll = student.getKuetId() != null ? student.getKuetId() : (profile != null && profile.getRoll() != null ? profile.getRoll() : "N/A");

        // Prevent duplicate active applications & handle re-applications cleanly
        Optional<Application> existingApp = applicationRepository.findByProjectIdAndApplicantId(opportunity.getId(), student.getId());
        Application application;
        if (existingApp.isPresent()) {
            application = existingApp.get();
            if (ACTIVE_APPLICATION_STATUSES.contains(application.getStatus())) {
                throw new BadRequestException("You have already submitted an active application for this research opportunity");
            }
            application.setStatement(request.getStatement());
            application.setStatus("PENDING");
            application.setApplicantCgpa(cgpa);
            application.setApplicantDept(dept);
            application.setApplicantSkills(skills);
            application.setApplicantRoll(roll);
            application.setUpdatedAt(LocalDateTime.now());
        } else {
            application = Application.builder()
                    .opportunityType("PROJECT")
                    .project(opportunity)
                    .applicant(student)
                    .statement(request.getStatement())
                    .status("PENDING")
                    .applicantName(student.getName())
                    .applicantRoll(roll)
                    .applicantDept(dept)
                    .applicantCgpa(cgpa)
                    .applicantSkills(skills)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Increment applicant counter on project
            opportunity.setContributorsCount(opportunity.getContributorsCount() + 1);
            projectRepository.save(opportunity);
        }

        application = applicationRepository.save(application);

        // Generate notification for publishing teacher
        if (opportunity.getOwner() != null) {
            notificationService.createNotification(
                    opportunity.getOwner(),
                    "New Research Application",
                    "Student " + student.getName() + " applied for '" + opportunity.getName() + "'.",
                    "RESEARCH_APPLICATION",
                    String.valueOf(application.getId())
            );
        }

        log.info("Research application submitted: appId={}, oppId={}, studentId={}", application.getId(), opportunityId, student.getId());
        return mapToApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResearchApplicationResponse> getApplicationsForOpportunity(Long opportunityId, String principalName, int page, int size) {
        User teacher = getUserByPrincipal(principalName);
        Project opportunity = projectRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Research opportunity not found with id: " + opportunityId));

        verifyOpportunityOwnership(opportunity, teacher);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Application> appPage = applicationRepository.findByProjectId(opportunityId, pageable);

        return PagedResponse.<ResearchApplicationResponse>builder()
                .content(appPage.getContent().stream().map(this::mapToApplicationResponse).toList())
                .page(appPage.getNumber())
                .size(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResearchApplicationResponse> getMyApplications(String principalName, int page, int size) {
        User student = getUserByPrincipal(principalName);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Application> appPage = applicationRepository.findByApplicantIdAndProjectIdIsNotNull(student.getId(), pageable);

        return PagedResponse.<ResearchApplicationResponse>builder()
                .content(appPage.getContent().stream().map(this::mapToApplicationResponse).toList())
                .page(appPage.getNumber())
                .size(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ResearchApplicationResponse getApplicationById(Long id, String principalName) {
        User caller = getUserByPrincipal(principalName);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research application not found with id: " + id));

        boolean isApplicant = application.getApplicant().getId().equals(caller.getId());
        boolean isOpportunityOwner = application.getProject() != null &&
                application.getProject().getOwner() != null &&
                application.getProject().getOwner().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());

        if (!isApplicant && !isOpportunityOwner && !isAdmin) {
            throw new AccessDeniedException("Unauthorized to view this application");
        }

        return mapToApplicationResponse(application);
    }

    @Transactional
    public ResearchApplicationResponse reviewApplication(Long id, String principalName, ResearchApplicationReviewRequest request) {
        User teacher = getUserByPrincipal(principalName);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research application not found with id: " + id));

        if (application.getProject() == null) {
            throw new BadRequestException("Application is not linked to a research project");
        }

        verifyOpportunityOwnership(application.getProject(), teacher);

        String currentStatus = application.getStatus() != null ? application.getStatus().toUpperCase() : "PENDING";
        String targetStatus = request.getStatus().toUpperCase();

        if ("WITHDRAWN".equals(currentStatus)) {
            throw new BadRequestException("Cannot review an application that has been withdrawn by the student");
        }
        if ("REJECTED".equals(currentStatus)) {
            throw new BadRequestException("Cannot review an application that has already been rejected");
        }
        if ("ACCEPTED".equals(currentStatus) && !"ACCEPTED".equals(targetStatus)) {
            throw new BadRequestException("Cannot change status of an application that has already been accepted into the lab");
        }

        if (!List.of("UNDER_REVIEW", "ACCEPTED", "REJECTED", "REVISION_REQUESTED").contains(targetStatus)) {
            throw new BadRequestException("Invalid review status: " + targetStatus + ". Allowed statuses: UNDER_REVIEW, ACCEPTED, REJECTED, REVISION_REQUESTED");
        }

        if ("REVISION_REQUESTED".equals(targetStatus)) {
            if (request.getFeedback() == null || request.getFeedback().isBlank()) {
                throw new BadRequestException("Feedback instructions are required when requesting revisions");
            }
        }

        application.setStatus(targetStatus);
        if (request.getFeedback() != null) application.setFeedback(request.getFeedback());
        if (request.getReviewNotes() != null) application.setReviewNotes(request.getReviewNotes());

        application = applicationRepository.save(application);

        // Notify student about review decision
        String message = switch (targetStatus) {
            case "ACCEPTED" -> "Congratulations! Your research application for '" + application.getProject().getName() + "' has been accepted.";
            case "REJECTED" -> "Your research application for '" + application.getProject().getName() + "' was not accepted at this time.";
            case "REVISION_REQUESTED" -> "Faculty feedback on your application for '" + application.getProject().getName() + "': " + request.getFeedback();
            default -> "Your application status for '" + application.getProject().getName() + "' is now: " + targetStatus;
        };

        notificationService.createNotification(
                application.getApplicant(),
                "Research Application " + targetStatus,
                message,
                "RESEARCH_DECISION",
                String.valueOf(application.getId())
        );

        log.info("Research application id={} reviewed by teacherId={}, newStatus={}", id, teacher.getId(), targetStatus);
        return mapToApplicationResponse(application);
    }

    @Transactional
    public ResearchApplicationResponse withdrawApplication(Long id, String principalName) {
        User student = getUserByPrincipal(principalName);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research application not found with id: " + id));

        if (!application.getApplicant().getId().equals(student.getId())) {
            throw new AccessDeniedException("Unauthorized: You can only withdraw your own applications");
        }

        if (!"PENDING".equalsIgnoreCase(application.getStatus()) && !"APPLIED".equalsIgnoreCase(application.getStatus()) && !"UNDER_REVIEW".equalsIgnoreCase(application.getStatus())) {
            throw new BadRequestException("Cannot withdraw application in status: " + application.getStatus());
        }

        application.setStatus("WITHDRAWN");
        application = applicationRepository.save(application);
        log.info("Research application id={} withdrawn by studentId={}", id, student.getId());
        return mapToApplicationResponse(application);
    }

    @Transactional
    public ResearchApplicationResponse resubmitApplication(Long id, String principalName, ResearchApplicationResubmitRequest request) {
        User student = getUserByPrincipal(principalName);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research application not found with id: " + id));

        if (!application.getApplicant().getId().equals(student.getId())) {
            throw new AccessDeniedException("Unauthorized: You can only resubmit your own applications");
        }

        if (!"REVISION_REQUESTED".equalsIgnoreCase(application.getStatus())) {
            throw new BadRequestException("Application can only be resubmitted when status is REVISION_REQUESTED. Current status: " + application.getStatus());
        }

        if (application.getProject() != null && !"OPEN".equalsIgnoreCase(application.getProject().getStatus())) {
            throw new BadRequestException("Cannot resubmit: Research opportunity is no longer open");
        }

        if (application.getProject() != null && application.getProject().getDeadline() != null && LocalDateTime.now().isAfter(application.getProject().getDeadline())) {
            throw new BadRequestException("Cannot resubmit: The application deadline for this research opportunity has passed");
        }

        application.setStatement(request.getStatement());
        if (request.getSkills() != null) application.setApplicantSkills(request.getSkills());
        application.setStatus("PENDING");
        application = applicationRepository.save(application);

        // Notify teacher of resubmission
        if (application.getProject() != null && application.getProject().getOwner() != null) {
            notificationService.createNotification(
                    application.getProject().getOwner(),
                    "Research Application Resubmitted",
                    "Student " + student.getName() + " has resubmitted their application for '" + application.getProject().getName() + "'.",
                    "RESEARCH_RESUBMITTED",
                    String.valueOf(application.getId())
            );
        }

        log.info("Research application id={} resubmitted by studentId={}", id, student.getId());
        return mapToApplicationResponse(application);
    }

    // =========================================================================
    // Helper & Mapping Methods
    // =========================================================================

    private void verifyOpportunityOwnership(Project project, User caller) {
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Unauthorized: You can only manage research opportunities that belong to your account");
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

    private Long resolveUserIdSilently(String principalIdentifier) {
        if (principalIdentifier == null || principalIdentifier.isBlank()) return null;
        return userRepository.findByIdentifier(principalIdentifier).map(User::getId).orElse(null);
    }

    private ResearchOpportunityResponse mapToOpportunityResponse(Project p, Long callerUserId) {
        boolean isApplied = false;
        if (callerUserId != null) {
            isApplied = applicationRepository.existsByProjectIdAndApplicantId(p.getId(), callerUserId);
        }

        User owner = p.getOwner();
        Long ownerId = owner != null ? owner.getId() : null;
        String ownerName = owner != null ? owner.getName() : "KUET Faculty";
        String ownerRole = owner != null ? owner.getRole() : "TEACHER";

        return ResearchOpportunityResponse.builder()
                .id(p.getId())
                .title(p.getName())
                .name(p.getName())
                .description(p.getDescription())
                .category(p.getCategory())
                .requiredSkills(p.getSkillsRequired() != null ? p.getSkillsRequired() : p.getTechnologies())
                .technologies(p.getTechnologies())
                .preferredBackground(p.getPreferredBackground())
                .positionsCount(p.getPositionsCount())
                .deadline(p.getDeadline())
                .eligibilityCriteria(p.getEligibilityCriteria())
                .duration(p.getDuration())
                .researchType(p.getType())
                .type(p.getType())
                .status(p.getStatus())
                .minCgpa(p.getMinCgpa())
                .ownerDept(p.getOwnerDept())
                .paperDoi(p.getPaperDoi())
                .ownerId(ownerId)
                .ownerName(ownerName)
                .ownerRole(ownerRole)
                .isApplied(isApplied)
                .applicantsCount(p.getContributorsCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ResearchApplicationResponse mapToApplicationResponse(Application a) {
        Long oppId = a.getProject() != null ? a.getProject().getId() : null;
        String oppTitle = a.getProject() != null ? a.getProject().getName() : "Research Project";
        Long teacherId = (a.getProject() != null && a.getProject().getOwner() != null) ? a.getProject().getOwner().getId() : null;
        String teacherName = (a.getProject() != null && a.getProject().getOwner() != null) ? a.getProject().getOwner().getName() : "Faculty";

        return ResearchApplicationResponse.builder()
                .id(a.getId())
                .opportunityId(oppId)
                .opportunityTitle(oppTitle)
                .applicantId(a.getApplicant().getId())
                .applicantName(a.getApplicantName() != null ? a.getApplicantName() : a.getApplicant().getName())
                .applicantRoll(a.getApplicantRoll())
                .applicantDept(a.getApplicantDept())
                .applicantCgpa(a.getApplicantCgpa())
                .applicantSkills(a.getApplicantSkills())
                .statement(a.getStatement())
                .feedback(a.getFeedback())
                .reviewNotes(a.getReviewNotes())
                .status(a.getStatus())
                .teacherId(teacherId)
                .teacherName(teacherName)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
