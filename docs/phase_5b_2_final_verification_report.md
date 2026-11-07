# Wizards of Teligati — Phase 5B.2 Final Verification Gate Report

**Date:** November 6, 2026  
**Scope:** Phase 5B.2 Backend Authentication & User Registration  
**Component:** `backend/` (Spring Boot 3.2.4, Java 21)  

---

## 1. Executive Verdict

### **VERDICT: PASS WITH LIMITATIONS**

The backend authentication, registration gating, session management, token security, and database concurrency controls have passed all rigorous automated tests and verification gates without defects.

**Verdict Justification:**
- **PASS:** All server-side identity checks, BCrypt work factor 12 hashing, HMAC-SHA256 JWT access tokens, SHA-256 hashed rotating refresh tokens, session reuse detection, and multi-threaded pessimistic locking pass 100% of the 51 automated security test cases.
- **LIMITATIONS (Documented & Expected):**
  1. *Email OTP Verification Limitation:* Syntactic roll/domain checks enforce KUET format, but real-time email OTP verification is bounded by the absence of production SMTP relay credentials (e.g., KUET SMTP server, AWS SES, or SendGrid). Newly registered students correctly default to `isVerified: false` until an email relay is provisioned.
  2. *Stateless Access Token Revocation Limitation:* In accordance with stateless OAuth2/JWT design, revoked sessions invalidate the refresh token immediately, but active access tokens remain valid until their 15-minute expiration period elapses. Instantaneous sub-second revocation would require an external distributed cache (Redis blacklist).

---

## 2. Verified Security Controls

1. **Student Verification & Privilege Gating:**
   - Client requests cannot manipulate `isVerified`. The field is omitted from registration DTOs and hardcoded to `false` on the server during student registration.
   - Unverified students receive `403 FORBIDDEN` when accessing restricted platform endpoints protected by `@PreAuthorize("@userSecurity.isVerified(authentication)")`.
   - Verified student status can only be set by server-side verification logic.
2. **Production Fail-Safe Configuration:**
   - Production startup (`prod` profile) automatically validates configuration values via `ProductionSecurityValidator`.
   - Startup **fails fast** with an `IllegalStateException` if `JWT_SECRET` is missing, blank, equals the development default secret, or is under 256 bits (32 bytes).
   - Startup **fails fast** if `app.seeder.enabled=true` under the `prod` profile.
   - Demo credentials (`password: demo`) and development invitation tokens are strictly prevented from loading in production.
