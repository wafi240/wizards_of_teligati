package com.teligati.backend.service;

import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.dto.ProjectRequest;
import com.teligati.backend.dto.ProjectResponse;
import com.teligati.backend.dto.ProposalDecisionRequest;
import com.teligati.backend.dto.ResearchProposalRequest;
import com.teligati.backend.dto.ResearchProposalResponse;
import com.teligati.backend.entity.Project;
import com.teligati.backend.entity.ResearchProposal;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ApplicationRepository;
import com.teligati.backend.repository.ProjectRepository;
import com.teligati.backend.repository.ResearchProposalRepository;
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
public class ResearchService {

    private final ProjectRepository projectRepository;
    private final ResearchProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public ResearchService(ProjectRepository projectRepository,
                           ResearchProposalRepository proposalRepository,
                           UserRepository userRepository,
                           ApplicationRepository applicationRepository) {
        this.projectRepository = projectRepository;
        this.proposalRepository = proposalRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    // ==========================================
    // Research Opportunities & Projects
    // ==========================================

    @Transactional
    public ProjectResponse createProject(String principalIdentifier, ProjectRequest request) {
        User user = getUserByPrincipal(principalIdentifier);

        // Verification check
        if (!user.isVerified()) {
            throw new BadRequestException("Only verified academic accounts can publish research opportunities");
        }

        String projectType = request.getType() != null && !request.getType().isBlank()
                ? request.getType().trim()
                : "Research Opportunity";

        String ownerDept = request.getOwnerDept() != null && !request.getOwnerDept().isBlank()
                ? request.getOwnerDept().trim()
                : (user.getDepartment() != null ? user.getDepartment() : "KUET");

        boolean isOpen = request.getIsOpenForContribution() != null
                ? request.getIsOpenForContribution()
                : true;

        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .technologies(request.getTechnologies() != null ? request.getTechnologies().trim() : "")
                .githubUrl(request.getGithubUrl() != null ? request.getGithubUrl().trim() : "")
                .isOpenForContribution(isOpen)
                .type(projectType)
                .ownerDept(ownerDept)
                .contributorsCount(1)
                .paperDoi(request.getPaperDoi() != null ? request.getPaperDoi().trim() : "")
                .status("OPEN")
                .owner(user)
                .build();

        Project saved = projectRepository.save(project);
        return mapToProjectResponse(saved, user.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> getProjects(String status, String type, String search,
                                                      String principalIdentifier, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanType = (type != null && !type.isBlank()) ? type.trim() : null;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim() : "OPEN";

        Page<Project> projectPage = projectRepository.searchProjects(cleanStatus, cleanType, cleanSearch, pageable);

        Long currentUserId = resolveUserIdSilently(principalIdentifier);

        List<ProjectResponse> content = projectPage.getContent().stream()
                .map(project -> mapToProjectResponse(project, currentUserId))
                .collect(Collectors.toList());

        return PagedResponse.<ProjectResponse>builder()
                .content(content)
                .page(projectPage.getNumber())
                .size(projectPage.getSize())
                .totalElements(projectPage.getTotalElements())
                .totalPages(projectPage.getTotalPages())
                .last(projectPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, String principalIdentifier) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Research project not found with id: " + projectId));

        Long currentUserId = resolveUserIdSilently(principalIdentifier);
        return mapToProjectResponse(project, currentUserId);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, String principalIdentifier, ProjectRequest request) {
        User caller = getUserByPrincipal(principalIdentifier);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Research project not found with id: " + projectId));

        verifyProjectOwnership(project, caller);

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription().trim());
        }
        if (request.getTechnologies() != null) {
            project.setTechnologies(request.getTechnologies().trim());
        }
        if (request.getGithubUrl() != null) {
            project.setGithubUrl(request.getGithubUrl().trim());
        }
        if (request.getIsOpenForContribution() != null) {
            project.setOpenForContribution(request.getIsOpenForContribution());
        }
        if (request.getType() != null) {
            project.setType(request.getType().trim());
        }
        if (request.getOwnerDept() != null) {
            project.setOwnerDept(request.getOwnerDept().trim());
        }
        if (request.getPaperDoi() != null) {
            project.setPaperDoi(request.getPaperDoi().trim());
        }

        Project updated = projectRepository.save(project);
        return mapToProjectResponse(updated, caller.getId());
    }

    @Transactional
    public void deleteProject(Long projectId, String principalIdentifier) {
        User caller = getUserByPrincipal(principalIdentifier);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Research project not found with id: " + projectId));

        verifyProjectOwnership(project, caller);
        projectRepository.delete(project);
    }

    // ==========================================
    // Research Proposals
    // ==========================================

    @Transactional
    public ResearchProposalResponse submitProposal(String principalIdentifier, ResearchProposalRequest request) {
        User student = getUserByPrincipal(principalIdentifier);

        // Verification check: only verified students can submit research proposals
        if (!student.isVerified()) {
            throw new BadRequestException("Your account is pending institutional verification. Research proposal submissions are restricted.");
        }

        User faculty = userRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Target faculty member not found with id: " + request.getFacultyId()));

        if (!"ROLE_TEACHER".equalsIgnoreCase(faculty.getRole())) {
            throw new BadRequestException("Target recipient must be an academic faculty member");
        }

        ResearchProposal proposal = ResearchProposal.builder()
                .topic(request.getTopic().trim())
                .proposalAbstract(request.getProposalAbstract().trim())
                .degreeType(request.getDegreeType().trim())
                .status("PENDING")
                .feedback("Under review by faculty advisor")
                .student(student)
                .faculty(faculty)
                .build();

        ResearchProposal saved = proposalRepository.save(proposal);
        return mapToProposalResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResearchProposalResponse> getMyProposals(String principalIdentifier, int page, int size) {
        User user = getUserByPrincipal(principalIdentifier);
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ResearchProposal> proposalPage;
        if ("ROLE_TEACHER".equalsIgnoreCase(user.getRole())) {
            proposalPage = proposalRepository.findByFacultyId(user.getId(), pageable);
        } else {
            proposalPage = proposalRepository.findByStudentId(user.getId(), pageable);
        }

        List<ResearchProposalResponse> content = proposalPage.getContent().stream()
                .map(this::mapToProposalResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ResearchProposalResponse>builder()
                .content(content)
                .page(proposalPage.getNumber())
                .size(proposalPage.getSize())
                .totalElements(proposalPage.getTotalElements())
                .totalPages(proposalPage.getTotalPages())
                .last(proposalPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ResearchProposalResponse getProposalById(Long proposalId, String principalIdentifier) {
        User caller = getUserByPrincipal(principalIdentifier);
        ResearchProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Research proposal not found with id: " + proposalId));

        boolean isStudentOwner = proposal.getStudent().getId().equals(caller.getId());
        boolean isFacultyTarget = proposal.getFaculty().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());

        if (!isStudentOwner && !isFacultyTarget && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view this research proposal");
        }

        return mapToProposalResponse(proposal);
    }

    @Transactional
    public ResearchProposalResponse decideProposal(Long proposalId, String principalIdentifier, ProposalDecisionRequest decision) {
        User caller = getUserByPrincipal(principalIdentifier);
        ResearchProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Research proposal not found with id: " + proposalId));

        boolean isFacultyTarget = proposal.getFaculty().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());

        if (!isFacultyTarget && !isAdmin) {
            throw new AccessDeniedException("Only the targeted faculty member is authorized to make decisions on this proposal");
        }

        String action = decision.getStatus() != null ? decision.getStatus().trim().toUpperCase() : "";
        if (!"ACCEPTED".equals(action) && !"REJECTED".equals(action) && !"REVISION_REQUESTED".equals(action)) {
            throw new BadRequestException("Invalid decision status. Allowed values: ACCEPTED, REJECTED, REVISION_REQUESTED");
        }

        proposal.setStatus(action);
        if (decision.getFeedback() != null && !decision.getFeedback().isBlank()) {
            proposal.setFeedback(decision.getFeedback().trim());
        }

        ResearchProposal updated = proposalRepository.save(proposal);
        return mapToProposalResponse(updated);
    }

    private void verifyProjectOwnership(Project project, User caller) {
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to modify this research project");
        }
    }

    private Long resolveUserIdSilently(String principalIdentifier) {
        if (principalIdentifier == null || principalIdentifier.isBlank()) {
            return null;
        }
        return userRepository.findByIdentifier(principalIdentifier)
                .map(User::getId)
                .orElse(null);
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }

    private ProjectResponse mapToProjectResponse(Project project, Long currentUserId) {
        boolean isApplied = false;
        if (currentUserId != null) {
            isApplied = applicationRepository.existsByProjectIdAndApplicantId(project.getId(), currentUserId);
        }

        User owner = project.getOwner();
        Long ownerId = owner != null ? owner.getId() : null;
        String ownerName = owner != null ? owner.getName() : "KUET Faculty";

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .technologies(project.getTechnologies())
                .githubUrl(project.getGithubUrl())
                .isOpenForContribution(project.isOpenForContribution())
                .type(project.getType())
                .ownerDept(project.getOwnerDept())
                .contributorsCount(project.getContributorsCount())
                .paperDoi(project.getPaperDoi())
                .status(project.getStatus())
                .ownerId(ownerId)
                .ownerName(ownerName)
                .isApplied(isApplied)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private ResearchProposalResponse mapToProposalResponse(ResearchProposal proposal) {
        return ResearchProposalResponse.builder()
                .id(proposal.getId())
                .topic(proposal.getTopic())
                .proposalAbstract(proposal.getProposalAbstract())
                .degreeType(proposal.getDegreeType())
                .status(proposal.getStatus())
                .feedback(proposal.getFeedback())
                .studentId(proposal.getStudent().getId())
                .studentName(proposal.getStudent().getName())
                .studentRoll(proposal.getStudent().getKuetId())
                .facultyId(proposal.getFaculty().getId())
                .facultyName(proposal.getFaculty().getName())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }
}
