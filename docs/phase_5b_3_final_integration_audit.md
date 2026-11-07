# Wizards of Teligati — Phase 5B.3 Final Integration Audit Report

**Date:** November 6, 2026  
**Auditor:** Antigravity (Advanced Agentic Coding)  
**Audit Scope:** Android Authentication Integration & Security Layer  
**Verdict:** **PASS WITH LIMITATIONS**  

---

## 1. Executive Summary & Verdict

This audit performs an evidence-based security and integration verification of the Phase 5B.3 Android authentication integration with the Spring Boot backend.

### Verdict: PASS WITH LIMITATIONS
- **Verdict Explanation:**
  - **PASS:** The native Android client strictly enforces Android Keystore encryption for JWT tokens, binds Authorization headers exclusively to the backend origin, prevents token leakage to external hosts, handles concurrent 401s with exactly one refresh call, avoids race conditions between logout and refresh, and authoritatively routes users by backend role (`ROLE_STUDENT`, `ROLE_ALUMNI`, `ROLE_TEACHER`). Unverified students are explicitly presented with pending status and gated at the UI layer without claiming email verification has occurred. All 26 Android tests pass, the debug APK builds successfully, and all 51 backend regression tests pass.
  - **LIMITATIONS:**
    1. Wallet and escrow balances remain client-side Room records because the backend financial ledger domain APIs are deferred to Phase 5B.4. They are preserved locally across user profile updates, but are not authoritative financial guarantees.
    2. Real institutional email verification remains pending external SMTP relay activation; the client explicitly reports `isVerified = false` and blocks restricted actions.

---

## 2. Token Storage Audit

### 2.1 Storage Mechanism & Encryption
- **Class:** `com.teligati.app.data.SessionManager`
- **Underlying Engine:** `androidx.security.crypto.EncryptedSharedPreferences` backed by Android Keystore (`MasterKey.KeyScheme.AES256_GCM`).
- **Cryptographic Scheme:**
  - Preference keys: `PrefKeyEncryptionScheme.AES256_SIV`
  - Preference values: `PrefValueEncryptionScheme.AES256_GCM`
- **Plaintext Fallback Protection:**
  `initPreferences` inspects `isTestEnvironment()`. On any real Android runtime (device or ART emulator), any failure to initialize `EncryptedSharedPreferences` throws `SecurityException`. It never silently degrades to unencrypted `SharedPreferences` on Android.
- **Passwords:** Neither plain text nor hashed passwords are ever stored in `SessionManager` or Room.
- **Room Database Separation:** Tokens (`access_token`, `refresh_token`) are stored exclusively in `EncryptedSharedPreferences` and are never written to Room entity tables (`users`, `student_profiles`, etc.).
- **Log Sanitation:** HTTP headers and sensitive tokens are filtered (`HttpLoggingInterceptor.Level.HEADERS` in debug, `Level.NONE` in release; tokens never logged).
- **Session Bleed Prevention:** `saveAuthSession()` explicitly executes `editor.clear()` before storing newly authenticated user credentials, ensuring User A's cached name, CGPA, department, or tokens cannot bleed into User B's session upon subsequent login on the same device.

---

## 3. AuthInterceptor and TokenAuthenticator Audit

### 3.1 Backend Origin Isolation (`AuthInterceptor.java`)
- **Origin Check:** `AuthInterceptor` parses `baseUrl` and compares `url.host()` and `url.port()`.
- **Token Leakage Prevention:** The `Authorization: Bearer <accessToken>` header is attached **only** if `isBackendOrigin(request.url())` evaluates to `true`.
- **Evidence:** Verified by automated test `testAuthInterceptorDoesNotSendTokenToExternalOrigin`, where requests to an external origin do not receive the Bearer token.

### 3.2 Token Refresh & Concurrency Handling (`TokenAuthenticator.java`)
- **Origin Guard:** `TokenAuthenticator` checks that the 401 response originated from the configured backend origin before attempting refresh. Unrelated 401s are ignored.
- **Auth Endpoint Exclusion:** If the failed request was `/auth/refresh`, `/auth/login`, `/auth/logout`, or `/auth/register`, refresh is aborted to prevent recursion.
- **Bounded Retry:** `responseCount(response) >= 2` terminates retries immediately, preventing infinite 401 loops.
- **Concurrency Prevention:** Refresh is guarded by a synchronized lock. If multiple concurrent requests receive 401 simultaneously:
  1. The first thread acquires the lock, observes that the access token has not changed, and invokes `api/v1/auth/refresh`.
  2. The remaining threads wait. Once the first thread updates `SessionManager`, the waiting threads wake up, detect `currentBearer != failedAuthHeader`, and immediately retry with the refreshed token without making duplicate refresh calls.
  3. Verified by automated test `testConcurrent401ResponsesExecuteSingleRefresh` (4 threads, exactly 1 refresh call executed).
