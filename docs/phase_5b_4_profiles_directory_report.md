# Wizards of Teligati — Phase 5B.4: Profiles & Directory APIs Report

**Date:** November 6, 2026  
**Status:** COMPLETE & FULLY VERIFIED  
**Component:** Spring Boot 3.x Backend (`backend/`) & Android Client Application (`android/`)  
**Database Schema Version:** Flyway Migration `V3__profile_directory_expansion.sql`

---

## 1. Executive Summary

Phase 5B.4 establishes authoritative server-side profile management and unified directory lookup capabilities for the **Wizards of Teligati** platform, connecting native Android UI screens to the authenticated Spring Boot backend.

### Key Milestones Achieved
1. **Database Schema & Entity Expansion:**
   - Authored Flyway migration `V3__profile_directory_expansion.sql` extending the `profiles` table with academic, professional, and institutional fields (bio, technical skills, projects, github/linkedin URLs, CGPA, specialization, clubs, faculty designation, research focus, lab name, publications count, actively hiring status, and institutional contact email).
   - Added high-performance indexes on `name`, `department`, `batch`, `current_company`, `country`, and `designation`.
   - Updated JPA entity `Profile.java` with corresponding mappings.

2. **Secure DTO Contracts & Privacy Enforcement:**
   - Implemented strict DTO-based API contracts: `ProfileResponse`, `ProfileUpdateRequest`, `StudentDirectoryResponse`, `AlumniDirectoryResponse`, `TeacherDirectoryResponse`, and generic `PagedResponse<T>`.
   - Sensitive academic information (specifically student `cgpa`) is protected by strict ownership privacy: visible **only** to the profile owner via `GET /api/v1/profiles/me` or `GET /api/v1/profiles/{userId}` when the authenticated caller is the account owner. Non-owners and directory consumers receive no CGPA data.
   - Server-side role immutability: Profile edit requests (`PUT /api/v1/profiles/me`) strictly ignore any client attempts to modify `role`, `isVerified`, `userId`, `email`, or password.
   - Password hashes and refresh tokens are excluded from all profile and directory responses.

3. **Backend Service & Controller Layer:**
   - Implemented `ProfileController` (`/api/v1/profiles`) and `ProfileService`:
     - `GET /api/v1/profiles/me`
     - `PUT /api/v1/profiles/me`
     - `GET /api/v1/profiles/{userId}`
   - Implemented `DirectoryController` (`/api/v1/directory`) and `DirectoryService`:
     - `GET /api/v1/directory/students` (paginated, filtered by name, department, batch)
     - `GET /api/v1/directory/alumni` (paginated, filtered by name, department, batch, company, country, position)
     - `GET /api/v1/directory/teachers` (paginated, filtered by name, department, designation, research focus)

4. **Android Client Integration:**
   - Added `ProfileApiService` with Retrofit declarations and exposed it via `ApiClient`.
   - Created Android `ProfileRepository` providing asynchronous execution, main-thread callback delivery, and seamless Room database synchronization.
   - Updated `ProfileFragment`: loads authoritative profile from backend via `GET /api/v1/profiles/me`, updates profile via `PUT /api/v1/profiles/me`, while preserving local Room wallet balances (`availableBalance` and `escrowBalance`).
   - Updated `StudentPortfolioFragment`: retrieves and persists portfolio details to backend and Room.
   - Updated `StudentAlumniDirectoryFragment`: loads alumni directory with real server data, pagination, and instant client-side filtering.
   - Room database acts as resilient offline-first cache; offline network errors gracefully degrade to local Room data.

5. **Automated Verification:**
   - Backend tests: **66 tests completed, 0 failed** (including 11 comprehensive profile & directory integration tests).
   - Android tests: **33 tests completed, 0 failed** (including MockWebServer end-to-end network tests).
   - Android APK build: **BUILD SUCCESSFUL** via `assembleDebug`.

---

## 2. API Contract Specification

All endpoints require JWT Bearer authentication in the `Authorization` header (`Authorization: Bearer <access_token>`).

### 2.1 Profile Management APIs

| Method | Endpoint | Description | Role Access |
|---|---|---|---|
| `GET` | `/api/v1/profiles/me` | Retrieves the authenticated user's own profile. Includes private fields (e.g. CGPA). | Any authenticated role |
| `PUT` | `/api/v1/profiles/me` | Updates authenticated user's profile details. Role and verification are immutable. | Any authenticated role |
| `GET` | `/api/v1/profiles/{userId}` | Retrieves profile of a specific user. Hides private fields (CGPA) for non-owners. | Any authenticated role |

