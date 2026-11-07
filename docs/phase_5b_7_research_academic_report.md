# Phase 5B.7 — Research & Academic Opportunities: Final Implementation Report

**Project:** Wizards of Teligati (KUET Collaboration Platform)  
**Phase:** 5B.7 Research & Academic Opportunities  
**Date:** November 6, 2026  
**Status:** COMPLETE — FULLY VERIFIED  

---

## 1. Executive Summary

Phase 5B.7 implements the end-to-end **Research & Academic Opportunities** domain connecting KUET faculty (teachers) and verified students. It integrates Spring Boot backend REST endpoints, Flyway database migrations, server-side authorization and state machine validations, academic notification triggers, Android network services, Room database repositories, and reactive UI fragments.

All verification gates have succeeded:
- **Backend Test Suite:** 112 / 112 tests passed (17 test suites, 0 failures, 0 errors, 0 skipped).
- **Android Unit Tests:** 55 / 55 tests passed (6 test suites, 0 failures, 0 errors, 0 skipped).
- **Android APK Build:** `assembleDebug` completed with 0 errors.

---

## 2. Database Architecture & Flyway V6 Migration

File: `backend/src/main/resources/db/migration/V6__research_academic_opportunities.sql`

### 2.1 Extended `projects` Table
- Added `positions_count INT DEFAULT 1`
- Added `deadline TIMESTAMP`
- Added `preferred_background VARCHAR(255)`
- Added `eligibility_criteria TEXT`
- Added `duration VARCHAR(100)`
- Added `skills_required VARCHAR(255)`
- Added `category VARCHAR(100)`
- Added `min_cgpa DOUBLE PRECISION`
- Performance indexes on `(status, category)` and `deadline`.

### 2.2 Created `research_papers` Table
```sql
CREATE TABLE research_papers (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    abstract_text TEXT NOT NULL,
    research_field VARCHAR(100) NOT NULL,
    authors VARCHAR(500) NOT NULL,
    publication_date DATE,
    doi VARCHAR(255),
    reference_url VARCHAR(500),
    attachment_url VARCHAR(500),
    teacher_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_paper_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 2.3 Extended `applications` Table
- Added `feedback TEXT` (faculty review notes / instructions for revisions).
- Added `review_notes TEXT` (internal faculty notes).
- Performance indexes on `(applicant_id, status)` and `(project_id, status)`.

### 2.4 Created `notifications` Table
```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## 3. Backend Implementation & Authorization Security

### 3.1 Entities & Repositories
- `ResearchPaper`: JPA entity mapped to `research_papers`, with `ResearchPaperRepository` supporting field filtering and title/author searches.
- `Project`: JPA entity extended with academic opening fields, with `ProjectRepository.searchResearchOpportunities(...)`.
- `Application`: JPA entity extended with `feedback` and `reviewNotes`.
- `Notification`: JPA entity mapped to `notifications`, with `NotificationRepository` supporting unread counts and user queries.

### 3.2 Services & Security Controls
- **`ResearchPaperService`**:
  - `publishPaper`: Authenticated user must have `ROLE_TEACHER` or `ROLE_ADMIN`.
  - `updatePaper` & `deletePaper`: Strictly enforces teacher ownership (caller must own the paper or be admin).
  - `getPapers`: Publicly readable by all authenticated KUET members.
- **`ResearchAcademicService`**:
  - `createOpportunity`: Caller must have `ROLE_TEACHER` or `ROLE_ADMIN`.
  - `closeOpportunity`: Validates caller ownership; sets status to `CLOSED` and `isOpenForContribution = false`.
  - `submitApplication`:
    - Strict role enforcement: Only `ROLE_STUDENT` can apply.
    - Institutional verification check: Verified students only (`isVerified = true`).
    - Opportunity deadline check: Rejects submissions if `deadline` has passed.
    - Opportunity status check: Rejects submissions if opportunity is `CLOSED` or not open for contribution.
    - Duplicate check: Prevents active duplicate applications (`PENDING`, `UNDER_REVIEW`, `REVISION_REQUESTED`, `ACCEPTED`).
    - CGPA eligibility check: Rejects applicants below minimum CGPA.
  - `reviewApplication`:
    - Teacher ownership: Caller must be the faculty owner of the opportunity.
    - Validates target status: `ACCEPTED`, `REJECTED`, `REVISION_REQUESTED`.
    - Mandatory feedback: Feedback instructions required when requesting revisions.
  - `resubmitApplication`:
    - Student applicant ownership check.
    - Status check: Only applications in `REVISION_REQUESTED` state can be resubmitted.
    - Updates statement/skills and resets state to `PENDING`.
  - `withdrawApplication`:
    - Student applicant ownership check.
    - Status check: Only pending/active applications can be withdrawn (`WITHDRAWN`).
- **`NotificationService`**:
  - Automatically sends contextual notifications on academic events:
    - Application submitted -> Notifies faculty lead.
    - Review decision -> Notifies student (`ACCEPTED`, `REJECTED`, `REVISION_REQUESTED`).
    - Resubmission -> Notifies faculty lead.

