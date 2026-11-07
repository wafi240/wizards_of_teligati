package com.teligati.backend.service;

import com.teligati.backend.dto.JobRequest;
import com.teligati.backend.dto.JobResponse;
import com.teligati.backend.dto.PagedResponse;
import com.teligati.backend.entity.Job;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ApplicationRepository;
import com.teligati.backend.repository.JobRepository;
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
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public JobService(JobRepository jobRepository,
                      UserRepository userRepository,
                      ApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public JobResponse createJob(String principalIdentifier, JobRequest request) {
        User user = getUserByPrincipal(principalIdentifier);

        String category = request.getCategory();
        if (category == null || category.isBlank()) {
            category = "FULLTIME_JOBS";
        }

        // Role authorization check: Students cannot create full-time professional job postings
        if ("FULLTIME_JOBS".equalsIgnoreCase(category) && "ROLE_STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new BadRequestException("Students are not authorized to post full-time professional jobs");
        }

        String org = request.getOrganization() != null && !request.getOrganization().isBlank()
                ? request.getOrganization().trim()
                : (request.getCompany() != null ? request.getCompany().trim() : "KUET Network");

        String comp = request.getCompany() != null && !request.getCompany().isBlank()
                ? request.getCompany().trim()
                : org;

        Job job = Job.builder()
                .title(request.getTitle().trim())
                .company(comp)
                .organization(org)
                .location(request.getLocation() != null ? request.getLocation().trim() : "Remote / KUET")
                .type(request.getType() != null ? request.getType().trim() : "Contract / Full-time")
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .compensation(request.getCompensation() != null ? request.getCompensation().trim() : "Negotiable")
                .category(category)
                .duration(request.getDuration() != null ? request.getDuration().trim() : "Flexible")
                .skillsRequired(request.getSkillsRequired() != null ? request.getSkillsRequired().trim() : "")
                .minCgpa(request.getMinCgpa() != null ? request.getMinCgpa() : 0.0)
                .status("OPEN")
                .postedBy(user)
                .build();

        Job saved = jobRepository.save(job);
        return mapToJobResponse(saved, user.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobResponse> getJobs(String category, String status, String search,
                                              String principalIdentifier, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim() : "OPEN";

        Page<Job> jobPage = jobRepository.searchJobs(cleanCategory, cleanStatus, cleanSearch, pageable);

        Long currentUserId = resolveUserIdSilently(principalIdentifier);

        List<JobResponse> content = jobPage.getContent().stream()
                .map(job -> mapToJobResponse(job, currentUserId))
                .collect(Collectors.toList());

        return PagedResponse.<JobResponse>builder()
                .content(content)
                .page(jobPage.getNumber())
                .size(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .last(jobPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId, String principalIdentifier) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        Long currentUserId = resolveUserIdSilently(principalIdentifier);
        return mapToJobResponse(job, currentUserId);
    }

    @Transactional
    public JobResponse updateJob(Long jobId, String principalIdentifier, JobRequest request) {
        User caller = getUserByPrincipal(principalIdentifier);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        verifyJobOwnership(job, caller);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            job.setTitle(request.getTitle().trim());
        }
        if (request.getCompany() != null) {
            job.setCompany(request.getCompany().trim());
        }
        if (request.getOrganization() != null) {
            job.setOrganization(request.getOrganization().trim());
        }
        if (request.getLocation() != null) {
            job.setLocation(request.getLocation().trim());
        }
        if (request.getType() != null) {
            job.setType(request.getType().trim());
        }
        if (request.getDescription() != null) {
            job.setDescription(request.getDescription().trim());
        }
        if (request.getCompensation() != null) {
            job.setCompensation(request.getCompensation().trim());
        }
        if (request.getDuration() != null) {
            job.setDuration(request.getDuration().trim());
        }
        if (request.getSkillsRequired() != null) {
            job.setSkillsRequired(request.getSkillsRequired().trim());
        }
        if (request.getMinCgpa() != null) {
            job.setMinCgpa(request.getMinCgpa());
        }

        Job updated = jobRepository.save(job);
        return mapToJobResponse(updated, caller.getId());
    }

    @Transactional
    public JobResponse closeJob(Long jobId, String principalIdentifier) {
        User caller = getUserByPrincipal(principalIdentifier);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        verifyJobOwnership(job, caller);
        job.setStatus("CLOSED");
        Job updated = jobRepository.save(job);
        return mapToJobResponse(updated, caller.getId());
    }

    @Transactional
    public void deleteJob(Long jobId, String principalIdentifier) {
        User caller = getUserByPrincipal(principalIdentifier);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        verifyJobOwnership(job, caller);
        jobRepository.delete(job);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobResponse> getMyJobs(String principalIdentifier, int page, int size) {
        User caller = getUserByPrincipal(principalIdentifier);
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Job> jobPage = jobRepository.findByPostedById(caller.getId(), pageable);

        List<JobResponse> content = jobPage.getContent().stream()
                .map(job -> mapToJobResponse(job, caller.getId()))
                .collect(Collectors.toList());

        return PagedResponse.<JobResponse>builder()
                .content(content)
                .page(jobPage.getNumber())
                .size(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .last(jobPage.isLast())
                .build();
    }

    private void verifyJobOwnership(Job job, User caller) {
        boolean isOwner = job.getPostedBy() != null && job.getPostedBy().getId().equals(caller.getId());
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(caller.getRole());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to modify this job posting");
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

    private JobResponse mapToJobResponse(Job job, Long currentUserId) {
        boolean isApplied = false;
        if (currentUserId != null) {
            isApplied = applicationRepository.existsByJobIdAndApplicantId(job.getId(), currentUserId);
        }

        User poster = job.getPostedBy();
        Long posterId = poster != null ? poster.getId() : null;
        String posterName = poster != null ? poster.getName() : "KUET Member";
        String posterRole = poster != null ? poster.getRole() : "MEMBER";

        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .organization(job.getOrganization())
                .location(job.getLocation())
                .type(job.getType())
                .description(job.getDescription())
                .compensation(job.getCompensation())
                .category(job.getCategory())
                .duration(job.getDuration())
                .skillsRequired(job.getSkillsRequired())
                .minCgpa(job.getMinCgpa())
                .status(job.getStatus())
                .postedByUserId(posterId)
                .postedByName(posterName)
                .posterRole(posterRole)
                .isApplied(isApplied)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
