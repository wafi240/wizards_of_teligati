# Wizards of Teligati — Phase 5E: Project Demonstration Guide

**Project:** Wizards of Teligati (KUET CSE Academic Platform)  
**Phase:** Phase 5E — Final Presentation Packaging  
**Date:** November 06, 2026  
**Target Audience:** KUET CSE Faculty Examination Committee, Evaluators & Project Reviewers  

---

## 1. Project Overview

**Wizards of Teligati** is a role-aware university collaboration platform designed specifically for Khulna University of Engineering & Technology (KUET). It bridges three key stakeholders in the campus ecosystem:

1. **Students:** Discover campus-level assistant work (replacing informal off-campus tuition), find funded research assistantships, submit thesis/PhD proposals, apply for alumni career referrals, and seek direct guidance.
2. **Alumni:** Post global career opportunities (jobs and internships), hire junior students for remote technical assistance with demo escrow protection, and endorse verified KUET students through official recommendation letters.
3. **Faculty / Teachers:** Manage research lab recruitment, publish calls for undergraduate co-authors, evaluate research proposals, mentor student researchers, and provide academic references.

---

## 2. Quick-Start Setup Instructions

### Prerequisites
- **Operating System:** Windows 10/11, Linux, or macOS
- **JDK:** Java 21 (Temurin / JetBrains Runtime 21)
- **Android SDK:** API Level 34+ (Emulator or physical device connected via USB with Developer Mode & USB Debugging enabled)
- **Node / Python (Optional):** Only for testing scripts

---

### Step A: Starting the Spring Boot Backend

The backend includes a self-contained development profile (`dev`) that runs an in-memory PostgreSQL-compatible H2 database with all Flyway migrations (V1 through V8) and automated demo seeders pre-configured.

1. Open a terminal (PowerShell, Command Prompt, or Bash) in the project root:
   ```powershell
   cd backend
   ```
2. Run the application with the `dev` profile:
   ```powershell
   # Windows
   .\gradlew.bat bootRun --args='--spring.profiles.active=dev'

   # Linux / macOS
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```
3. Verify that the backend is healthy:
   Open a browser or terminal and navigate to:
   ```
   http://localhost:8080/api/v1/health
   ```
   Expected response:
   ```json
   {
     "success": true,
     "message": "System is operational",
     "data": {
       "status": "UP",
       "database": "UP",
       "version": "1.0.0-SNAPSHOT"
     }
   }
   ```

---

### Step B: Launching the Android Application

#### Option 1: On an Android Emulator
1. Start your Android Emulator (e.g. `Pixel_8` running API 34+):
   ```powershell
   emulator -avd Pixel_8 -no-boot-anim
   ```
2. Install and launch the latest debug APK:
   ```powershell
   cd android
   .\gradlew.bat installDebug
   ```
   *Alternatively, install directly via ADB:*
   ```powershell
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```

#### Option 2: On a Physical Android Phone
1. Connect your phone via USB with **USB Debugging** enabled.
2. If testing against a backend running on your host PC via USB cable, execute:
   ```powershell
   adb reverse tcp:8080 tcp:8080
   ```
3. Install the APK:
   ```powershell
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 3. Demo Credentials Matrix

For instant, fail-safe demonstration during the presentation, each login screen features a **`⚡ Try Demo Account`** button that populates verified credentials with a single tap.

| Role | Username / Identifier | Password | Quick Login Action | Key Features to Highlight |
|---|---|---|---|---|
| **Student** | `2307003` (Roll) | `demo` | Tap **`⚡ Try Demo Account`** on Student Login | View verified profile, demo wallet balance, browse alumni directory, request career guidance, view assistant jobs. |
| **Alumni** | `tanvir.munich@kuet.ac.bd` | `demo` | Tap **`⚡ Try Demo Account`** on Alumni Login | Manage job postings, reply to student messages, evaluate BMW reference request, write recommendation letter, view demo earnings ledger. |
| **Faculty** | `azharul@cse.kuet.ac.bd` | `demo` | Tap **`⚡ Try Demo Account`** on Faculty Login | Post call for co-authors, review & accept Farhan's research proposal, recruit Wafi into Intelligent Systems Lab. |

---

## 4. Financial Module Notice

> ### ⚠️ DEMO WALLET — SIMULATION ONLY
> **Zero Real Money Involved:** The platform contains a full escrow and wallet demonstration module for educational and evaluative purposes.  
> Every credit amount (e.g., ৳10,000 balance, ৳15,000 assistant salary, €55,000 job posting) is stored as virtual simulation data.  
> No external payment gateways (bKash, Nagad, Stripe, SSLCommerz) or banking APIs are integrated.

---

## 5. Account Switching & Resetting Instructions

To seamlessly switch between roles during the presentation:
1. Tap the navigation drawer (hamburger icon ☰ at top-left) or swipe from the left edge.
2. Select **Settings** from the bottom of the drawer menu.
3. Tap the red button: **`🚪 Sign Out / Logout`**.
4. The application securely revokes the active token, clears the encrypted session, and returns to the role selection screen (**AuthGatewayActivity**).
5. Tap the target role's login button (Student, Alumni, or Faculty) and tap **`⚡ Try Demo Account`**.
