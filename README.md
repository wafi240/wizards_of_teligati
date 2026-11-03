# 🧙 WIZARDS OF TELIGATI
**Connect. Collaborate. Grow.**

An exclusive digital ecosystem and collaboration network for KUET students, alumni, and faculty.
Built completely in **Java** (Pure Java Native Android Application + Java Spring Boot 3 Backend).

---

## 📁 Project Architecture (100% Java)

```
Wizards_of_teligati/
├── android/                 <-- Native Android App (100% Pure Java)
│   ├── app/
│   │   ├── src/main/java/com/teligati/app/
│   │   │   ├── MainActivity.java
│   │   │   ├── adapter/
│   │   │   │   ├── AlumniAdapter.java
│   │   │   │   ├── OpportunityAdapter.java
│   │   │   │   ├── ProjectAdapter.java
│   │   │   │   └── TransactionAdapter.java
│   │   │   ├── fragment/
│   │   │   │   ├── HomeFragment.java
│   │   │   │   ├── ExploreFragment.java
│   │   │   │   ├── OpportunitiesFragment.java
│   │   │   │   ├── ProjectsFragment.java
│   │   │   │   ├── AiFragment.java
│   │   │   │   └── ProfileFragment.java
│   │   │   └── model/
│   │   │       ├── AlumniProfile.java
│   │   │       ├── Opportunity.java
│   │   │       ├── OpportunityCategory.java
│   │   │       ├── ProjectItem.java
│   │   │       ├── SampleData.java
│   │   │       └── TransactionRecord.java
│   │   └── src/main/res/
│   │       ├── layout/
│   │       ├── menu/
│   │       └── values/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── backend/                 <-- Spring Boot 3 REST API (100% Pure Java 17)
│   ├── src/main/java/com/teligati/backend/
│   │   ├── BackendApplication.java
│   │   ├── controller/ApiController.java
│   │   ├── model/
│   │   └── repository/
│   └── build.gradle
└── README.md
```

---

## 🚀 How to Run in Android Studio

1. Open **Android Studio**.
2. Click **File → Open** and select the **`android`** folder:
   ```
   C:\Users\HP\OneDrive\Desktop\Wizards_of_teligati\android
   ```
3. Click the green **Play button (▶)** in the top toolbar to launch on your Pixel 8 emulator or real device.
4. Or generate the `.apk` via **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

---

## 🛡️ Core Product Rules
* **No Repository Marketplace / Buying & Selling:** Code repositories exist strictly for portfolio showcases, learning, and collaboration.
* **Work-Based Compensation:** Payments are strictly for genuine professional work (Alumni Assistant hiring, freelance tasks, paid contributions, and jobs) tracked via an escrow transaction ledger.