### 3.3 REST API Endpoints
- `GET /api/v1/research/opportunities` — Query & filter research calls.
- `GET /api/v1/research/opportunities/{id}` — Get single opportunity.
- `POST /api/v1/research/opportunities` — Publish research opening (Teacher).
- `PUT /api/v1/research/opportunities/{id}` — Edit research opening (Teacher).
- `PATCH /api/v1/research/opportunities/{id}/close` — Close opening (Teacher).
- `DELETE /api/v1/research/opportunities/{id}` — Delete opening (Teacher).
- `POST /api/v1/research/opportunities/{id}/applications` — Submit application (Student).
- `GET /api/v1/research/opportunities/{id}/applications` — View applicants (Teacher).
- `GET /api/v1/research/applications/me` — View my applications (Student).
- `GET /api/v1/research/applications/{id}` — View single application.
- `PATCH /api/v1/research/applications/{id}/review` — Review application (Teacher: ACCEPT, REJECT, REVISION_REQUESTED).
- `PATCH /api/v1/research/applications/{id}/resubmit` — Resubmit revised application (Student).
- `PATCH /api/v1/research/applications/{id}/withdraw` — Withdraw pending application (Student).
- `GET /api/v1/research/papers` — Browse & search research papers.
- `POST /api/v1/research/papers` — Publish paper (Teacher).
- `PUT /api/v1/research/papers/{id}` — Update paper (Teacher).
- `DELETE /api/v1/research/papers/{id}` — Delete paper (Teacher).
- `GET /api/v1/notifications` — Fetch user notification feed.
- `PATCH /api/v1/notifications/{id}/read` — Mark notification read.

---

## 4. Android Integration & UI Workflow

### 4.1 Network Architecture
- `ResearchApiService`: Full Retrofit contract covering all research opportunities, applications, and papers.
- `ApiClient`: Exposes `getResearchApiService()` with bearer authentication and automatic token refresh interceptors.
- `OpportunityRepository`: Integrates `ResearchApiService`, coordinating remote calls with Room local database cache.
- `AppRepository`: Exposes clean asynchronous methods:
  - `reviewResearchApplication(id, status, feedback, callback)`
  - `resubmitResearchApplication(id, statement, skills, callback)`
  - `withdrawResearchApplication(id, callback)`
  - `closeResearchOpportunity(id, callback)`
  - `publishResearchPaper(paper, callback)`
  - `getResearchPapers(callback)`

### 4.2 Fragment Behaviors
1. **`TeacherResearchOpportunitiesFragment`**:
   - Posts new research calls with duration, funding, eligibility, and scope.
   - Allows closing/deleting openings via `closeResearchOpportunity`.
   - Inspects applications received for each opening.
2. **`TeacherStudentResearchersFragment`**:
   - Lists applicant students with Roll, Department, CGPA, and research statements.
   - Allows faculty to **Accept into Lab** (`ACCEPTED`).
   - Allows faculty to **Decline** (`REJECTED`) or **Request Revisions** with custom feedback instructions (`REVISION_REQUESTED`).
   - Supports viewing full student profiles and initiating direct messages.
3. **`TeacherResearchPapersFragment`**:
   - Lists published papers; allows publishing calls for co-authors with DOI and abstract.
   - Enforces ownership when deleting or updating papers.
4. **`StudentResearchPapersFragment`**:
   - Displays published papers with category filtering and keyword search.
   - Enforces institutional email verification gate before allowing students to express interest or submit statements.
   - Prevents duplicate applications.
5. **`StudentHistoryFragment`**:
   - Displays student's research applications alongside job applications and referrals.
   - Tapping an application reveals detailed review status.
   - When status is `REVISION_REQUESTED`, provides a **Revise & Resubmit** action dialog to address faculty notes.
   - When status is `PENDING`, provides an explicit **Withdraw Application** action.

---

## 5. Verification Results

### 5.1 Backend Verification
Command: `gradlew.bat test --rerun-tasks`
- **Total Tests:** 112
- **Pass Rate:** 100% (112 / 112)
- **Failures / Errors:** 0
- **Suites:** 17 suites including `MigrationV6Test` (4 tests) and `ResearchAcademicDomainTest` (6 tests).

### 5.2 Android Unit Test Verification
Command: `gradlew.bat testDebugUnitTest`
- **Total Tests:** 55
- **Pass Rate:** 100% (55 / 55)
- **Failures / Errors:** 0
- **Suites:** 6 suites including `ResearchAcademicIntegrationTest` (5 tests covering Retrofit requests, MockWebServer assertions, bearer token injection, and DTO serialization).

### 5.3 Android Build Verification
Command: `gradlew.bat assembleDebug`
- **Result:** BUILD SUCCESSFUL in 17s.
- **Artifact:** `android/app/build/outputs/apk/debug/app-debug.apk` compiled and packaged cleanly.

---

## 6. Phase Classification & Stop Gate

- **Phase Status:** `COMPLETE — FULLY VERIFIED`
- **Constraint Compliance:**
  - Preserved existing architecture and Room database tables.
  - Zero mock data or dummy endpoints used.
  - Financial system remains strictly demo/simulated.
  - No automatic advancement to Phase 5B.8.
