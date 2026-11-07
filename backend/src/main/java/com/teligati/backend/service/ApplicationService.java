package com.teligati.backend.service;

import com.teligati.backend.dto.ApplicationRequest;
import com.teligati.backend.dto.ApplicationResponse;
import com.teligati.backend.dto.ApplicationStatusUpdateRequest;
import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.entity.Application;
import com.teligati.backend.entity.Job;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.Project;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ApplicationRepository;
import com.teligati.backend.repository.JobRepository;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.ProjectRepository;
import com.teligati.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              ProjectRepository projectRepository,
                              UserRepository userRepository,
                              ProfileRepository profileRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public ApplicationResponse submitApplication(String principalIdentifier, ApplicationRequest request) {
        User applicant = getUserByPrincipal(principalIdentifier);

        // Institutional verification requirement
        if (!applicant.isVerified()) {
            throw new BadRequestException("Your account is pending institutional email verification. Applying for opportunities is restricted until verified.");
        }

        String oppType = request.getOpportunityType() != null ? request.getOpportunityType().trim().toUpperCase() : "";
        if (!"JOB".equals(oppType) && !"PROJECT".equals(oppType)) {
            throw new BadRequestException("Opportunity type must be 'JOB' or 'PROJECT'");
        }

        Job job = null;
        Project project = null;

        if ("JOB".equals(oppType)) {
            if (request.getJobId() == null) {
                throw new BadRequestException("jobId is required when opportunityType is JOB");
            }
            job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with id: " + request.getJobId()));

            if (!"OPEN".equalsIgnoreCase(job.getStatus())) {
                throw new BadRequestException("This job posting is closed and no longer accepting applications");
            }

            // Prevent self-application
            if (job.getPostedBy() != null && job.getPostedBy().getId().equals(applicant.getId())) {
                throw new BadRequestException("You cannot apply to your own job posting");
            }

            // Prevent duplicate applications
            if (applicationRepository.existsByJobIdAndApplicantId(job.getId(), applicant.getId())) {
                throw new BadRequestException("You have already applied for this position");
            }

        } else {
            if (request.getProjectId() == null) {
                throw new BadRequestException("projectId is required when opportunityType is PROJECT");
            }
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Research project not found with id: " + request.getProjectId()));

            if (!"OPEN".equalsIgnoreCase(project.getStatus()) || !project.isOpenForContribution()) {
                throw new BadRequestException("This research project is currently closed for applications or collaboration");
            }

            // Prevent self-application
            if (project.getOwner() != null && project.getOwner().getId().equals(applicant.getId())) {
                throw new BadRequestException("You cannot apply to your own research project");
            }

            // Prevent duplicate applications
            if (applicationRepository.existsByProjectIdAndApplicantId(project.getId(), applicant.getId())) {
                throw new BadRequestException("You have already expressed interest in this research project");
            }
        }

        // Pull applicant metadata from profile if available
        Profile profile = profileRepository.findByUser(applicant).orElse(null);
        Double cgpa = profile != null ? profile.getCgpa() : 0.0;
        String skills = profile != null && profile.getSkills() != null ? profile.getSkills() : "KUET Scholar";
        String roll = applicant.getKuetId() != null ? applicant.getKuetId() : "N/A";
        String dept = applicant.getDepartment() != null ? applicant.getDepartment() : (profile != null ? profile.getDepartment() : "KUET");

        Application application = Application.builder()
                .opportunityType(oppType)
                .job(job)
                .project(project)
                .applicant(applicant)
                .statement(request.getStatement() != null ? request.getStatement().trim() : "Standard application statement.")
                .status("APPLIED")
                .applicantName(applicant.getName())
                .applicantRoll(roll)
                .applicantDept(dept)
                .applicantCgpa(cgpa)
                .applicantSkills(skills)
                .build();

        Application saved = applicationRepository.save(application);
        return mapToApplicationResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getMyApplications(String principalIdentifier, int page, int size) {
        User applicant = getUserByPrincipal(principalIdentifier);
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Application> appPage = applicationRepository.findByApplicantId(applicant.getId(), pageable);

        List<ApplicationResponse> content = appPage.getContent().stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ApplicationResponse>builder()
                .content(content)
                .page(appPage.getNumber())
                .size(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getReceivedApplications(String principalIdentifier, int page, int size) {
        User owner = getUserByPrincipal(principalIdentifier);
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Application> appPage = applicationRepository.findAllReceivedApplications(owner.getId(), pageable);

        List<ApplicationResponse> content = appPage.getContent().stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ApplicationResponse>builder()
                .content(content)
                .page(appPage.getNumber())
                .size(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getApplicationsForJob(Long jobId, String principalIdentifier, int page, int size) {
        User caller = getUserByPrincipal(principalIdentifier);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with id: " + jobId));

        boolean isOwner = job.getPostedBy() != null && job.getPostedBy().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view applications for this job");
        }

        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Application> appPage = applicationRepository.findByJobId(jobId, pageable);

        List<ApplicationResponse> content = appPage.getContent().stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ApplicationResponse>builder()
                .content(content)
                .page(appPage.getNumber())
                .size(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getApplicationsForProject(Long projectId, String principalIdentifier, int page, int size) {
        User caller = getUserByPrincipal(principalIdentifier);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Research project not found with id: " + projectId));

        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view applications for this project");
        }

        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Application> appPage = applicationRepository.findByProjectId(projectId, pageable);

        List<ApplicationResponse> content = appPage.getContent().stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ApplicationResponse>builder()
                .content(content)
                .page(appPage.getNumber())
                .size(appPage.getSize())
                .totalElements(appPage.getTotalElements())
                .totalPages(appPage.getTotalPages())
                .last(appPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long applicationId, String principalIdentifier) {
        User caller = getUserByPrincipal(principalIdentifier);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        boolean isApplicant = application.getApplicant().getId().equals(caller.getId());
        boolean isOwner = isOpportunityOwner(application, caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());

        if (!isApplicant && !isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view this application");
        }

        return mapToApplicationResponse(application);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, String principalIdentifier, ApplicationStatusUpdateRequest request) {
        User caller = getUserByPrincipal(principalIdentifier);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        String targetStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";
        String currentStatus = application.getStatus();

        boolean isApplicant = application.getApplicant().getId().equals(caller.getId());
        boolean isOwner = isOpportunityOwner(application, caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());

        if (!isApplicant && !isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to update this application");
        }

        // Applicant can only withdraw
        if (isApplicant && !isOwner && !isAdmin) {
            if (!"WITHDRAWN".equals(targetStatus)) {
                throw new BadRequestException("Applicants can only withdraw their application");
            }
            if ("ACCEPTED".equals(currentStatus) || "REJECTED".equals(currentStatus) || "WITHDRAWN".equals(currentStatus)) {
                throw new BadRequestException("Cannot withdraw an application in status: " + currentStatus);
            }
            application.setStatus("WITHDRAWN");
            return mapToApplicationResponse(applicationRepository.save(application));
        }

        // Owner/Admin state transitions
        if ("WITHDRAWN".equals(currentStatus)) {
            throw new BadRequestException("Cannot update an application that has been withdrawn by the applicant");
        }
        if ("REJECTED".equals(currentStatus)) {
            throw new BadRequestException("Cannot update an application that has already been rejected");
        }

        if (!"SHORTLISTED".equals(targetStatus) && !"ACCEPTED".equals(targetStatus) && !"REJECTED".equals(targetStatus)) {
            throw new BadRequestException("Invalid status update for posting owner. Allowed: SHORTLISTED, ACCEPTED, REJECTED");
        }

        application.setStatus(targetStatus);
        return mapToApplicationResponse(applicationRepository.save(application));
    }

    private boolean isOpportunityOwner(Application app, Long userId) {
        if ("JOB".equals(app.getOpportunityType()) && app.getJob() != null && app.getJob().getPostedBy() != null) {
            return app.getJob().getPostedBy().getId().equals(userId);
        }
        if ("PROJECT".equals(app.getOpportunityType()) && app.getProject() != null && app.getProject().getOwner() != null) {
            return app.getProject().getOwner().getId().equals(userId);
        }
        return false;
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }

    private ApplicationResponse mapToApplicationResponse(Application app) {
        String jobTitle = app.getJob() != null ? app.getJob().getTitle() : null;
        Long jobId = app.getJob() != null ? app.getJob().getId() : null;
        String projTitle = app.getProject() != null ? app.getProject().getName() : null;
        Long projId = app.getProject() != null ? app.getProject().getId() : null;

        return ApplicationResponse.builder()
                .id(app.getId())
                .opportunityType(app.getOpportunityType())
                .jobId(jobId)
                .jobTitle(jobTitle)
                .projectId(projId)
                .projectTitle(projTitle)
                .applicantId(app.getApplicant().getId())
                .applicantName(app.getApplicantName())
                .applicantRoll(app.getApplicantRoll())
                .applicantDept(app.getApplicantDept())
                .applicantCgpa(app.getApplicantCgpa())
                .applicantSkills(app.getApplicantSkills())
                .statement(app.getStatement())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
