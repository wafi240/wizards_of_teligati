# Wizards of Teligati — Phase 5E: Presentation & Live Demonstration Outline

**Project:** Wizards of Teligati (KUET Academic Platform)  
**Phase:** Phase 5E — Final Presentation Packaging  
**Duration:** 8 to 10 Minutes Live Demonstration  
**Target:** Final Year CSE Project Defense / Examination Board  

---

## 1. Presentation Timeline & Demonstration Script

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DEMONSTRATION TIMELINE                          │
├─────────────┬──────────────────────────────────────────┬───────────────┤
│ 00:00-01:30 │ Motivation & Architecture Overview       │ Slides        │
│ 01:30-04:00 │ Student Experience (Jobs, Directory, AI) │ Live App      │
│ 04:00-06:30 │ Alumni Experience (Messaging & Referral) │ Live App      │
│ 06:30-08:30 │ Faculty Experience (Proposals & Lab)     │ Live App      │
│ 08:30-10:00 │ Technical Stack, Limitations & Q&A       │ Slides / Code │
└─────────────┴──────────────────────────────────────────┴───────────────┘
```

---

### Segment 1: Motivation & Problem Statement (00:00 – 01:30)
- **Speaker Points:**
  - *"At KUET, undergraduate students face a common challenge: spending 15–20 hours a week on informal private tuitions to cover living expenses, diverting valuable time away from core computer science problem solving, research, and technical projects."*
  - *"Simultaneously, KUET alumni in global tech companies (BMW, Google, Tech Corp) and KUET faculty members leading research labs actively seek reliable, hardworking junior students for paid technical assistant work and research papers, but lacked a verified campus ecosystem."*
  - *"Wizards of Teligati solves this with a verified three-party platform: Students, Alumni, and Faculty."*
- **Slide Reference:** Problem statement, target user personas, and high-level architecture diagram.

---

### Segment 2: Student Experience (Live Demo) (01:30 – 04:00)
- **Action 1: Launch App & Student Login**
  - Launch application on the emulator or phone.
  - On `AuthGatewayActivity`, point out the three distinct roles.
  - Tap **Student Login** $\to$ tap **`⚡ Try Demo Account`** (`2307003` / `demo`).
  - *Talking Point:* *"Authentication is backed by cryptographic verification against the KUET roll registry using Spring Security and JWT tokens."*
- **Action 2: Student Dashboard & Demo Ledger**
  - Point out the verified badge: **"Wafi (Student) ✓ Verified Student"**.
  - Show the Demo Wallet card: *"Demonstrates simulated virtual earnings (৳10,000 available), strictly zero real money involved."*
- **Action 3: Alumni Directory & Career Guidance Request**
  - Open drawer $\to$ tap **Alumni Directory**.
  - Browse cards: point out Engr. Tanvir Ahmed (BMW Munich) and Rahman Ahmed (ABC Technologies).
  - Tap **Request Guidance** on Tanvir's card.
  - Type message: *"Interested in your work at BMW Munich. Could I seek career guidance?"* $\to$ tap **Send**.
  - *Talking Point:* *"This immediately establishes a persistent conversation thread on the backend and triggers a real-time notification."*
- **Action 4: Teligati AI Career Advisor**
  - Tap the floating action button (FAB 🪄).
  - Tap the **Replace Tuitions** chip $\to$ tap **Send**.
  - Show AI response recommending an alumni automation assistantship at ৳15,000/month replacing tuition hours.
- **Action 5: Logout**
  - Open drawer $\to$ **Settings** $\to$ tap **`🚪 Sign Out / Logout`**.
  - Point out the clean return to the gateway screen.

---

### Segment 3: Alumni Experience (Live Demo) (04:00 – 06:30)
- **Action 1: Alumni Login**
  - Tap **Alumni Login** $\to$ tap **`⚡ Try Demo Account`** (`tanvir.munich@kuet.ac.bd` / `demo`).
  - Point out toolbar branding: **"Alumni Portal"** with verified badge.
- **Action 2: Real-Time Messaging Reply**
  - Tap the message icon in the toolbar.
  - Show the active thread: **Thread: Wafi (Student)**.
  - Show student Wafi's inquiry received earlier.
  - Send reply: *"Glad to connect Wafi! Happy to guide you."* $\to$ Send.
- **Action 3: Reference & Recommendation Letter Workflow**
  - Tap bottom navigation **Requests**.
  - Show incoming request from Samun Sadab Wafi for **BMW Group** (Status: *Pending*).
  - Tap **Accept / Endorse** $\to$ tap **ACCEPT & WRITE LETTER**.
  - Review the official recommendation dialog with pre-filled KUET credentials $\to$ tap **SUBMIT LETTER**.
  - Show real-time card update to **Target: BMW Group • COMPLETED • Letter ✓**.
  - *Talking Point:* *"This replaces informal, lost email requests with a structured, verified alumni voucher system."*
- **Action 4: Logout**
  - Open drawer $\to$ **Settings** $\to$ tap **`🚪 Sign Out / Logout`**.

---

### Segment 4: Faculty / Teacher Experience (Live Demo) (06:30 – 08:30)
- **Action 1: Faculty Login**
  - Tap **Faculty Login** $\to$ tap **`⚡ Try Demo Account`** (`azharul@cse.kuet.ac.bd` / `demo`).
  - Point out toolbar branding: **"Faculty Portal"** with Prof. Dr. K. M. Azharul Hasan.
- **Action 2: Call for Co-Authors**
  - Tap bottom navigation **Papers**.
  - Tap **`+ Publish Call for Co-Authors`**.
  - Enter: *"Distributed Sensor Network Consensus"* $\to$ tap **PUBLISH TO LAB**.
  - Show the publication card appearing at the top of the conference calls list.
- **Action 3: Proposal Review & Supervision Acceptance**
  - Tap bottom navigation **Proposals**.
  - Locate proposal: *"Adaptive Federated Learning for Distributed Medical IoT Devices"* (Farhan Sadik, Roll 1907008).
  - Tap **Accept** $\to$ confirm supervisor feedback $\to$ status updates to **Accepted ✓**.
- **Action 4: Lab Researcher Recruitment**
  - Open drawer $\to$ tap **Student Researchers**.
  - Find applicant Samun Sadab Wafi (Roll 2307003, CGPA 3.84).
  - Tap **Accept Lab** $\to$ tap **ACCEPT INTO LAB** $\to$ badge updates to **Accepted ✓**.

---

### Segment 5: Architecture, Technologies & Conclusion (08:30 – 10:00)
- **Key Technologies Summary:**
  - **Mobile:** Android (Java/Kotlin), Material Design 3, Retrofit 2, OkHttp 3, Room SQLite, Android Jetpack.
  - **Backend:** Spring Boot 3.2.4 (Java 21), Spring Data JPA, Spring Security (Stateless JWT), Flyway (V1–V8).
  - **Database:** PostgreSQL / H2 in PostgreSQL compatibility mode.
  - **Quality Assurance:** 147 backend tests + 67 Android unit tests (214 automated tests, 100% pass rate).
- **Final Takeaway:**
  - *"Wizards of Teligati is built specifically for KUET's academic reality—simple to run, reliable to evaluate, and directly addressing students' tuition displacement and academic collaboration needs."*

---

## 2. Anticipated Panel Questions & Answers

| # | Anticipated Question | Recommended Answer |
|---|---|---|
| 1 | *Why did you build your own backend instead of using Firebase?* | *"Firebase provides quick real-time sync, but lacks relational integrity, Flyway version-controlled migrations, custom role-based business rules, and campus data ownership. Spring Boot with JPA gives KUET full control over data residency and institutional schema."* |
| 2 | *Is the financial transaction real?* | *"No, absolutely not. As required for a university project, this is strictly a simulated demo ledger with virtual credits to demonstrate freelancing workflows, escrow locks, and milestone payouts without financial compliance liability."* |
| 3 | *How is identity verified?* | *"Students verify their KUET Roll via institutional email. Alumni and Faculty receive cryptographic invitation tokens verified against departmental alumni records before account activation."* |
| 4 | *What happens if the device is offline?* | *"The application uses an offline-first architecture powered by Room. Local SQLite caches all profiles, jobs, papers, and messages, so the app remains fully functional and browseable even if the campus Wi-Fi drops."* |
