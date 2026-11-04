<div align="center">
  <img src="assets/logo.png" alt="Wizards of Teligati Logo" width="100%" style="max-width: 800px; border-radius: 12px;"/>
  <h1>🧙 WIZARDS OF TELIGATI</h1>
  <h3>Connect. Collaborate. Grow.</h3>
  <p><b>KUET's Professional, Career &amp; Collaboration Network (100% Pure Java)</b></p>
</div>

---

## 🔐 Multi-Role Authentication System

The platform features tailored login and verification gateways for each KUET stakeholder:

1. **🎓 Student Login:**
   * Authenticate via KUET Student Roll (e.g., `2307003`) & Webmail.
   * Direct access to Assistant work (replacing tuition), research co-authorships, and 1-page resume generation.
   * **1-Tap Demo Student Login** available.

2. **💼 Alumni Login:**
   * Authenticate via Passing Batch (e.g., `2K12`), Current Company (e.g., `BMW Munich`), & verified alumni email.
   * Ability to hire personal assistants, post micro-bounties, give official **"KUET Vouches"**, and release escrow payments.
   * **1-Tap Demo Alumni Login** available.

3. **🏛️ Faculty & Teacher Login:**
   * Authenticate via KUET Institutional Email (`azharul@cse.kuet.ac.bd`) & Department designation.
   * Post research lab opportunities, supervise papers, and recruit student co-authors.
   * **1-Tap Demo Faculty Login** available.

---

## 🌟 Strategic New Features Added

* **🔥 "Tuition-to-Tech" Assistant Engine:**
  * One-tap filter on the Careers portal that highlights remote technical assistant roles (৳10,000–৳15,000/mo) designed to replace private tutoring with real engineering experience.
* **⭐ KUET Alumni Vouch System:**
  * Alumni can endorse juniors with an official verified vouch badge directly within the directory.
* **📄 1-Page KUET Verified Tech Resume Exporter:**
  * Generates an exportable tech resume summarizing CGPA (3.84), department ranking, club leaderships (SGIPC, Robonix), and verified skills.
* **🌍 Country Relocation Hubs:**
  * Dedicated portals for Germany (Munich/Berlin) and USA (Silicon Valley/Austin) connecting students to local alumni chapters for housing, visa, and relocation guidance.
* **⚡ Micro-Bounty Escrow Release:**
  * Alumni employers can confirm task completion and release escrowed funds directly into the student's ledger.
* **🪄 Teligati AI Security Scanner:**
  * Real-time secret detection alerting students if they accidentally paste API keys, database passwords, or credentials into code proposals.

---

## 📁 100% Pure Java Architecture

```
Wizards_of_teligati/
├── android/                 <-- Native Android App (100% Pure Java)
│   ├── app/
│   │   ├── src/main/java/com/teligati/app/
│   │   │   ├── auth/
│   │   │   │   ├── AuthGatewayActivity.java       (Role Selection Welcome Portal)
│   │   │   │   ├── StudentLoginActivity.java      (Dedicated Student Login)
│   │   │   │   ├── StudentRegisterActivity.java   (Dedicated Student Registration)
│   │   │   │   ├── AlumniLoginActivity.java       (Dedicated Alumni Login)
│   │   │   │   ├── AlumniRegisterActivity.java    (Dedicated Alumni Registration)
│   │   │   │   ├── FacultyLoginActivity.java      (Dedicated Faculty Login)
│   │   │   │   └── FacultyRegisterActivity.java   (Dedicated Faculty Registration)
│   │   │   ├── LoginActivity.java                 (Alternative Tabbed Role Auth)
│   │   │   ├── MainActivity.java                  (BottomNavigation Host)
│   │   │   ├── adapter/
│   │   │   │   ├── AlumniAdapter.java     (Mentorship & Vouching)
│   │   │   │   ├── OpportunityAdapter.java (Tuition replacement & jobs)
│   │   │   │   ├── ProjectAdapter.java    (Spellbook collaborations)
│   │   │   │   └── TransactionAdapter.java (Escrow ledger)
│   │   │   ├── fragment/
│   │   │   │   ├── HomeFragment.java
│   │   │   │   ├── ExploreFragment.java   (Country hubs & directory)
│   │   │   │   ├── OpportunitiesFragment.java
│   │   │   │   ├── ProjectsFragment.java
│   │   │   │   ├── AiFragment.java        (Teligati AI + Secret Scanner)
│   │   │   │   └── ProfileFragment.java   (Resume export & escrow release)
│   │   │   └── model/
│   │   └── src/main/res/
│   │       ├── layout/
│   │       ├── menu/
│   │       └── mipmap-*/ (Adaptive Custom Wizard Icon)
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── backend/                 <-- Spring Boot 3 REST API (Pure Java 17)
│   ├── src/main/java/com/teligati/backend/
│   └── build.gradle
└── README.md
```

---

## 🛡️ Core Rules
* **No Repository Marketplace / Buying & Selling:** Projects and code exist strictly for learning, showcase, and team collaboration.
* **Work-Based Compensation Only:** Payments are strictly tied to real services rendered (Assistant Hiring, Freelance work, and Jobs) backed by an escrow ledger.

---

## 🚀 How to Run in Android Studio
1. Open Android Studio.
2. Select **File → Open** and choose the **`android`** folder:
   `C:\Users\HP\OneDrive\Desktop\Wizards_of_teligati\android`
3. Click the green **Play button (▶)** to run on your emulator or phone!
4. The app opens directly to the **Multi-Role Login Portal** with 1-tap demo logins for Student, Alumni, and Teacher!