- **Atomic Persistence:** `sessionManager.updateTokens(newAccessToken, newRefreshToken)` writes both tokens in a single `editor.apply()`.
- **Logout Race Condition Prevention:** If `clearSession()` is called (e.g. user taps logout) while a refresh request is in flight, `updateTokens()` detects that `KEY_REFRESH_TOKEN` was removed, returns `false`, and refuses to revive the old session.
  - Verified by automated test `testLogoutRaceConditionPreventsSessionRevival`.

---

## 4. Room Database Synchronization Audit

### 4.1 Implementation: `AuthRepository.syncUserWithRoom()`
- **Authoritative Identity:** `userId` and `normRole` are derived directly from the trusted server response (`summary.getKuetId()`, `summary.getEmail()`, `summary.getRole()`).
- **Profile Integrity:** Synchronization updates only `name`, `email`, `department`, `rollOrBatch`, and `isVerified`. It cannot overwrite the authenticated user's ID with client-supplied values.
- **Financial Balance Ownership:**
  ```java
  double available = existingUser != null ? existingUser.getAvailableBalance() : 1000.0;
  double escrow = existingUser != null ? existingUser.getEscrowBalance() : 0.0;
  ```
  Wallet and escrow balances are maintained locally in Room because financial endpoints belong to future phases. They are not treated as backend-authoritative data.
- **Account Isolation:** Queries and updates are keyed strictly by `userId`. User A's data is never merged or transferred to User B.
- **Local Functionality Intact:** All local Room entities (`job_postings`, `job_applications`, `research_papers`, `research_proposals`, `reference_requests`, `messages`, `notifications`, `transactions`) remain intact.

---

## 5. Role Compatibility & Routing Audit

| Category | Backend Role (`User.role`) | Normalized Client Role | Destination Activity / Dashboard | Status |
|---|---|---|---|---|
| Student Registration / Login | `ROLE_STUDENT` | `STUDENT` | `MainActivity` $\rightarrow$ `StudentDashboardFragment` | **VERIFIED** |
| Alumni Registration / Login | `ROLE_ALUMNI` | `ALUMNI` | `MainActivity` $\rightarrow$ `AlumniDashboardFragment` | **VERIFIED** |
| Faculty Registration / Login | `ROLE_TEACHER` | `TEACHER` | `MainActivity` $\rightarrow$ `TeacherDashboardFragment` | **VERIFIED** |
| Faculty Alternate Alias | `ROLE_FACULTY` / `FACULTY` | `TEACHER` | `MainActivity` $\rightarrow$ `TeacherDashboardFragment` | **VERIFIED** |

- Tested independently in `testRoleNormalizationMappingAllRoles`, `testStudentRegisterSuccessAndSavesSession`, `testAlumniRegisterWithInvitationToken`, and `testFacultyRegisterWithAuthorizationToken`.

---

## 6. Verification Restrictions & Gating Audit

### 6.1 Server Authority vs. UI Gating
- Verification status is populated from backend `TokenResponse.getUser().isVerified()` or `GET /api/v1/auth/verification-status`.
- **Dynamic Header & UI State:**
  - Removed all hardcoded names ("Samun Sadab Wafi", "Engr. Tanvir Ahmed", "Prof. Dr. K. M. Azharul Hasan").
  - `MainActivity` sidebar drawer header displays dynamic `session.getUserName()`, `session.getUserDept()`, and status.
  - `StudentDashboardFragment`, `AlumniDashboardFragment`, `TeacherDashboardFragment`, and `ProfileFragment` render dynamic user details and verification badges.
- **Student Verification Pending Notice:**
  - If `isVerified == false`, `cardVerificationNotice` is displayed:
    > **⏳ Institutional Verification Pending**  
    > *Your account is awaiting KUET institutional email verification (pending external SMTP deployment). Restricted actions like applying for assistant jobs and submitting research proposals remain locked.*
- **Action Gating:**
  - Assistant job application (`StudentAssistantJobsFragment.showApplyDialog`)
  - Reference request submission (`StudentReferenceOpportunitiesFragment.showReferenceRequestDialog`)
  - Research collaboration express interest (`StudentResearchPapersFragment.showExpressInterestDialog`)
  All inspect `session.isVerified()`. If `false`, an Alert Dialog is shown and the operation aborts.
