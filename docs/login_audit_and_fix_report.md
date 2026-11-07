# Wizards of Teligati — Login System Audit & Bug Fix Report

**Project:** Wizards of Teligati (KUET CSE Academic Demonstration Project)  
**Date:** November 6, 2026  
**Audited Subsystems:** Android Application Frontend, Spring Boot Backend, Database Seeding & Authentication API  
**Status:** **AUDITED, FIXED, TESTED & STABILIZED** (100% Verified)

---

## 1. Executive Summary

A comprehensive, end-to-end audit and remediation of the authentication and login subsystem was conducted across all three university roles:
1. **Student** (`ROLE_STUDENT`)
2. **Alumni** (`ROLE_ALUMNI`)
3. **Faculty / Teacher** (`ROLE_TEACHER` / `ROLE_FACULTY`)

Both login modalities—**standard credential submission** and **one-tap "⚡ Try Demo Account"**—were analyzed, verified against the live Spring Boot backend (`http://localhost:8080`), regression tested via automated Gradle test suites (backend and Android), and validated live on an Android emulator (`Pixel_8`, API 37).

All identified root causes have been resolved with clean, maintainable fixes adhering to the project's academic guidelines:
- **Zero fake/bypassed authentication:** All requests authenticate against the backend `/api/v1/auth/login` endpoint with BCrypt-hashed password verification and JWT issuance.
- **Zero payment gateways / simulated demo wallet only:** Financial ledger remains an in-memory / H2 educational escrow simulation.
- **Backend automated tests:** **147 / 147 passed** (0 failures, 0 errors, 0 skipped).
- **Android automated tests:** **67 / 67 passed** (0 failures, 0 errors, 0 skipped).
- **Live device tests:** All 6 login paths, session persistence, role mismatch alerts, and clean logouts were verified directly on the device.

---

## 2. Root Cause Analysis & Identified Defects

During the code inspection and live runtime reproduction, 5 specific architectural issues were identified:

### 1. Asynchronous Preferences Flushing in `SessionManager.java` (`apply()` vs `commit()`)
- **Problem:** `saveAuthSession()`, `updateTokens()`, `setSession()`, and `clearSession()` were invoking `SharedPreferences.Editor.apply()`.
- **Root Cause:** In Android, `apply()` writes modifications in-memory immediately while committing to persistent disk asynchronously in a worker thread. When activities immediately transitioned to `MainActivity` or switched between accounts, `MainActivity.onCreate()` executed before disk writes completed, reading stale preferences.
- **Fix:** Converted all mutations in `SessionManager` (`saveAuthSession`, `updateTokens`, `setSession`, `updateUserDetails`, `setAccessToken`, `setRefreshToken`, `setVerified`, and `clearSession`) to use synchronous `editor.commit()`.

### 2. `MainActivity.java` Intent Role Override Bug
- **Problem:** If a user logged out or switched accounts, `MainActivity` prioritized stale session tokens over explicit Intent extras passed from role-specific login activities.
- **Root Cause:** Lines 40–54 checked `if (session.isLoggedIn() && session.getAccessToken() != null)`, which used `session.getUserRole()` and completely bypassed `getIntent().getStringExtra("USER_ROLE")`.
- **Fix:** Prioritize explicit Intent extras (`USER_ROLE` and `USER_ID`) whenever passed from login activities. If the requested role diverges from the cached session, normalize the role and synchronize `session.setSession(userRole, userId)`.

### 3. Missing Screen-Level Role Validation (Cross-Role Leakage)
- **Problem:** Backend `/api/v1/auth/login` verifies credentials globally. If a Faculty member entered their credentials on `StudentLoginActivity`, or a Student entered credentials on `FacultyLoginActivity`, authentication succeeded and they were admitted without warning.
- **Root Cause:** Neither `StudentLoginActivity`, `AlumniLoginActivity`, nor `FacultyLoginActivity` checked whether `result.getUser().getRole()` corresponded to the intended screen role before launching `MainActivity`.
- **Fix:** Added role validation in all three login activities:
  - If a role mismatch occurs, `SessionManager.clearSession()` and `AuthRepository.logout(null)` are immediately triggered to prevent saving an unauthorized token, and an `AlertDialog` is displayed: `"This account is registered as [ROLE]. Please use the [ROLE] Login screen."`

