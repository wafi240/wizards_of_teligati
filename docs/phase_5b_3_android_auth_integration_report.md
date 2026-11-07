# Wizards of Teligati — Phase 5B.3: Android Authentication Integration Report

**Date:** November 6, 2026  
**Status:** Completed & Fully Verified  
**Scope:** Android Authentication Integration with Spring Boot Backend  

---

## 1. Executive Summary

Phase 5B.3 integrates the native Android Java application (`android/`) with the Spring Boot 3.x backend (`backend/`) authentication system completed and verified in Phase 5B.2.

All 8 backend authentication endpoints are connected through a type-safe networking layer built with **Retrofit 2.11.0** and **OkHttp 4.12.0**. Sensitive credentials and session tokens (JWT access and refresh tokens) are managed securely using **Android Keystore-backed `EncryptedSharedPreferences`**. Navigation and authorization strictly enforce authoritative backend roles (`ROLE_STUDENT`, `ROLE_ALUMNI`, `ROLE_TEACHER`), and students with `isVerified == false` are explicitly surfaced with verification-pending notices, preventing unverified access to sensitive domain operations without implementing fake email OTP verification.

Local Room database persistence was fully preserved and synchronized with remote user identity upon successful login or registration.

---

## 2. Architecture & Networking Layer

### 2.1 Configurable Base URL & Android Emulator Networking
- **Default Base URL:** `http://10.0.2.2:8080/` (standard Android emulator loopback alias to host machine).
- **Configuration:** Generated via `buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")` in `android/app/build.gradle.kts`.
- **Factory:** `ApiClient.java` manages Retrofit instances, allowing runtime or testing overrides (e.g., `MockWebServer`).
- **Timeouts:** 15-second connect, read, and write timeouts configured on the OkHttp client.

### 2.2 Token Storage & Keystore Security
- **`SessionManager.java`** was upgraded to use `EncryptedSharedPreferences` from `androidx.security:security-crypto:1.1.0-alpha06`.
- Hardware-backed master key generated via `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()`.
- Preference keys encrypted with `AES256_SIV`; preference values encrypted with `AES256_GCM`.
- Seamless fallback to standard private preferences in JVM unit testing environments where Android Keystore provider is not installed.
- Plaintext passwords and secret tokens are never written to Room or logs.

### 2.3 Transparent Token Interception & Rotation
- **`AuthInterceptor.java`:** Intercepts outbound HTTP requests. If a non-empty access token is present in `SessionManager` and the request does not already contain an `Authorization` header, it injects `Authorization: Bearer <accessToken>`. Also appends `Accept: application/json` and `User-Agent: WizardsOfTeligati-Android/1.0`.
- **`TokenAuthenticator.java`:** Implements `okhttp3.Authenticator` to handle HTTP 401 Unauthorized responses:
  - **Loop Guard:** Tracks prior response count via `response.priorResponse()`. Aborts retry if attempts >= 2 to strictly prevent infinite 401 loops.
  - **Auth Path Exclusion:** Bypasses authenticator on authentication endpoints (`/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/register`) to avoid recursive failures.
  - **Synchronized Concurrency Prevention:** Synchronizes on the authenticator instance. If another concurrent thread has already refreshed the access token in `SessionManager`, the pending request is immediately retried with the new token without sending redundant refresh requests.
  - **Synchronous Token Refresh:** Uses an independent, unauthenticated OkHttpClient to POST the refresh token to `/api/v1/auth/refresh`.
  - **Rotation & Revocation:** Upon 200 OK, updates `SessionManager` with rotated access and refresh tokens. On refresh failure (e.g., 400 Bad Request due to token reuse or expiration), terminates the session via `sessionManager.clearSession()`.

---

## 3. API Contract & Endpoint Implementation

The Android client integrates all 8 backend endpoints defined in `AuthController.java`:

