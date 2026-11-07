package com.teligati.backend.config;

import com.teligati.backend.entity.InvitationToken;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.User;
import com.teligati.backend.repository.InvitationTokenRepository;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@org.springframework.context.annotation.Profile({"dev", "local", "test"})
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      ProfileRepository profileRepository,
                      InvitationTokenRepository invitationTokenRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsersAndProfiles();
        seedInvitationTokens();
    }

    private void seedUsersAndProfiles() {
        if (userRepository.count() > 0) {
            log.info("Database already seeded with users; skipping demo user seed.");
            return;
        }

        log.info("Seeding initial demo users and profiles for Wizards of Teligati...");

        // 1. Demo Student
        User student = User.builder()
                .kuetId("2307003")
                .email("wafi2307003@stud.kuet.ac.bd")
                .name("Wafi (Student)")
                .department("Computer Science and Engineering")
                .password(passwordEncoder.encode("demo"))
                .role("ROLE_STUDENT")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        student = userRepository.save(student);

        Profile studentProfile = Profile.builder()
                .user(student)
                .name(student.getName())
                .roll(student.getKuetId())
                .department(student.getDepartment())
                .batch("2023")
                .status("Undergraduate Student")
                .cgpa(3.92)
                .skills("Java, Android, Spring Boot, PostgreSQL, Docker")
                .projects("Wizards of Teligati, Autonomous Drone Pathfinder")
                .githubUrl("https://github.com/wafi2307003")
                .linkedinUrl("https://linkedin.com/in/wafi-kuet")
                .specialization("Distributed Systems & Mobile Architecture")
                .clubs("KUET Computer Club, Teligati Open Source")
                .bio("Undergraduate CSE student at KUET passionate about distributed systems and Android development.")
                .build();
        profileRepository.save(studentProfile);

        // 2. Demo Alumni
        User alumni = User.builder()
                .email("tanvir.munich@kuet.ac.bd")
                .name("Tanvir (Alumni)")
                .department("Computer Science and Engineering")
                .password(passwordEncoder.encode("demo"))
                .role("ROLE_ALUMNI")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        alumni = userRepository.save(alumni);

        Profile alumniProfile = Profile.builder()
                .user(alumni)
                .name(alumni.getName())
                .department(alumni.getDepartment())
                .batch("2016")
                .graduationYear("2020")
                .status("Alumni")
                .currentCompany("Tech Corp Munich")
                .currentPosition("Senior Software Engineer")
                .city("Munich")
                .country("Germany")
                .skills("System Architecture, Cloud Infrastructure, Go, Kubernetes, Spring")
                .linkedinUrl("https://linkedin.com/in/tanvir-munich")
                .isAvailableForMentorship(true)
                .isActivelyHiring(true)
                .bio("Senior Software Engineer in Munich, Germany. KUET '16 batch. Happy to provide mentorship and refer talented KUETians.")
                .build();
        profileRepository.save(alumniProfile);

        // 3. Demo Faculty Member
        User teacher = User.builder()
                .email("azharul@cse.kuet.ac.bd")
                .name("Dr. Azharul (Faculty)")
                .department("Computer Science and Engineering")
                .password(passwordEncoder.encode("demo"))
                .role("ROLE_TEACHER")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        teacher = userRepository.save(teacher);

        Profile teacherProfile = Profile.builder()
                .user(teacher)
                .name(teacher.getName())
                .department(teacher.getDepartment())
                .status("Teacher")
                .designation("Professor & Head, CSE")
                .currentPosition("Professor & Head, CSE")
                .researchFocus("Distributed AI, Fault-Tolerant Systems, Edge Computing")
                .labName("Intelligent Systems & Networks Laboratory")
                .publicationsCount(38)
                .contactEmail("azharul@cse.kuet.ac.bd")
                .bio("Professor at Department of CSE, KUET. Directing research in resilient decentralized architectures and machine intelligence.")
                .build();
        profileRepository.save(teacherProfile);

        log.info("Successfully seeded 3 demo accounts (Student, Alumni, Teacher) with password 'demo'.");
    }

    private void seedInvitationTokens() {
        if (invitationTokenRepository.count() > 0) {
            log.info("Invitation tokens already present; skipping seed.");
            return;
        }

        log.info("Seeding test invitation tokens for Alumni and Faculty registration...");

        InvitationToken alumniToken = InvitationToken.builder()
                .tokenHash(JwtService.hashToken("ALUMNI-KUET-2026-TOKEN"))
                .email("verified.alumni@kuet.ac.bd")
                .role("ROLE_ALUMNI")
                .department("Computer Science and Engineering")
                .expiresAt(LocalDateTime.now().plusDays(90))
                .used(false)
                .build();
        invitationTokenRepository.save(alumniToken);

        InvitationToken facultyToken = InvitationToken.builder()
                .tokenHash(JwtService.hashToken("FACULTY-KUET-2026-TOKEN"))
                .email("verified.faculty@cse.kuet.ac.bd")
                .role("ROLE_TEACHER")
                .department("Computer Science and Engineering")
                .expiresAt(LocalDateTime.now().plusDays(90))
                .used(false)
                .build();
        invitationTokenRepository.save(facultyToken);

        log.info("Successfully seeded invitation tokens: ALUMNI-KUET-2026-TOKEN, FACULTY-KUET-2026-TOKEN.");
    }
}