3. **Invitation Token Integrity:**
   - Raw tokens are never persisted in the database; only SHA-256 hashes (`token_hash`) are stored.
   - Generated using `java.security.SecureRandom` (32 bytes entropy, 64 hex characters).
   - Single-use claim is enforced atomically via pessimistic row locks (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
   - Multi-threaded concurrency tests prove that 5 simultaneous threads claiming the same invitation token result in exactly 1 success and 4 rejections.
   - Strict email binding prevents token theft across different email addresses.
4. **Refresh Token Integrity & Reuse Detection:**
   - Stored strictly as SHA-256 hashes in `refresh_tokens`.
   - Rotation is transactional.
   - Pessimistic write locks prevent race conditions from creating multiple active lineages from concurrent refresh calls.
   - Replaying a revoked refresh token immediately revokes **all** active sessions for that user ID (`revokeAllByUserId`).
5. **Session Isolation on Logout:**
   - `POST /api/v1/auth/logout` verifies that the caller owns the refresh token being revoked. Unauthorized attempts to revoke another user's session are rejected with `400 BAD_REQUEST`.
6. **Data Integrity & Duplicate Protection:**
   - Database unique constraints (`kuet_id`, `email`, `token_hash`) prevent duplicate users or token collisions.
   - `DataIntegrityViolationException` is intercepted by `GlobalExceptionHandler` returning `409 CONFLICT` rather than an unhandled 500 error.
7. **Password Exposure Prevention:**
   - `User.password` is annotated with `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`.
   - All response DTOs (`UserSummaryResponse`, `TokenResponse`, `VerificationStatusResponse`) omit passwords entirely. Reflected class fields confirm zero password exposure.

---

## 3. Remaining Limitations

1. **Absence of External SMTP Gateway:**
   - Real-time 6-digit OTP delivery to `@stud.kuet.ac.bd` requires an active SMTP provider or institutional relay credentials.
   - Until configured, students remain in `UNVERIFIED` state upon registration.
2. **PostgreSQL Container Runtime:**
   - Local automated tests run against H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`) because Docker Desktop is unavailable on the local development machine.
   - Native PostgreSQL runtime validation must be performed in the target staging/production environment.
3. **Stateless JWT Lifetime:**
   - Access tokens have a 15-minute lifespan. After logout, the refresh token is revoked immediately, but the access token remains mathematically valid until expiration unless a centralized token blacklist (e.g., Redis) is introduced.

---

## 4. Endpoint Authorization & Verification Matrix

| HTTP Method | Route | Authentication Required | Role Required | Verification Required | Description |
|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/register/student` | None (Public) | None | N/A | Registers student with 7-digit roll & KUET email. Creates account with `isVerified=false`. |
| `POST` | `/api/v1/auth/register/alumni` | None (Public) | None | Requires Token | Registers alumni; gated by single-use invitation token. |
| `POST` | `/api/v1/auth/register/faculty` | None (Public) | None | Requires Token | Registers faculty; gated by single-use authorization token. |
| `POST` | `/api/v1/auth/login` | None (Public) | None | N/A | Dual identifier (roll or institutional email) + password. |
| `POST` | `/api/v1/auth/refresh` | None (Public) | None | N/A | Rotates refresh token, issues new JWT, detects token reuse. |
| `POST` | `/api/v1/auth/logout` | Authenticated | Any | No | Revokes refresh token session; enforces token ownership. |
| `GET` | `/api/v1/auth/me` | Authenticated (`Bearer`) | Any | No | Returns current user profile summary including `isVerified`. |
| `GET` | `/api/v1/auth/verification-status` | Authenticated (`Bearer`) | Any | No | Returns verification status and pending verification actions. |
| `GET` | `/api/v1/auth/student/restricted-feature` | Authenticated (`Bearer`) | `ROLE_STUDENT` | **YES (`isVerified=true`)** | Restricted student operation. Unverified returns `403 FORBIDDEN`. |
| `GET` | `/api/v1/health` | None (Public) | None | No | System health and database connectivity check. |
| `GET` | `/api/jobs` | Authenticated (`Bearer`) | Any | No | Prototype job listing endpoint. |
| `GET` | `/api/profiles` | Authenticated (`Bearer`) | Any | No | Prototype profile listing endpoint. |
| `GET` | `/api/projects` | Authenticated (`Bearer`) | Any | No | Prototype project listing endpoint. |

---

## 5. Migration Status

- **Migration History:**
  - `V1__init_foundation.sql`: Base tables (`users`, `profiles`, `jobs`, `projects`, `transactions`).
  - `V2__auth_and_tokens.sql`: Authentication additions (`kuet_id`, `refresh_tokens`, `invitation_tokens`).
- **Persistent Database Status:**
  - `V2` has **never** been applied to an external persistent database. It was authored in Phase 5B.2 and has executed exclusively in in-memory test databases.
  - The migration history is clean and contains no legacy rollbacks or dirty states.
- **Schema & Entity Parity:**
  - `User.java` perfectly matches `users` table columns (`kuet_id`, `email`, `role`, `is_verified`, etc.).
  - `RefreshToken.java` matches `refresh_tokens` table columns (`token_hash`, `user_id`, `device_info`, `expires_at`, `revoked`, `replaced_by_token_hash`).
  - `InvitationToken.java` matches `invitation_tokens` table columns (`token_hash`, `email`, `role`, `department`, `expires_at`, `used`).
- **PostgreSQL Compatibility:**
  - All SQL constructs (`BIGINT GENERATED BY DEFAULT AS IDENTITY`, `VARCHAR(64)`, `TIMESTAMP WITHOUT TIME ZONE`, `FOREIGN KEY ... ON DELETE CASCADE`, `CREATE INDEX IF NOT EXISTS`) are native PostgreSQL 10+ compliant syntax.

---

## 6. Test Evidence

### Test Summary
- **Total Tests:** 51
- **Passed:** 51 (100%)
- **Failed:** 0
- **Skipped:** 0
- **Build Status:** `BUILD SUCCESSFUL` (all compilation, bootJar, and assemble tasks passed)
- **Execution Report Path:** `backend/build/reports/tests/test/index.html`

### Test Suite Coverage Breakdown

| Test Suite Class | Tests | Focus Area |
|---|---|---|
| `SecurityAuditTest` | 16 | Concurrency (multithreaded invitation & refresh), Production validator, Token replay detection, Session isolation, Unverified student gating, Password exclusion, Algorithm enforcement. |
| `AuthControllerTest` | 17 | REST endpoints, Dual-identifier login, Registration constraints, Token rotation, Logout behavior, Protected `/me`. |
| `JwtServiceTest` | 5 | Token generation, Claims resolution, Expiration detection, Secure random generation, SHA-256 token hashing. |
| `SecurityConfigTest` | 4 | CORS preflight, Unauthorized rejection, Public route access. |
| `HealthControllerTest` | 1 | Health endpoint live data source check. |
| `HealthControllerUnitTest` | 2 | MockMvc slice testing for healthy and degraded states. |
| `HealthServiceTest` | 2 | Service layer health calculation. |
| `GlobalExceptionHandlerTest` | 3 | Sanitized JSON error handling and validation error extraction. |
| `BackendApplicationTests` | 1 | Spring Boot application context load verification. |

### Concurrency & Production Profile Verification Notes:
- **True Concurrency:** Multi-threaded tests in `SecurityAuditTest` (`concurrentInvitationConsumption` and `concurrentRefreshRequests`) use `ExecutorService` and `CountDownLatch` to guarantee simultaneous parallel execution across threads, rather than sequential calls.
- **Production Context Loading:** `productionProfileContextStartupFailsWhenJwtSecretMissing` initializes a full `SpringApplication` instance with `prod` profile, verifying that Spring Boot fails startup when `JWT_SECRET` is missing.

---

## 7. Modified Files During Phase 5B.2

| File Path | Type | Modification Summary |
|---|---|---|
| `backend/build.gradle` | Configuration | Added JJWT 0.12.5 dependencies (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`). |
| `backend/src/main/resources/application.properties` | Configuration | Added default JWT properties and profile fallback. |
| `backend/src/main/resources/application-dev.properties` | Configuration | Configured development profile with `app.seeder.enabled=true`. |
| `backend/src/main/resources/application-prod.properties` | Configuration | Configured production profile with `app.seeder.enabled=false`, `app.jwt.secret=${JWT_SECRET:}`. |
| `backend/src/main/resources/db/migration/V2__auth_and_tokens.sql` | Migration | Schema for `kuet_id`, `refresh_tokens`, and hashed `invitation_tokens`. |
| `backend/src/main/java/com/teligati/backend/entity/User.java` | Entity | Added `kuetId`, `isVerified`, write-only `password`. |
| `backend/src/main/java/com/teligati/backend/entity/RefreshToken.java` | Entity | Entity for hashed refresh tokens with rotation and replacement tracking. |
| `backend/src/main/java/com/teligati/backend/entity/InvitationToken.java` | Entity | Entity for hashed single-use invitation tokens. |
| `backend/src/main/java/com/teligati/backend/repository/UserRepository.java` | Repository | Added `findByIdentifier`, `existsByKuetId`, `existsByEmail`. |
| `backend/src/main/java/com/teligati/backend/repository/RefreshTokenRepository.java` | Repository | Added `findByTokenHashForUpdate`, `revokeAllByUserId`. |
| `backend/src/main/java/com/teligati/backend/repository/InvitationTokenRepository.java` | Repository | Added `findByTokenHashForUpdate`. |
| `backend/src/main/java/com/teligati/backend/security/JwtService.java` | Security | Token signing, claims parsing, validation, SHA-256 hashing, and random token generation. |
| `backend/src/main/java/com/teligati/backend/security/JwtAuthenticationFilter.java` | Security | Bearer token authentication filter. |
| `backend/src/main/java/com/teligati/backend/security/UserSecurity.java` | Security | Evaluates `isVerified` status for method security expressions. |
| `backend/src/main/java/com/teligati/backend/security/SecurityConfig.java` | Security | Wired JWT filter, method security, and route permissions. |
| `backend/src/main/java/com/teligati/backend/config/ProductionSecurityValidator.java` | Security | Startup validator enforcing production secret key and seeder restrictions. |
| `backend/src/main/java/com/teligati/backend/config/DataSeeder.java` | Configuration | Development/test data seeder restricted to non-production environments. |
| `backend/src/main/java/com/teligati/backend/service/AuthService.java` | Service | Registration, login, token rotation, reuse detection, logout session isolation. |
| `backend/src/main/java/com/teligati/backend/controller/AuthController.java` | Controller | REST endpoints under `/api/v1/auth/**`. |
| `backend/src/main/java/com/teligati/backend/exception/GlobalExceptionHandler.java` | Exception Handling | Handled `BadCredentialsException` and `DataIntegrityViolationException`. |
| `backend/src/main/java/com/teligati/backend/dto/*` | DTOs | Request and response contracts (`LoginRequest`, `TokenResponse`, etc.). |
| `backend/src/test/java/com/teligati/backend/auth/AuthControllerTest.java` | Test | Integration tests for authentication endpoints. |
| `backend/src/test/java/com/teligati/backend/security/SecurityAuditTest.java` | Test | 16 comprehensive security audit test cases. |
| `backend/src/test/java/com/teligati/backend/security/JwtServiceTest.java` | Test | JWT unit tests. |

---

## 8. Android Client Scope Confirmation

**CONFIRMATION:**  
Zero Android files were modified during this phase. `git status` verifies that all Android source files, layouts, and Gradle scripts remain strictly unmodified from the previous approved phase.
