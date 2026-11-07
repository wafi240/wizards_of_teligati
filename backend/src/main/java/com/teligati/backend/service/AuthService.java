package com.teligati.backend.service;

import com.teligati.backend.dto.*;
import com.teligati.backend.entity.InvitationToken;
import com.teligati.backend.entity.Profile;
import com.teligati.backend.entity.RefreshToken;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.InvitationTokenRepository;
import com.teligati.backend.repository.ProfileRepository;
import com.teligati.backend.repository.RefreshTokenRepository;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       InvitationTokenRepository invitationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse registerStudent(StudentRegisterRequest request) {
        String roll = request.getRoll().trim();
        String email = request.getEmail().trim().toLowerCase();

        // 1. Roll number format: exactly 7 digits
        if (!roll.matches("^\\d{7}$")) {
            throw new BadRequestException("Student roll must be exactly 7 digits (e.g. 2307003)");
        }

        // 2. Institutional email domain verification
        if (!email.endsWith("kuet.ac.bd")) {
            throw new BadRequestException("Students must register with an institutional KUET email address (*@stud.kuet.ac.bd or *@kuet.ac.bd)");
        }

        // 3. Duplicate checks
        if (userRepository.existsByKuetId(roll)) {
            throw new BadRequestException("Student with roll " + roll + " is already registered");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered: " + email);
        }

        // 4. Create User entity with unverified status (requires email OTP or admin roster verification)
        User user = User.builder()
                .kuetId(roll)
                .email(email)
                .name(request.getName().trim())
                .department(request.getDepartment().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_STUDENT")
                .isVerified(false) // Not verified until ownership of institutional email/identity is proven
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        // 5. Initialize linked Profile entity
        Profile profile = Profile.builder()
                .user(user)
                .name(user.getName())
                .roll(user.getKuetId())
                .department(user.getDepartment())
                .status("Undergraduate Student")
                .build();
        profileRepository.save(profile);

        // 6. Generate access & refresh tokens
        return createTokenResponse(user, null);
    }

    @Transactional
    public TokenResponse registerAlumni(AlumniRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String tokenStr = request.getInvitationToken().trim();
        String tokenHash = JwtService.hashToken(tokenStr);

        // 1. Strict invitation token verification with pessimistic write lock (concurrency safe)
        InvitationToken invitation = invitationTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid alumni invitation token"));

        if (invitation.isUsed()) {
            throw new BadRequestException("Invitation token has already been claimed");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invitation token has expired");
        }
        if (!"ROLE_ALUMNI".equalsIgnoreCase(invitation.getRole())) {
            throw new BadRequestException("Provided invitation token is not valid for alumni registration");
        }
        if (invitation.getEmail() != null && !invitation.getEmail().isBlank()
                && !invitation.getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Invitation token was issued for a different email address");
        }

        // 2. Duplicate check
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered: " + email);
        }

        // 3. Atomically mark invitation token as claimed
        invitation.setUsed(true);
        invitationTokenRepository.save(invitation);

        // 4. Create User entity with verified Alumni role
        User user = User.builder()
                .email(email)
                .name(request.getName().trim())
                .department(request.getDepartment().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_ALUMNI")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        // 5. Initialize linked Profile
        Profile profile = Profile.builder()
                .user(user)
                .name(user.getName())
                .department(user.getDepartment())
                .status("Alumni")
                .isAvailableForMentorship(true)
                .build();
        profileRepository.save(profile);

        return createTokenResponse(user, null);
    }

    @Transactional
    public TokenResponse registerFaculty(FacultyRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String tokenStr = request.getInvitationToken().trim();
        String tokenHash = JwtService.hashToken(tokenStr);

        // 1. Strict invitation token verification with pessimistic write lock (concurrency safe)
        InvitationToken invitation = invitationTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid faculty authorization token"));

        if (invitation.isUsed()) {
            throw new BadRequestException("Authorization token has already been claimed");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Authorization token has expired");
        }
        if (!"ROLE_TEACHER".equalsIgnoreCase(invitation.getRole())) {
            throw new BadRequestException("Provided authorization token is not valid for faculty registration");
        }
        if (invitation.getEmail() != null && !invitation.getEmail().isBlank()
                && !invitation.getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Authorization token was issued for a different faculty email address");
        }

        // 2. Official institutional email check
        if (!email.endsWith("kuet.ac.bd")) {
            throw new BadRequestException("Faculty members must register with an official KUET email address (*@kuet.ac.bd)");
        }

        // 3. Duplicate check
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered: " + email);
        }

        // 4. Atomically mark token as claimed
        invitation.setUsed(true);
        invitationTokenRepository.save(invitation);

        // 5. Create User entity with verified Teacher role
        User user = User.builder()
                .email(email)
                .name(request.getName().trim())
                .department(request.getDepartment().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_TEACHER")
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        // 6. Initialize linked Profile
        String designation = request.getDesignation() != null && !request.getDesignation().isBlank()
                ? request.getDesignation().trim()
                : "Faculty Member";
        Profile profile = Profile.builder()
                .user(user)
                .name(user.getName())
                .department(user.getDepartment())
                .designation(designation)
                .currentPosition(designation)
                .status("Teacher")
                .contactEmail(user.getEmail())
                .build();
        profileRepository.save(profile);

        return createTokenResponse(user, null);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String deviceInfo) {
        String identifier = request.getIdentifier().trim();

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid identifier or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid identifier or password");
        }

        return createTokenResponse(user, deviceInfo);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public TokenResponse refreshToken(RefreshTokenRequest request, String deviceInfo) {
        String rawToken = request.getRefreshToken().trim();
        String tokenHash = JwtService.hashToken(rawToken);

        // Pessimistic row lock prevents concurrent requests from creating multiple valid successors
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        // Refresh Token Reuse Detection (Stolen Token Protection)
        if (tokenEntity.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(tokenEntity.getUser().getId());
            throw new BadRequestException("Token reuse detected. All active sessions have been revoked for your security.");
        }

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }

        // Rotate Refresh Token
        String newRawRefreshToken = jwtService.generateRefreshTokenString();
        String newHash = JwtService.hashToken(newRawRefreshToken);

        tokenEntity.setRevoked(true);
        tokenEntity.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(tokenEntity);

        RefreshToken newTokenEntity = RefreshToken.builder()
                .tokenHash(newHash)
                .user(tokenEntity.getUser())
                .deviceInfo(deviceInfo != null ? deviceInfo : tokenEntity.getDeviceInfo())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtService.getRefreshExpirationMs())))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newTokenEntity);

        String newAccessToken = jwtService.generateAccessToken(tokenEntity.getUser());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(mapToSummary(tokenEntity.getUser()))
                .build();
    }

    @Transactional
    public void logout(String rawRefreshToken, String authenticatedIdentifier) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = JwtService.hashToken(rawRefreshToken.trim());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                // Session isolation: ensure caller owns the token being revoked
                if (authenticatedIdentifier != null && !authenticatedIdentifier.isBlank()) {
                    User user = token.getUser();
                    boolean emailMatches = user.getEmail() != null && user.getEmail().equalsIgnoreCase(authenticatedIdentifier);
                    boolean rollMatches = user.getKuetId() != null && user.getKuetId().equalsIgnoreCase(authenticatedIdentifier);
                    if (!emailMatches && !rollMatches) {
                        throw new BadRequestException("Unauthorized attempt to revoke a session belonging to another user");
                    }
                }
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(String principalIdentifier) {
        User user = userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for: " + principalIdentifier));

        return mapToSummary(user);
    }

    @Transactional(readOnly = true)
    public VerificationStatusResponse getVerificationStatus(String principalIdentifier) {
        User user = userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for: " + principalIdentifier));

        boolean verified = user.isVerified();
        return VerificationStatusResponse.builder()
                .isVerified(verified)
                .identifier(user.getKuetId() != null ? user.getKuetId() : user.getEmail())
                .role(user.getRole())
                .status(verified ? "VERIFIED" : "UNVERIFIED")
                .message(verified
                        ? "Account identity has been fully verified."
                        : "Account is pending institutional verification. Verification via KUET email OTP or administrative roster approval is required for unrestricted platform access.")
                .restrictedActions(verified
                        ? List.of()
                        : List.of("APPLY_FOR_JOBS", "SUBMIT_RESEARCH_PROPOSALS", "REQUEST_ALUMNI_REFERENCES"))
                .build();
    }

    @Transactional
    public void verifyUser(String identifier) {
        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier));
        user.setVerified(true);
        userRepository.save(user);
    }

    private TokenResponse createTokenResponse(User user, String deviceInfo) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshTokenString();
        String tokenHash = JwtService.hashToken(rawRefreshToken);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .deviceInfo(deviceInfo)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtService.getRefreshExpirationMs())))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(mapToSummary(user))
                .build();
    }

    private UserSummaryResponse mapToSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .kuetId(user.getKuetId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .department(user.getDepartment())
                .isVerified(user.isVerified())
                .build();
    }
}