| Endpoint | HTTP Method | Request DTO | Response DTO | Purpose & Handling |
|---|---|---|---|---|
| `/api/v1/auth/register/student` | POST | `StudentRegisterRequest` | `ApiResponse<TokenResponse>` | Student registration with roll, name, institutional email (`@stud.kuet.ac.bd`), dept, password. Sets `isVerified = false`. |
| `/api/v1/auth/register/alumni` | POST | `AlumniRegisterRequest` | `ApiResponse<TokenResponse>` | Alumni registration with name, institutional email, dept, password, and invitation token (`invitationToken`). |
| `/api/v1/auth/register/faculty` | POST | `FacultyRegisterRequest` | `ApiResponse<TokenResponse>` | Faculty registration with name, institutional email, dept, designation, password, and authorization token (`authorizationToken`). |
| `/api/v1/auth/login` | POST | `LoginRequest` | `ApiResponse<TokenResponse>` | Unified login endpoint supporting roll number, institutional email, or username. |
| `/api/v1/auth/refresh` | POST | `RefreshTokenRequest` | `ApiResponse<TokenResponse>` | Refreshes access token and rotates refresh token. Invoked automatically by `TokenAuthenticator`. |
| `/api/v1/auth/logout` | POST | `RefreshTokenRequest` | `ApiResponse<Void>` | Server-side session revocation. Clears local `SessionManager`. |
| `/api/v1/auth/me` | GET | *None* (Bearer Token) | `ApiResponse<UserSummaryResponse>` | Retrieves authenticated user profile and verifies server-side identity. |
| `/api/v1/auth/verification-status`| GET | *None* (Bearer Token) | `ApiResponse<VerificationStatusResponse>` | Queries institutional verification status and active restriction list. |

---

## 4. User Experience & Role Authorization

### 4.1 Authoritative Role Routing
- Navigation is determined by the authenticated user's backend authority (`ROLE_STUDENT`, `ROLE_ALUMNI`, `ROLE_TEACHER`), normalized in `SessionManager.normalizeRole(...)`:
  - `ROLE_STUDENT` $\rightarrow$ `STUDENT` $\rightarrow$ Student Dashboard & Navigation
  - `ROLE_ALUMNI` $\rightarrow$ `ALUMNI` $\rightarrow$ Alumni Dashboard & Navigation
  - `ROLE_TEACHER` $\rightarrow$ `TEACHER` $\rightarrow$ Faculty/Teacher Dashboard & Navigation
- In `MainActivity.java`, session credentials take precedence over Intent extras, ensuring clients cannot forge a role transition without valid credentials.

### 4.2 Explicit Student Verification Pending UX
- Newly registered students receive `isVerified = false` from the backend.
- **Visual Status:**
  - Header name badge renders: `Samun Sadab Wafi ⏳ Verification Pending`
  - Warning banner card (`cardVerificationNotice`) is surfaced prominently on the Student Dashboard:
    > **⏳ Institutional Verification Pending**  
    > *Your account is awaiting KUET institutional email verification (pending external SMTP deployment). Restricted actions like applying for assistant jobs and submitting research proposals remain locked.*
- **Action Gating:**
  - Gated operations in `StudentAssistantJobsFragment`, `StudentReferenceOpportunitiesFragment`, and `StudentResearchPapersFragment` inspect `session.isVerified()`.
  - Unverified students attempting restricted actions receive an informative Alert Dialog explaining that institutional email verification is pending external SMTP deployment, and the action is blocked without submitting unauthorized network requests.

### 4.3 Form Validation & Error Handling
- Registration forms parse structured backend validation errors (`ApiError` with `List<String> details`).
- If input fails institutional email constraints or duplicate roll checks, the client displays specific field guidance (e.g., `• email: must be a valid institutional KUET student email ending in @stud.kuet.ac.bd`).

---

## 5. Room Database Synchronization