- **No False Claims:** The app explicitly informs the user that institutional verification is pending external SMTP deployment; it does not claim email verification occurred.
- **Backend Authority Note:** UI checks are UX guardrails. Actual security is guaranteed on the server by `@userSecurity.isVerified(authentication)` on protected endpoints.

---

## 7. Logout and Account Switching Audit

1. **Successful Logout:** Calls `/api/v1/auth/logout`, revokes refresh token on backend, executes `sessionManager.clearSession()`, clears back-stack via `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, and navigates to `AuthGatewayActivity`.
2. **Offline Logout:** If device is offline or backend returns an error, `AuthRepository.logout()` safely falls back to clearing `sessionManager.clearSession()` so the user is never trapped in a broken authenticated state. Tested in `testOfflineLogoutClearsSessionGracefully`.
3. **Account Switching:** When logging out as Student and logging in as Alumni, `saveAuthSession` clears prior preferences, updates `SessionManager`, and loads `AlumniDashboardFragment`. No student data or cached identity remains in the session.
4. **Back Navigation After Logout:** Activities are launched with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, ensuring pressing Android Back button closes the app rather than reopening a logged-out dashboard.

---

## 8. Test Evidence & Results

### 8.1 Android Network & Integration Tests
File: `android/app/src/test/java/com/teligati/app/network/AuthIntegrationTest.java`  
Total Tests: **16** | Passed: **16** | Failed: **0**

| Test Case Name | Boundary / Functionality Tested | Result |
|---|---|---|
| `testStudentRegisterSuccessAndSavesSession` | Real Retrofit POST `/register/student` $\rightarrow$ 201 Created $\rightarrow$ Session populated with `isVerified=false` | **PASS** |
| `testStudentLoginSuccessAndSavesSession` | Real Retrofit POST `/login` $\rightarrow$ 200 OK $\rightarrow$ Session populated with tokens and `isVerified=true` | **PASS** |
| `testAlumniRegisterWithInvitationToken` | Real Retrofit POST `/register/alumni` $\rightarrow$ verifies `invitationToken` in request body $\rightarrow$ 201 Created | **PASS** |
| `testFacultyRegisterWithAuthorizationToken` | Real Retrofit POST `/register/faculty` $\rightarrow$ verifies `authorizationToken` in request body $\rightarrow$ 201 Created | **PASS** |
| `testVerificationStatusUnverifiedResponse` | Real Retrofit GET `/verification-status` $\rightarrow$ parses `isVerified=false` and `restrictedActions` list | **PASS** |
| `testAuthInterceptorAttachesBearerToken` | Real OkHttp call $\rightarrow$ asserts `Authorization: Bearer <token>` and `Accept: application/json` | **PASS** |
| `testAuthInterceptorDoesNotSendTokenToExternalOrigin` | Real OkHttp call to external host $\rightarrow$ verifies Bearer token is NOT sent to unrelated hosts | **PASS** |
| `testTokenAuthenticatorOn401RefreshesSuccessfully` | Real 401 response $\rightarrow$ triggers `/refresh` $\rightarrow$ rotates tokens $\rightarrow$ retries original request | **PASS** |
| `testTokenAuthenticatorSingleRetryOnRefreshFailure` | Real 401 response $\rightarrow$ refresh returns 400 $\rightarrow$ terminates retry and clears session | **PASS** |
| `testConcurrent401ResponsesExecuteSingleRefresh` | 4 concurrent threads hit 401 simultaneously $\rightarrow$ exactly 1 `/refresh` call executed $\rightarrow$ all 4 succeed | **PASS** |
| `testLogoutRaceConditionPreventsSessionRevival` | Simulates logout during active refresh $\rightarrow$ `updateTokens` returns `false` $\rightarrow$ session stays cleared | **PASS** |
| `testSessionBleedPreventionBetweenDifferentUsers` | Logs in User A then User B $\rightarrow$ verifies all User A keys are purged without bleeding into User B | **PASS** |
| `testErrorParsingWithValidationDetails` | Real 400 Bad Request with field errors $\rightarrow$ parsed into user-friendly bullet points | **PASS** |
| `testLogoutClearsSessionAndRevokes` | Real POST `/logout` with refresh token $\rightarrow$ clears tokens and logged-in state | **PASS** |
| `testOfflineLogoutClearsSessionGracefully` | Simulates offline / 500 error during logout $\rightarrow$ session is still cleared cleanly | **PASS** |
| `testRoleNormalizationMappingAllRoles` | Tests normalization of `ROLE_STUDENT`, `ROLE_ALUMNI`, `ROLE_TEACHER`, `ROLE_FACULTY`, `FACULTY` | **PASS** |

### 8.2 Additional Android Unit Tests
File: `android/app/src/test/java/com/teligati/app/WorkflowUnitTest.java`  
Total Tests: **10** | Passed: **10** | Failed: **0**

### 8.3 Compilation Verification
- **Command:** `gradlew.bat assembleDebug`  
- **Result:** `BUILD SUCCESSFUL in 20s` (36 actionable tasks).

### 8.4 Spring Boot Backend Regression Suite
- **Command:** `gradlew.bat test --rerun-tasks`  
- **Result:** `BUILD SUCCESSFUL in 57s` (5 actionable tasks, all 51 backend tests executed and passed).

---

## 9. Modified Files During Phase 5B.3 Audit

1. `android/app/build.gradle.kts` — added `BASE_URL` BuildConfig field, `security-crypto`, `mockwebserver`, and unit test configuration.
2. `android/app/src/main/java/com/teligati/app/data/SessionManager.java` — implemented Keystore encryption, atomic `updateTokens`, test-only JVM fallback, and session bleed protection.
3. `android/app/src/main/java/com/teligati/app/network/AuthInterceptor.java` — added backend origin validation to prevent leaking tokens to external hosts.
4. `android/app/src/main/java/com/teligati/app/network/TokenAuthenticator.java` — origin check, atomic token persistence, race-free logout protection.
5. `android/app/src/main/java/com/teligati/app/network/ApiClient.java` — configured Retrofit, OkHttp timeouts, interceptor, and authenticator.
6. `android/app/src/main/java/com/teligati/app/data/repository/AuthRepository.java` — all 8 authentication operations, Room user/profile sync, error parsing, and testable postExecutor.
7. `android/app/src/main/java/com/teligati/app/MainActivity.java` — dynamic sidebar header, backend-authoritative role routing, startup verification status refresh.
8. `android/app/src/main/java/com/teligati/app/auth/StudentLoginActivity.java` — connected to `AuthRepository.login`.
9. `android/app/src/main/java/com/teligati/app/auth/StudentRegisterActivity.java` — connected to `AuthRepository.registerStudent`.
10. `android/app/src/main/java/com/teligati/app/auth/AlumniLoginActivity.java` — connected to `AuthRepository.login`.
11. `android/app/src/main/java/com/teligati/app/auth/AlumniRegisterActivity.java` & layout — added password & invitation token fields, connected to `AuthRepository.registerAlumni`.
12. `android/app/src/main/java/com/teligati/app/auth/FacultyLoginActivity.java` — connected to `AuthRepository.login`.
13. `android/app/src/main/java/com/teligati/app/auth/FacultyRegisterActivity.java` & layout — added password & authorization token fields, connected to `AuthRepository.registerFaculty`.
14. `android/app/src/main/java/com/teligati/app/LoginActivity.java` — connected tabbed login to `AuthRepository.login`.
15. `android/app/src/main/java/com/teligati/app/fragment/StudentDashboardFragment.java` & layout — dynamic name, verification pending banner, logout integration.
16. `android/app/src/main/java/com/teligati/app/fragment/AlumniDashboardFragment.java` — dynamic name, logout integration.
17. `android/app/src/main/java/com/teligati/app/fragment/TeacherDashboardFragment.java` — dynamic name, logout integration.
18. `android/app/src/main/java/com/teligati/app/fragment/ProfileFragment.java` — dynamic name & verification status fallback.
19. `android/app/src/main/java/com/teligati/app/fragment/StudentAssistantJobsFragment.java` — verification status action gating.
20. `android/app/src/main/java/com/teligati/app/fragment/StudentReferenceOpportunitiesFragment.java` — verification status action gating.
21. `android/app/src/main/java/com/teligati/app/fragment/StudentResearchPapersFragment.java` — verification status action gating.
22. `android/app/src/test/java/com/teligati/app/network/AuthIntegrationTest.java` — 16 integration tests.
23. `android/app/src/test/java/com/teligati/app/testutil/InMemorySharedPreferences.java` — test utility for JVM test runs.

---

## 10. Remaining Limitations

1. **Local Domain Persistence vs. Remote Backend:**  
   Domain APIs (jobs, research papers, applications, proposals, messages) remain managed locally in Room. Their remote integration with Spring Boot is scheduled for subsequent phases.
2. **Wallet & Escrow Balances:**  
   Balances in Room are client-side test ledger figures. True financial transactions will require backend ledger endpoints in future phases.
3. **Institutional SMTP Service:**  
   Student accounts remain unverified (`isVerified = false`) pending deployment of the production SMTP service.

---

## 11. Conclusion

The Phase 5B.3 integration audit is complete with verdict **PASS WITH LIMITATIONS**.
No unrelated code was touched. Work has stopped per instructions, awaiting review.
