package com.teligati.backend.service;

import com.teligati.backend.dto.AlumniDirectoryResponse;
import com.teligati.backend.dto.StudentDirectoryResponse;
import com.teligati.backend.dto.TeacherDirectoryResponse;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.User;
import com.teligati.backend.repository.ProfileRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DirectoryService {

    private final ProfileRepository profileRepository;

    public DirectoryService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public Page<StudentDirectoryResponse> getStudentDirectory(
            String name,
            String department,
            String batch,
            Pageable pageable
    ) {
        Specification<Profile> spec = (root, query, cb) -> {
            Join<Profile, User> userJoin = root.join("user", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(userJoin.get("role"), "ROLE_STUDENT"));

            if (name != null && !name.isBlank()) {
                String pattern = "%" + name.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(userJoin.get("name")), pattern),
                        cb.like(cb.lower(root.get("roll")), pattern),
                        cb.like(cb.lower(userJoin.get("kuetId")), pattern)
                ));
            }

            if (department != null && !department.isBlank()) {
                String deptPattern = "%" + department.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("department")), deptPattern),
                        cb.like(cb.lower(userJoin.get("department")), deptPattern)
                ));
            }

            if (batch != null && !batch.isBlank()) {
                String batchPattern = "%" + batch.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("batch")), batchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Profile> profiles = profileRepository.findAll(spec, pageable);
        return profiles.map(this::mapToStudentDirectoryResponse);
    }

    @Transactional(readOnly = true)
    public Page<AlumniDirectoryResponse> getAlumniDirectory(
            String name,
            String department,
            String batch,
            String company,
            String country,
            String position,
            Pageable pageable
    ) {
        Specification<Profile> spec = (root, query, cb) -> {
            Join<Profile, User> userJoin = root.join("user", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(userJoin.get("role"), "ROLE_ALUMNI"));

            if (name != null && !name.isBlank()) {
                String pattern = "%" + name.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(userJoin.get("name")), pattern)
                ));
            }

            if (department != null && !department.isBlank()) {
                String deptPattern = "%" + department.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("department")), deptPattern),
                        cb.like(cb.lower(userJoin.get("department")), deptPattern)
                ));
            }

            if (batch != null && !batch.isBlank()) {
                String batchPattern = "%" + batch.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("batch")), batchPattern));
            }

            if (company != null && !company.isBlank()) {
                String companyPattern = "%" + company.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("currentCompany")), companyPattern));
            }

            if (country != null && !country.isBlank()) {
                String countryPattern = "%" + country.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("country")), countryPattern));
            }

            if (position != null && !position.isBlank()) {
                String posPattern = "%" + position.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("currentPosition")), posPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Profile> profiles = profileRepository.findAll(spec, pageable);
        return profiles.map(this::mapToAlumniDirectoryResponse);
    }

    @Transactional(readOnly = true)
    public Page<TeacherDirectoryResponse> getTeacherDirectory(
            String name,
            String department,
            String designation,
            String researchFocus,
            Pageable pageable
    ) {
        Specification<Profile> spec = (root, query, cb) -> {
            Join<Profile, User> userJoin = root.join("user", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(userJoin.get("role"), "ROLE_TEACHER"));

            if (name != null && !name.isBlank()) {
                String pattern = "%" + name.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(userJoin.get("name")), pattern)
                ));
            }

            if (department != null && !department.isBlank()) {
                String deptPattern = "%" + department.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("department")), deptPattern),
                        cb.like(cb.lower(userJoin.get("department")), deptPattern)
                ));
            }

            if (designation != null && !designation.isBlank()) {
                String desigPattern = "%" + designation.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("designation")), desigPattern),
                        cb.like(cb.lower(root.get("currentPosition")), desigPattern)
                ));
            }

            if (researchFocus != null && !researchFocus.isBlank()) {
                String focusPattern = "%" + researchFocus.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("researchFocus")), focusPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Profile> profiles = profileRepository.findAll(spec, pageable);
        return profiles.map(this::mapToTeacherDirectoryResponse);
    }

    private StudentDirectoryResponse mapToStudentDirectoryResponse(Profile profile) {
        User user = profile.getUser();
        return StudentDirectoryResponse.builder()
                .userId(user.getId())
                .roll(profile.getRoll() != null ? profile.getRoll() : user.getKuetId())
                .name(profile.getName() != null ? profile.getName() : user.getName())
                .department(profile.getDepartment() != null ? profile.getDepartment() : user.getDepartment())
                .batch(profile.getBatch())
                .skills(profile.getSkills())
                .projects(profile.getProjects())
                .specialization(profile.getSpecialization())
                .clubs(profile.getClubs())
                .githubUrl(profile.getGithubUrl())
                .linkedinUrl(profile.getLinkedinUrl())
                .bio(profile.getBio())
                .isVerified(user.isVerified())
                .build();
    }

    private AlumniDirectoryResponse mapToAlumniDirectoryResponse(Profile profile) {
        User user = profile.getUser();
        return AlumniDirectoryResponse.builder()
                .userId(user.getId())
                .name(profile.getName() != null ? profile.getName() : user.getName())
                .department(profile.getDepartment() != null ? profile.getDepartment() : user.getDepartment())
                .batch(profile.getBatch())
                .graduationYear(profile.getGraduationYear())
                .currentCompany(profile.getCurrentCompany())
                .currentPosition(profile.getCurrentPosition())
                .country(profile.getCountry())
                .city(profile.getCity())
                .skills(profile.getSkills())
                .linkedinUrl(profile.getLinkedinUrl())
                .bio(profile.getBio())
                .isAvailableForMentorship(profile.isAvailableForMentorship())
                .isActivelyHiring(profile.isActivelyHiring())
                .isVerified(user.isVerified())
                .build();
    }

    private TeacherDirectoryResponse mapToTeacherDirectoryResponse(Profile profile) {
        User user = profile.getUser();
        return TeacherDirectoryResponse.builder()
                .userId(user.getId())
                .name(profile.getName() != null ? profile.getName() : user.getName())
                .department(profile.getDepartment() != null ? profile.getDepartment() : user.getDepartment())
                .designation(profile.getDesignation() != null ? profile.getDesignation() : profile.getCurrentPosition())
                .researchFocus(profile.getResearchFocus())
                .labName(profile.getLabName())
                .publicationsCount(profile.getPublicationsCount())
                .contactEmail(profile.getContactEmail() != null ? profile.getContactEmail() : user.getEmail())
                .bio(profile.getBio())
                .isVerified(user.isVerified())
                .build();
    }
}
