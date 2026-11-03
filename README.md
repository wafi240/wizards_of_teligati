# 🧙 WIZARDS OF TELIGATI
**Connect. Collaborate. Grow.**

An exclusive digital ecosystem for KUET students and alumni connecting the community across batches, countries, companies, careers, and projects.

---

## 📁 Project Structure

```
Wizards_of_teligati/
├── android/                 <-- Native Android App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/teligati/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── model/Models.kt
│   │   │   ├── ui/navigation/AppNavigation.kt
│   │   │   ├── ui/screens/
│   │   │   │   ├── HomeScreen.kt          (Professional Feed & KUET Updates)
│   │   │   │   ├── ExploreScreen.kt       (KUETians Around the World & Directory)
│   │   │   │   ├── ProjectsScreen.kt      (Spellbook - Repositories & Contributions)
│   │   │   │   ├── OpportunitiesScreen.kt (Jobs, Assistant Hiring & Tasks)
│   │   │   │   ├── TeligatiAiScreen.kt    (Teligati AI Assistant & Security)
│   │   │   │   └── ProfileScreen.kt       (Verified Profile & Escrow Earnings Ledger)
│   │   │   └── ui/theme/
│   │   │       ├── Color.kt               (Deep Navy, Mystic Cyan, Indigo)
│   │   │       └── Theme.kt
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── backend/                 <-- Spring Boot 3 + PostgreSQL REST API
│   ├── src/main/java/com/teligati/backend/
│   │   ├── controller/ApiController.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Profile.java
│   │   │   ├── Job.java
│   │   │   ├── Project.java
│   │   │   └── Transaction.java
│   │   └── repository/
│   ├── src/main/resources/application.properties
│   └── build.gradle
└── README.md
```

---

## 🚀 How to Open in Android Studio

1. **Launch Android Studio**.
2. Click **Open** (or **File > Open**).
3. In the file picker, select:
   ```
   C:\Users\HP\OneDrive\Desktop\Wizards_of_teligati\android
   ```
   *(Select the **`android`** folder directly so Android Studio recognizes the Gradle build).*
4. Allow Android Studio to sync Gradle dependencies (it will automatically download all required Android 34 libraries).
5. Click the **Run (Green ▶)** button in the top toolbar to start the app on an Android Emulator or connected physical phone!

---

## 📦 How to Build the APK (`WizardsOfTeligati.apk`)

### From Android Studio:
1. In the top menu bar, click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Once the build completes, a popup notification will appear at the bottom right. Click **locate**.
3. Your generated APK is located at:
   ```
   android\app\build\outputs\apk\debug\app-debug.apk
   ```
4. Rename `app-debug.apk` to `WizardsOfTeligati.apk`.

### From Command Line:
```powershell
cd C:\Users\HP\OneDrive\Desktop\Wizards_of_teligati\android
.\gradlew assembleDebug
```

---

## 📲 How to Distribute & Install on a Physical Phone

```
Upload APK (Drive / Dropbox)
          ↓
Share Direct Download Link
          ↓
Android User Downloads `WizardsOfTeligati.apk`
          ↓
Open APK on Phone & Tap "Install"
(Allow "Install unknown apps" if prompted)
          ↓
Open Wizards of Teligati App 🎉
```

---

## 🛡️ Critical Product Rules Implemented
* **No Repository Marketplace / Buying & Selling:** Projects and code repositories exist solely for showcase, learning, and collaboration.
* **Work-Based Compensation Only:** Monetary transactions are strictly tied to real services rendered (Assistant Hiring, Freelance work, Paid contributions, and Jobs) managed by an escrow ledger.
* **Teligati AI Assistant:** Includes security warnings to prevent leaking API keys and smart career matching for KUETians.