Upon successful authentication:
1. `AuthRepository.syncUserWithRoom` runs asynchronously.
2. An existing `UserEntity` in Room is updated with the remote user's name, email, department, roll/batch, and verified status, preserving existing wallet and escrow balances.
3. If no entity exists, a new `UserEntity` is inserted into the local Room database.
4. Corresponding role profiles (`StudentProfileEntity`, `AlumniProfileEntity`, or `TeacherProfileEntity`) are automatically created if absent.

---

## 6. Verification & Automated Test Evidence

### 6.1 Android Unit & MockWebServer Integration Suite
Location: `android/app/src/test/java/com/teligati/app/network/AuthIntegrationTest.java`

| Test Name | Verification Focus | Result |
|---|---|---|
| `testStudentRegisterSuccessAndSavesSession` | POST `/register/student`, receives 201 Created, verifies session tokens, role (`STUDENT`), and `isVerified=false`. | **PASS** |
| `testStudentLoginSuccessAndSavesSession` | POST `/login`, receives 200 OK, verifies session tokens, role, and `isVerified=true`. | **PASS** |
| `testAlumniRegisterWithInvitationToken` | POST `/register/alumni`, verifies `invitationToken` in request payload, saves `ALUMNI` session. | **PASS** |
| `testFacultyRegisterWithAuthorizationToken` | POST `/register/faculty`, verifies `authorizationToken` and `designation`, saves `TEACHER` session. | **PASS** |
| `testVerificationStatusUnverifiedResponse` | GET `/verification-status`, verifies unverified payload and `restrictedActions` list parsing. | **PASS** |
| `testAuthInterceptorAttachesBearerToken` | Intercepts request, asserts `Authorization: Bearer <token>` and `Accept: application/json` headers. | **PASS** |
| `testTokenAuthenticatorOn401RefreshesSuccessfully` | Initial 401 triggers `/refresh`, rotates tokens in `SessionManager`, and retries original request with new token. | **PASS** |
| `testTokenAuthenticatorSingleRetryOnRefreshFailure` | 401 followed by 400 refresh failure aborts without loop and clears `SessionManager`. | **PASS** |
| `testErrorParsingWithValidationDetails` | Parses 400 Bad Request with nested `ApiError.details` into human-readable bullet points. | **PASS** |
| `testLogoutClearsSessionAndRevokes` | POST `/logout`, revokes refresh token, and clears all session preferences. | **PASS** |

**Execution Result:**
```
BUILD SUCCESSFUL in 26s
24 actionable tasks: 5 executed, 19 up-to-date
All 20 tests completed, 0 failed.
```

### 6.2 Android APK Compilation
- Command: `gradlew.bat assembleDebug`
- Execution Result: `BUILD SUCCESSFUL in 46s` (36 actionable tasks).

### 6.3 Backend Regression Test Suite
- Command: `gradlew.bat test --rerun-tasks`
- Execution Result: `BUILD SUCCESSFUL in 58s` (all 51 backend tests executed and passed).

---

## 7. Deliverables & Next Steps

All requirements for **Phase 5B.3: Android Authentication Integration** are met:
- [x] Networking layer using Retrofit 2.11.0 + OkHttp 4.12.0 with configurable Base URL (`BuildConfig.BASE_URL`).
- [x] Secure session management using `EncryptedSharedPreferences` with Keystore backing.
- [x] AuthInterceptor and TokenAuthenticator handling Bearer tokens, single-retry 401 rotation, and synchronized concurrency.
- [x] All 8 authentication endpoints integrated.
- [x] Authoritative role routing (`STUDENT`, `ALUMNI`, `TEACHER`).
- [x] Explicit student verification-pending UX and action gating.
- [x] Local Room persistence synchronized with remote user identity.
- [x] Comprehensive unit and MockWebServer integration tests passing.
- [x] Zero regressions in Spring Boot backend test suite.

**Awaiting user review and approval before proceeding to any subsequent phase.**
