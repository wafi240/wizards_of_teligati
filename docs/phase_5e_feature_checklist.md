# Wizards of Teligati — Phase 5E: Comprehensive Feature Checklist

**Project:** Wizards of Teligati (KUET CSE Academic Platform)  
**Phase:** Phase 5E — Final Presentation Packaging  
**Date:** November 06, 2026  
**Evaluation Target:** Project Demonstration & Defense  

---

## 1. Role-Specific Feature Matrix

### 🎓 Student Role Features
| Feature | Screen / Component | Functional Description | Verification Status |
|---|---|---|---|
| **Role-Specific Login** | `StudentLoginActivity` | Roll-based login (`2307003`) with password and one-tap demo button. | ✅ **VERIFIED** |
| **Encrypted JWT Authentication** | `AuthRepository` & `SessionManager` | Keystore encrypted token storage, automatic token header injection. | ✅ **VERIFIED** |
| **Dashboard Overview** | `StudentDashboardFragment` | Role header, KUET verified student badge, demo wallet widget, quick links. | ✅ **VERIFIED** |
| **Alumni Directory** | `StudentAlumniDirectoryFragment` | Search and explore KUET alumni by name, company, country, and tech stack. | ✅ **VERIFIED** |
| **Guidance Request Dialog** | `StudentAlumniDirectoryFragment` | Modal dialog allowing students to send direct career guidance requests to alumni. | ✅ **VERIFIED** |
| **Assistant Jobs Browsing** | `StudentAssistantJobsFragment` | Explore departmental assistantships (automation, grading, tutoring alternatives). | ✅ **VERIFIED** |
| **Research Opportunities** | `StudentDashboardFragment` | Browse faculty-led funded lab openings and thesis topics. | ✅ **VERIFIED** |
| **Direct Messaging** | `MessagesFragment` | Real-time message exchange with alumni and faculty members. | ✅ **VERIFIED** |
| **Notifications Center** | `NotificationsFragment` | In-app alerts for incoming messages, reference letter completions, proposal updates. | ✅ **VERIFIED** |
| **Verified Student Portfolio** | `ProfileFragment` | View CGPA, academic clubs, projects, GitHub/LinkedIn links, and earnings ledger. | ✅ **VERIFIED** |

---

### 💼 Alumni Role Features
| Feature | Screen / Component | Functional Description | Verification Status |
|---|---|---|---|
| **Alumni Login** | `AlumniLoginActivity` | Email-based authentication (`tanvir.munich@kuet.ac.bd`) with one-tap demo button. | ✅ **VERIFIED** |
| **Alumni Portal Subtitle** | `MainActivity` Material Toolbar | Dedicated portal branding with verified badge and department metadata. | ✅ **VERIFIED** |
| **Post Professional Job** | `AlumniProfessionalJobsFragment` | Post global careers and internships for KUET graduates (€55,000 - €70,000). | ✅ **VERIFIED** |
| **Post Assistant Opportunity** | `AlumniAssistantJobsFragment` | Create funded micro-tasks and assistant roles with simulated escrow funding. | ✅ **VERIFIED** |
| **Review Reference Requests** | `AlumniReferenceRequestsFragment` | View incoming referral requests from KUET juniors applying for global tech companies. | ✅ **VERIFIED** |
| **Official Recommendation Authoring** | `AlumniReferenceRequestsFragment` | Review student profile, accept request, draft and submit recommendation letter. | ✅ **VERIFIED** |
| **Reply to Direct Messages** | `MessagesFragment` | View student inquiry threads and send replies back to student inboxes. | ✅ **VERIFIED** |
| **Earnings & Escrow Ledger** | `ProfileFragment` | Simulated demo ledger displaying virtual available balance and escrow holdings. | ✅ **VERIFIED** |
| **Student Applicants Review** | `AlumniStudentApplicantsFragment` | Review applicants applying for posted assistant opportunities. | ✅ **VERIFIED** |

---

### 🏛️ Faculty / Teacher Role Features
| Feature | Screen / Component | Functional Description | Verification Status |
|---|---|---|---|
| **Faculty Login** | `FacultyLoginActivity` | Institutional email login (`azharul@cse.kuet.ac.bd`) with one-tap demo button. | ✅ **VERIFIED** |
| **Faculty Portal Branding** | `MainActivity` Material Toolbar | Dedicated faculty theme, department leadership metadata, lab affiliation. | ✅ **VERIFIED** |
| **Call for Co-Authors** | `TeacherResearchPapersFragment` | Post conference/journal calls seeking undergraduate co-authors with DOI/topics. | ✅ **VERIFIED** |
| **Public Research Openings** | `TeacherResearchOpportunitiesFragment` | Create public funded lab openings with monthly stipends (e.g. ৳12,000 / month). | ✅ **VERIFIED** |
| **Research Proposals Evaluation** | `TeacherProposalsFragment` | Review incoming PhD, MSc, and BSc thesis proposals (e.g. Federated Learning IoT). | ✅ **VERIFIED** |
| **Accept/Reject Proposals with Feedback** | `TeacherProposalsFragment` | Modal feedback dialog allowing faculty to accept or request revision on topics. | ✅ **VERIFIED** |
| **Student Researchers Recruitment** | `TeacherStudentResearchersFragment` | View student candidates, inspect CGPA and skills, accept candidates into lab. | ✅ **VERIFIED** |
| **Academic Reference Management** | `AlumniReferenceRequestsFragment` | Review academic endorsement requests for higher education / scholarships. | ✅ **VERIFIED** |

---

## 2. Cross-Cutting & Platform Features

| Capability | Module | Verification Summary | Status |
|---|---|---|---|
| **Role-Based Access Control (RBAC)** | Spring Security + Android Gateway | Endpoints and screens strictly partitioned by role (`STUDENT`, `ALUMNI`, `TEACHER`). Non-authorized access blocked with HTTP 403 / Access Denied toasts. | ✅ **VERIFIED** |
| **Demo Financial Ledger (Simulation)** | `WalletController` + `ProfileFragment` | Virtual credits, demo deposits, simulated withdrawals, escrow release. Strictly zero real payment gateways. | ✅ **VERIFIED** |
| **Teligati AI Career Advisor** | `AiFragment` & Floating Action Button | Rule-based and pattern matching career matching: tuition replacement, lab discovery, and security scan. | ✅ **VERIFIED** |
| **Offline-First Room Caching** | Android Architecture Components | Local SQLite/Room entities cache profiles, jobs, papers, messages, and references for smooth offline presentation. | ✅ **VERIFIED** |
| **Flyway Database Migrations** | Spring Boot Flyway (V1–V8) | Clean, reproducible database schema with automated validation across fresh installations. | ✅ **VERIFIED** |
| **Clean Logout & Session Reset** | `SessionManager` & `SettingsFragment` | Secure single-tap logout that completely wipes memory and storage tokens before transitioning to login gateway. | ✅ **VERIFIED** |