### 4. Identifier Canonicalization for Student Email Logins
- **Problem:** If a student entered an institutional email (e.g. `wafi2307003@stud.kuet.ac.bd`) instead of a 7-digit roll number, `StudentLoginActivity` passed the email string directly as `USER_ID`.
- **Root Cause:** Room database queries and student UI dashboards expect the canonical 7-digit roll number (`2307003`).
- **Fix:** Extracted `result.getUser().getKuetId()` as `effectiveId` to ensure the dashboard and repositories always receive the canonical student roll number.

### 5. `AuthGatewayActivity` Window Stack Linger
- **Problem:** Starting login activities without finishing `AuthGatewayActivity` kept a heavy activity (holding `logo_banner.png`) alive in the background, causing Android WindowManager focus event delays during transitions.
- **Fix:** `AuthGatewayActivity` now finishes when delegating to `StudentLoginActivity`, `AlumniLoginActivity`, or `FacultyLoginActivity`. Each login activity implements `OnBackPressedCallback` to cleanly return to `AuthGatewayActivity` on back press.

---

## 3. Implementation Details & Modified Files

### A. `android/app/src/main/java/com/teligati/app/data/SessionManager.java`
- Switched all preference updates from `.apply()` to synchronous `.commit()`:
  - `saveAuthSession()`
  - `updateTokens()`
  - `setSession()`
  - `updateUserDetails()`
  - `setAccessToken()`
  - `setRefreshToken()`
  - `setVerified()`
  - `clearSession()`

### B. `android/app/src/main/java/com/teligati/app/MainActivity.java`
- Synchronized session initialization in `onCreate()`:
  ```java
  SessionManager session = SessionManager.getInstance(this);
  String intentRole = getIntent() != null ? getIntent().getStringExtra("USER_ROLE") : null;
  String intentId = getIntent() != null ? getIntent().getStringExtra("USER_ID") : null;

  if (intentRole != null && !intentRole.trim().isEmpty()) {
      userRole = SessionManager.normalizeRole(intentRole);
      if (intentId != null && !intentId.trim().isEmpty()) {
          userId = intentId.trim();
      }
      if (!userRole.equalsIgnoreCase(session.getUserRole()) || !userId.equalsIgnoreCase(session.getUserId())) {
          session.setSession(userRole, userId);
      }
  } else if (session.isLoggedIn() && session.getAccessToken() != null) {
      userRole = session.getUserRole();
      userId = session.getUserId();
  } else {
      session.setSession(userRole, userId);
  }
  ```

### C. `android/app/src/main/java/com/teligati/app/auth/AuthGatewayActivity.java`
- Refactored role selection navigation to finish gateway when launching login or registration activities, preventing background window linger.
- Added `navigateTo(Class<?> targetActivity)` helper.

### D. `android/app/src/main/java/com/teligati/app/auth/StudentLoginActivity.java`
- Added canonical roll extraction: `effectiveId = result.getUser().getKuetId()`.
- Added role mismatch enforcement:
  ```java
  if (!"STUDENT".equalsIgnoreCase(role)) {
      SessionManager.getInstance(StudentLoginActivity.this).clearSession();
      authRepository.logout(null);
      new AlertDialog.Builder(StudentLoginActivity.this)
              .setTitle("Role Mismatch")
              .setMessage("This account is registered as " + role + ". Please use the " + role + " Login screen.")
              .setPositiveButton("OK", null)
              .show();
      return;
  }
  ```
- Added `OnBackPressedCallback` to cleanly navigate back to `AuthGatewayActivity`.

### E. `android/app/src/main/java/com/teligati/app/auth/AlumniLoginActivity.java`
- Added canonical email extraction: `effectiveId = result.getUser().getEmail()`.
- Added role mismatch enforcement against `"ALUMNI"`.
- Added `OnBackPressedCallback` to return to `AuthGatewayActivity`.

### F. `android/app/src/main/java/com/teligati/app/auth/FacultyLoginActivity.java`
- Added canonical email extraction: `effectiveId = result.getUser().getEmail()`.
- Added role mismatch enforcement against `"TEACHER"` / `"FACULTY"`.
- Added `OnBackPressedCallback` to return to `AuthGatewayActivity`.

---

## 4. Test Verification Results

### 1. Spring Boot Backend Tests
- **Command:** `gradlew test` (backend)
- **Results:**
  - **Total Tests:** 147
  - **Passed:** 147
  - **Failures:** 0
  - **Errors:** 0
  - **Skipped:** 0
  - **Build Status:** `BUILD SUCCESSFUL`

### 2. Android Unit Tests
- **Command:** `gradlew testDebugUnitTest` (android)
- **Results:**
  - **Total Tests:** 67
  - **Passed:** 67
  - **Failures:** 0
  - **Errors:** 0
  - **Skipped:** 0
  - **Build Status:** `BUILD SUCCESSFUL`

