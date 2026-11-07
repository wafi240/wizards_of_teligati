package com.teligati.backend.service;

import com.teligati.backend.dto.ProfileResponse;
import com.teligati.backend.dto.ProfileUpdateRequest;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public ProfileService(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String principalIdentifier) {
        User user = getUserByPrincipal(principalIdentifier);
        Profile profile = getOrCreateProfileForUser(user);
        return mapToProfileResponse(profile, user, true);
    }

    @Transactional
    public ProfileResponse updateMyProfile(String principalIdentifier, ProfileUpdateRequest request) {
        User user = getUserByPrincipal(principalIdentifier);
        Profile profile = getOrCreateProfileForUser(user);

        String userRole = user.getRole();
        boolean isStudent = "ROLE_STUDENT".equalsIgnoreCase(userRole);
        boolean isAlumni = "ROLE_ALUMNI".equalsIgnoreCase(userRole);
        boolean isTeacher = "ROLE_TEACHER".equalsIgnoreCase(userRole);

        // 1. Role-specific attribute authorization checks
        if (!isStudent) {
            if (request.getCgpa() != null) {
                throw new BadRequestException("Only students are authorized to update CGPA");
            }
            if (request.getSpecialization() != null && !request.getSpecialization().isBlank()) {
                throw new BadRequestException("Only students are authorized to update student specialization");
            }
            if (request.getClubs() != null && !request.getClubs().isBlank()) {
                throw new BadRequestException("Only students are authorized to update student club affiliations");
            }
        }

        if (!isTeacher) {
            if (request.getDesignation() != null && !request.getDesignation().isBlank()) {
                throw new BadRequestException("Only faculty members are authorized to update academic designation");
            }
            if (request.getResearchFocus() != null && !request.getResearchFocus().isBlank()) {
                throw new BadRequestException("Only faculty members are authorized to update research focus areas");
            }
            if (request.getLabName() != null && !request.getLabName().isBlank()) {
                throw new BadRequestException("Only faculty members are authorized to update laboratory affiliation");
            }
            if (request.getPublicationsCount() != null) {
                throw new BadRequestException("Only faculty members are authorized to update publications count");
            }
            if (request.getContactEmail() != null && !request.getContactEmail().isBlank()) {
                throw new BadRequestException("Only faculty members are authorized to update faculty contact email");
            }
        }

        if (!isAlumni) {
            if (request.getIsActivelyHiring() != null) {
                throw new BadRequestException("Only alumni are authorized to update hiring status");
            }
        }

        // 2. Update basic user/profile info if supplied
        if (request.getName() != null && !request.getName().isBlank()) {
            String updatedName = request.getName().trim();
            user.setName(updatedName);
            profile.setName(updatedName);
        }

        if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
            String updatedDept = request.getDepartment().trim();
            user.setDepartment(updatedDept);
            profile.setDepartment(updatedDept);
        }

        // 3. Profile common metadata
        if (request.getBatch() != null) profile.setBatch(request.getBatch().trim());
        if (request.getGraduationYear() != null) profile.setGraduationYear(request.getGraduationYear().trim());
        if (request.getStatus() != null) profile.setStatus(request.getStatus().trim());
        if (request.getBio() != null) profile.setBio(request.getBio().trim());
        if (request.getSkills() != null) profile.setSkills(request.getSkills().trim());
        if (request.getProjects() != null) profile.setProjects(request.getProjects().trim());
        if (request.getGithubUrl() != null) profile.setGithubUrl(request.getGithubUrl().trim());
        if (request.getLinkedinUrl() != null) profile.setLinkedinUrl(request.getLinkedinUrl().trim());

        // 4. Location and employment
        if (request.getCurrentCompany() != null) profile.setCurrentCompany(request.getCurrentCompany().trim());
        if (request.getCurrentPosition() != null) profile.setCurrentPosition(request.getCurrentPosition().trim());
        if (request.getCountry() != null) profile.setCountry(request.getCountry().trim());
        if (request.getCity() != null) profile.setCity(request.getCity().trim());
        if (request.getIsAvailableForMentorship() != null) {
            profile.setAvailableForMentorship(request.getIsAvailableForMentorship());
        }

        // 5. Student specific fields (guarded by isStudent)
        if (isStudent) {
            if (request.getCgpa() != null) profile.setCgpa(request.getCgpa());
            if (request.getSpecialization() != null) profile.setSpecialization(request.getSpecialization().trim());
            if (request.getClubs() != null) profile.setClubs(request.getClubs().trim());
        }

        // 6. Faculty specific fields (guarded by isTeacher)
        if (isTeacher) {
            if (request.getDesignation() != null) {
                profile.setDesignation(request.getDesignation().trim());
                if (profile.getCurrentPosition() == null || profile.getCurrentPosition().isBlank()) {
                    profile.setCurrentPosition(request.getDesignation().trim());
                }
            }
            if (request.getResearchFocus() != null) profile.setResearchFocus(request.getResearchFocus().trim());
            if (request.getLabName() != null) profile.setLabName(request.getLabName().trim());
            if (request.getPublicationsCount() != null) profile.setPublicationsCount(request.getPublicationsCount());
            if (request.getContactEmail() != null) profile.setContactEmail(request.getContactEmail().trim());
        }

        // 7. Alumni specific fields (guarded by isAlumni)
        if (isAlumni) {
            if (request.getIsActivelyHiring() != null) {
                profile.setActivelyHiring(request.getIsActivelyHiring());
            }
        }

        // SECURITY: user.role and user.isVerified are NOT altered here under any circumstances!
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        profile = profileRepository.save(profile);

        return mapToProfileResponse(profile, user, true);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(Long targetUserId, String callerIdentifier) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        Profile profile = getOrCreateProfileForUser(targetUser);

        User caller = getUserByPrincipal(callerIdentifier);
        boolean isOwner = caller.getId().equals(targetUser.getId());

        // If not owner, sensitive academic fields like CGPA are hidden
        return mapToProfileResponse(profile, targetUser, isOwner);
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for principal: " + principalIdentifier));
    }

    private Profile getOrCreateProfileForUser(User user) {
        return profileRepository.findByUser(user)
                .orElseGet(() -> {
                    Profile newProfile = Profile.builder()
                            .user(user)
                            .name(user.getName())
                            .roll(user.getKuetId())
                            .department(user.getDepartment())
                            .status(user.getRole())
                            .build();
                    return profileRepository.save(newProfile);
                });
    }

    public static ProfileResponse mapToProfileResponse(Profile profile, User user, boolean includePrivateFields) {
        return ProfileResponse.builder()
                .userId(user.getId())
                .kuetId(user.getKuetId())
                .email(user.getEmail())
                .name(profile.getName() != null ? profile.getName() : user.getName())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .department(profile.getDepartment() != null ? profile.getDepartment() : user.getDepartment())
                .batch(profile.getBatch())
                .graduationYear(profile.getGraduationYear())
                .status(profile.getStatus())
                .currentCompany(profile.getCurrentCompany())
                .currentPosition(profile.getCurrentPosition() != null ? profile.getCurrentPosition() : profile.getDesignation())
                .country(profile.getCountry())
                .city(profile.getCity())
                .isAvailableForMentorship(profile.isAvailableForMentorship())
                .bio(profile.getBio())
                .skills(profile.getSkills())
                .projects(profile.getProjects())
                .githubUrl(profile.getGithubUrl())
                .linkedinUrl(profile.getLinkedinUrl())
                .cgpa(includePrivateFields ? profile.getCgpa() : null) // Academic privacy
                .specialization(profile.getSpecialization())
                .clubs(profile.getClubs())
                .designation(profile.getDesignation() != null ? profile.getDesignation() : profile.getCurrentPosition())
                .researchFocus(profile.getResearchFocus())
                .labName(profile.getLabName())
                .publicationsCount(profile.getPublicationsCount())
                .contactEmail(profile.getContactEmail() != null ? profile.getContactEmail() : user.getEmail())
                .isActivelyHiring(profile.isActivelyHiring())
                .build();
    }
}