#### Request Contract: `PUT /api/v1/profiles/me`
```json
{
  "name": "Wafi (Student)",
  "department": "Computer Science and Engineering",
  "batch": "2023",
  "graduationYear": "2027",
  "status": "Undergraduate Student",
  "bio": "Dedicated distributed systems researcher and open-source contributor",
  "skills": "Java 21, Spring Boot 3, Android, Postgres, Redis",
  "projects": "Wizards of Teligati v5.0, Autonomous Drone Pathfinder",
  "githubUrl": "https://github.com/wafi2307003",
  "linkedinUrl": "https://linkedin.com/in/wafi-kuet",
  "cgpa": 3.92,
  "specialization": "Distributed Systems & Mobile Architecture",
  "clubs": "KUET Computer Club, Teligati Open Source",
  "currentCompany": null,
  "currentPosition": null,
  "country": null,
  "city": null,
  "isAvailableForMentorship": null,
  "isActivelyHiring": null,
  "designation": null,
  "researchFocus": null,
  "labName": null,
  "publicationsCount": null,
  "contactEmail": null
}
```

#### Response Contract: `GET /api/v1/profiles/me`
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "userId": 1,
    "kuetId": "2307003",
    "email": "wafi2307003@stud.kuet.ac.bd",
    "name": "Wafi (Student)",
    "role": "ROLE_STUDENT",
    "isVerified": true,
    "department": "Computer Science and Engineering",
    "batch": "2023",
    "status": "Undergraduate Student",
    "bio": "Dedicated distributed systems researcher and open-source contributor",
    "skills": "Java 21, Spring Boot 3, Android, Postgres, Redis",
    "projects": "Wizards of Teligati v5.0, Autonomous Drone Pathfinder",
    "githubUrl": "https://github.com/wafi2307003",
    "linkedinUrl": "https://linkedin.com/in/wafi-kuet",
    "cgpa": 3.92,
    "specialization": "Distributed Systems & Mobile Architecture",
    "clubs": "KUET Computer Club, Teligati Open Source"
  }
}
```

---

### 2.2 Directory APIs

| Method | Endpoint | Query Parameters | Description |
|---|---|---|---|
| `GET` | `/api/v1/directory/students` | `name`, `department`, `batch`, `page`, `size` | Paginated student directory. Student CGPA is omitted. |
| `GET` | `/api/v1/directory/alumni` | `name`, `department`, `batch`, `company`, `country`, `position`, `page`, `size` | Paginated alumni directory with employment & mentorship status. |
| `GET` | `/api/v1/directory/teachers` | `name`, `department`, `designation`, `researchFocus`, `page`, `size` | Paginated teacher directory with research lab & publications. |

#### Response Contract: `GET /api/v1/directory/alumni`
```json
{
  "success": true,
  "message": "Alumni directory retrieved successfully",
  "data": {
    "content": [
      {
        "userId": 2,
        "name": "Tanvir (Alumni)",
        "department": "Computer Science and Engineering",
        "batch": "2016",
        "graduationYear": "2020",
        "currentCompany": "Tech Corp Munich",
        "currentPosition": "Senior Software Engineer",
        "country": "Germany",
        "city": "Munich",
        "skills": "System Architecture, Cloud Infrastructure, Go, Kubernetes, Spring",
        "linkedinUrl": "https://linkedin.com/in/tanvir-munich",
        "bio": "Senior Software Engineer in Munich, Germany. KUET '16 batch. Happy to provide mentorship and refer talented KUETians.",
        "isAvailableForMentorship": true,
        "isActivelyHiring": true,
        "isVerified": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

## 3. Security and Data Protection Controls

### 3.1 Role Immutability & Privilege Elevation Prevention
- User role (`ROLE_STUDENT`, `ROLE_ALUMNI`, `ROLE_TEACHER`) and verification status (`isVerified`) are strictly managed by administrative or authentication workflows.
- In `ProfileService.updateMyProfile`, only profile metadata fields are mapped. Even if a malicious request injects `role: "ROLE_TEACHER"` or `isVerified: true`, the service ignores them.
- Verified in backend automated test: `cannotAlterRoleViaProfileUpdate()`.

### 3.2 Academic Privacy
- A student's CGPA is considered confidential institutional information.
- The `cgpa` property is returned **only** when the caller is accessing their own profile (`/profiles/me` or `/profiles/{userId}` where caller ID matches target ID).
- When any other user views a student's profile via `/profiles/{userId}`, or browses the `/directory/students` endpoint, `cgpa` is scrubbed and never serialized.
- Verified in backend automated test: `getOtherUserProfileHidesCgpa()`.

### 3.3 Offline Resilience & Room Cache Preservation
- In Android `ProfileRepository`, remote data is synchronized into the local Room database (`users`, `student_profiles`, `alumni_profiles`, `teacher_profiles`).
- Critical local state—such as user escrow balances (`escrowBalance`) and available wallet balances (`availableBalance`)—is queried prior to updating the Room entity so that remote profile synchronizations do not overwrite financial balances.
- If the device is offline or the server is unreachable, `ProfileRepository` seamlessly falls back to cached Room records.

---

## 4. Verification Evidence

### 4.1 Backend Test Results
```
Task :test
BUILD SUCCESSFUL in 56s
5 actionable tasks: 5 executed
66 tests completed, 0 failed, 0 skipped
```

#### Profile and Directory Test Breakdown (`ProfileDirectoryTest.java`):
- `getMyProfileUnauthorized`: 401 Unauthorized for requests without Bearer token.
- `getMyProfileAsStudent`: Student profile retrieval with CGPA, roll, department, and skills.
- `getMyProfileAsAlumni`: Alumni profile retrieval with company, country, and hiring flags.
- `getMyProfileAsTeacher`: Teacher profile retrieval with designation and lab name.
- `updateStudentProfile`: Successful update of skills, bio, and githubUrl.
- `cannotAlterRoleViaProfileUpdate`: Malicious role elevation injection blocked.
- `invalidCgpaRejected`: CGPA values outside [0.00, 4.00] rejected with 400 Bad Request.
- `getOtherUserProfileHidesCgpa`: Public lookup hides private CGPA.
- `getNonexistentProfile`: 404 Not Found for non-existent user profile.
- `getStudentDirectory`: Paginated student directory omits CGPA.
- `getStudentDirectoryWithFilter`: Name and department filter evaluation.
- `getAlumniDirectory`: Paginated alumni directory returns mentorship flags.
- `getAlumniDirectoryWithFilters`: Filtering by country and company.
- `getTeacherDirectory`: Paginated faculty directory returns lab and research focus.
- `getTeacherDirectoryWithFocusFilter`: Research focus area filter evaluation.

### 4.2 Android Test Results
```
Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 16s
24 actionable tasks: 2 executed, 22 up-to-date
33 tests completed, 0 failed, 0 skipped
```

#### Profile Directory Integration Test Breakdown (`ProfileDirectoryIntegrationTest.java`):
- `fetchMyProfile_success_returnsProfileAndAttachesBearerToken`: Verifies Bearer header attachment and response deserialization.
- `fetchMyProfile_unauthorized_invokesErrorCallback`: Verifies 401 error propagation.
- `updateMyProfile_success_sendsUpdateRequest`: Verifies PUT method, `/api/v1/profiles/me` path, and request JSON payload.
- `getStudentDirectory_success_passesQueryParamsAndReceivesPage`: Verifies URL query parameter formatting and pagination parsing.
- `getAlumniDirectory_success_returnsAlumniWithMentorshipInfo`: Verifies alumni directory parsing and mentorship attributes.
- `getTeacherDirectory_success_returnsFacultyWithResearchFocus`: Verifies teacher directory parsing.
- `getProfileById_success_retrievesTargetProfile`: Verifies `/api/v1/profiles/{userId}` path lookup and CGPA omission.

### 4.3 Android APK Build
```
Task :app:assembleDebug
BUILD SUCCESSFUL in 16s
36 actionable tasks: 3 executed, 33 up-to-date
```

---

## 5. Scope Boundaries & Next Phase

In accordance with Phase 5B.4 instructions:
- **No Domain APIs:** Gigs, assistant jobs, applications, reference requests, research papers, proposals, messaging, and escrow contracts were not integrated into backend REST endpoints during this phase. They remain supported via Room local storage.
- **Next Phase:** Phase 5B.5 (Opportunities & Gigs Domain APIs).