### 3. Debug APK Assembly
- **Command:** `gradlew assembleDebug` (android)
- **Artifact:** `android/app/build/outputs/apk/debug/app-debug.apk`
- **Build Status:** `BUILD SUCCESSFUL`
- **Installation:** Streamed install to `emulator-5554` confirmed `Success`.

---

## 5. Live Demonstration & Verification Across All 6 Paths

| Path # | Role | Credentials | Action | Observed Result | Status |
|---|---|---|---|---|---|
| **Path 1** | Student | `2307003` / `demo` | "⚡ Try Demo Account" | Loaded `Student Portal`. Name: `Wafi (Student) ✓ Verified Student`, Roll: `2307003`, CGPA: `3.84`, Wallet Balance: `৳10000`. | **PASS** |
| **Path 2** | Student | `2307003` / `demo` | Manual Entry + "Log In" | Form validated, authenticated via `/api/v1/auth/login`, loaded `Student Portal` with full assistant opportunities. | **PASS** |
| **Path 3** | Alumni | `tanvir.munich@kuet.ac.bd` / `demo` | "⚡ Try Demo Account" | Loaded `Alumni Portal`. Name: `Tanvir (Alumni) ✓ Verified Alumni`, Location: `Munich, Germany`, reference requests loaded. | **PASS** |
| **Path 4** | Alumni | `tanvir.munich@kuet.ac.bd` / `demo` | Manual Entry + "Log In" | Form validated, authenticated via backend, loaded `Alumni Executive Portal` with job post options. | **PASS** |
| **Path 5** | Faculty | `azharul@cse.kuet.ac.bd` / `demo` | "⚡ Try Demo Account" | Loaded `Faculty Portal`. Name: `Dr. Azharul (Faculty) ✓ Faculty`, Research: `Deep Learning, Cloud Computing`, PhD proposals loaded. | **PASS** |
| **Path 6** | Faculty | `azharul@cse.kuet.ac.bd` / `demo` | Manual Entry + "Log In" | Form validated, authenticated via backend, loaded `Faculty and Research Lab Portal` with proposal reviews. | **PASS** |

### Additional Security Verifications
1. **Cross-Role Rejection Test (Student Screen):**
   - Entered Alumni credentials (`tanvir.munich@kuet.ac.bd` / `demo`) on `StudentLoginActivity`.
   - **Observed:** Dialog popped up: `"This account is registered as ALUMNI. Please use the ALUMNI Login screen."` Session cleared; unauthenticated.
2. **Cross-Role Rejection Test (Alumni Screen):**
   - Entered Student credentials (`2307003` / `demo`) on `AlumniLoginActivity`.
   - **Observed:** Dialog popped up: `"This account is registered as STUDENT. Please use the STUDENT Login screen."` Session cleared; unauthenticated.
3. **Cross-Role Rejection Test (Faculty Screen):**
   - Entered Student credentials (`2307003` / `demo`) on `FacultyLoginActivity`.
   - **Observed:** Dialog popped up: `"This account is registered as STUDENT. Please use the STUDENT Login screen."` Session cleared; unauthenticated.
4. **Clean Logout & Session Invalidation:**
   - On each role dashboard, opened Navigation Drawer $\to$ Settings $\to$ "🚪 Sign Out / Logout".
   - Backend revoked refresh token, `SessionManager.clearSession()` cleared all local preferences, and app returned cleanly to `AuthGatewayActivity`.

---

## 6. Demo Account Credentials Summary

For university project demonstration:

| Role | Primary Identifier | Password | Name | Department | Key Dashboard Features |
|---|---|---|---|---|---|
| **Student** | `2307003` | `demo` | Samun Sadab Wafi | CSE (2nd Year) | Assistant jobs, research papers, reference requests, demo wallet balance (`৳10,000`), portfolio |
| **Alumni** | `tanvir.munich@kuet.ac.bd` | `demo` | Engr. Tanvir Ahmed | CSE '15 | Post full-time jobs, post assistant gigs, review applicants, evaluate reference letters |
| **Faculty** | `azharul@cse.kuet.ac.bd` | `demo` | Dr. K. M. Azharul Hasan | CSE Faculty | Post research papers, publish opportunities, evaluate student proposals, review references |

---

## 7. Conclusion

The login system for Wizards of Teligati is now fully audited, corrected, and verified. All 6 authentication paths operate reliably on Android with real-time Spring Boot token validation and zero cross-role leakage. The project is completely stable and ready for academic demonstration.
