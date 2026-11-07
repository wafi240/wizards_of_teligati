package com.teligati.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.teligati.app.network.model.TokenResponse;
import com.teligati.app.network.model.UserSummaryResponse;

public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String ENCRYPTED_PREF_NAME = "wizards_encrypted_session";
    private static final String FALLBACK_PREF_NAME = "WizardsSession";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_DEPT = "user_dept";
    private static final String KEY_USER_CGPA = "user_cgpa";
    private static final String KEY_IS_VERIFIED = "is_verified";
    private static final String KEY_BACKEND_ID = "backend_id";
    public static final String KEY_FIREBASE_UID = "firebase_uid";

    private static volatile SessionManager INSTANCE;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        this.prefs = initPreferences(context.getApplicationContext());
    }

    // Constructor for testing
    public SessionManager(SharedPreferences preferences) {
        this.prefs = preferences;
    }

    public static SessionManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SessionManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public static void setInstanceForTesting(SessionManager instance) {
        INSTANCE = instance;
    }

    /**
     * Determines whether the current process is executing within a desktop JVM test runner
     * rather than an actual Android OS / ART runtime.
     */
    public static boolean isTestEnvironment() {
        String runtime = System.getProperty("java.runtime.name");
        if (runtime == null) return false;
        String lower = runtime.toLowerCase();
        return lower.contains("openjdk") || lower.contains("java(tm)") || lower.contains("hotspot");
    }

    private SharedPreferences initPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Throwable t) {
            // Strictly forbid silent plaintext fallback on Android runtime
            if (isTestEnvironment()) {
                Log.w(TAG, "Test environment detected; using private SharedPreferences: " + t.getMessage());
                return context.getSharedPreferences(FALLBACK_PREF_NAME, Context.MODE_PRIVATE);
            }
            Log.e(TAG, "FATAL: Android Keystore-backed EncryptedSharedPreferences failed to initialize", t);
            throw new SecurityException("Android Keystore-backed EncryptedSharedPreferences is strictly required. Plaintext fallback is forbidden on Android.", t);
        }
    }

    /**
     * Saves full authenticated session returned by backend.
     * Clears all prior user preferences first to prevent session data leakage between accounts.
     */
    public synchronized void saveAuthSession(TokenResponse tokenResponse) {
        if (tokenResponse == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Prevents previous user's cached details from leaking into new session

        if (tokenResponse.getAccessToken() != null) {
            editor.putString(KEY_ACCESS_TOKEN, tokenResponse.getAccessToken());
        }
        if (tokenResponse.getRefreshToken() != null) {
            editor.putString(KEY_REFRESH_TOKEN, tokenResponse.getRefreshToken());
        }

        UserSummaryResponse user = tokenResponse.getUser();
        if (user != null) {
            if (user.getId() != null) {
                editor.putLong(KEY_BACKEND_ID, user.getId());
            }
            if (user.getRole() != null) {
                editor.putString(KEY_ROLE, normalizeRole(user.getRole()));
            }
            if (user.getEmail() != null) {
                editor.putString(KEY_USER_EMAIL, user.getEmail());
            }
            if (user.getName() != null) {
                editor.putString(KEY_USER_NAME, user.getName());
            }
            if (user.getDepartment() != null) {
                editor.putString(KEY_USER_DEPT, user.getDepartment());
            }
            String id = (user.getKuetId() != null && !user.getKuetId().trim().isEmpty())
                    ? user.getKuetId() : user.getEmail();
            if (id != null) {
                editor.putString(KEY_USER_ID, id);
            }
            editor.putBoolean(KEY_IS_VERIFIED, user.isVerified());
        }

        editor.commit();
    }

    /**
     * Atomically updates access and refresh tokens.
     * Prevents race conditions with logout: if the session was cleared, aborts without reviving the session.
     */
    public synchronized boolean updateTokens(String accessToken, String refreshToken) {
        String currentRefresh = prefs.getString(KEY_REFRESH_TOKEN, null);
        if (currentRefresh == null) {
            // Session was logged out / cleared while refresh was in flight
            return false;
        }

        SharedPreferences.Editor editor = prefs.edit();
        if (accessToken != null) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        return editor.commit();
    }

    /**
     * Legacy helper preserved for backward compatibility.
     */
    public void setSession(String role, String userId) {
        String defaultName = "Samun Sadab Wafi";
        String defaultDept = "CSE";
        float defaultCgpa = 3.84f;

        String normRole = normalizeRole(role);
        if ("ALUMNI".equalsIgnoreCase(normRole)) {
            defaultName = "Engr. Tanvir Ahmed";
            defaultDept = "CSE '15";
            if (userId == null || userId.isEmpty() || "alumni_1".equals(userId)) userId = "tanvir.munich@kuet.ac.bd";
        } else if ("TEACHER".equalsIgnoreCase(normRole) || "FACULTY".equalsIgnoreCase(normRole)) {
            defaultName = "Prof. Dr. K. M. Azharul Hasan";
            defaultDept = "Dept of CSE";
            if (userId == null || userId.isEmpty() || "teacher_1".equals(userId)) userId = "azharul@cse.kuet.ac.bd";
        } else {
            if (userId == null || userId.isEmpty()) userId = "2307003";
        }

        prefs.edit()
                .putString(KEY_ROLE, normRole)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, defaultName)
                .putString(KEY_USER_DEPT, defaultDept)
                .putFloat(KEY_USER_CGPA, defaultCgpa)
                .commit();
    }

    public void updateUserDetails(String name, String dept, float cgpa) {
        prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_DEPT, dept)
                .putFloat(KEY_USER_CGPA, cgpa)
                .commit();
    }

    public static String normalizeRole(String rawRole) {
        if (rawRole == null) return "STUDENT";
        String upper = rawRole.trim().toUpperCase();
        if (upper.startsWith("ROLE_")) {
            upper = upper.substring(5);
        }
        if ("FACULTY".equals(upper)) {
            return "TEACHER";
        }
        return upper;
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public synchronized void setAccessToken(String accessToken) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).commit();
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public synchronized void setRefreshToken(String refreshToken) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).commit();
    }

    public boolean isVerified() {
        return prefs.getBoolean(KEY_IS_VERIFIED, false);
    }

    public synchronized void setVerified(boolean verified) {
        prefs.edit().putBoolean(KEY_IS_VERIFIED, verified).commit();
    }

    public String getUserRole() {
        return prefs.getString(KEY_ROLE, "STUDENT");
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "2307003");
    }

    public Long getBackendId() {
        long id = prefs.getLong(KEY_BACKEND_ID, -1L);
        return id != -1L ? id : null;
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Samun Sadab Wafi");
    }

    public String getUserDept() {
        return prefs.getString(KEY_USER_DEPT, "CSE");
    }

    public float getUserCgpa() {
        return prefs.getFloat(KEY_USER_CGPA, 3.84f);
    }

    public synchronized void saveFirebaseSession(String uid, String email, String role, String name, String dept, String effectiveId) {
        if (uid == null) return;
        String normRole = normalizeRole(role);
        prefs.edit()
                .putString(KEY_FIREBASE_UID, uid)
                .putString(KEY_ROLE, normRole)
                .putString(KEY_USER_EMAIL, email != null ? email : "")
                .putString(KEY_USER_NAME, name != null ? name : "KUET User")
                .putString(KEY_USER_DEPT, dept != null ? dept : "CSE")
                .putString(KEY_USER_ID, (effectiveId != null && !effectiveId.trim().isEmpty()) ? effectiveId.trim() : uid)
                .putBoolean(KEY_IS_VERIFIED, true)
                .commit();
    }

    public String getFirebaseUid() {
        return prefs.getString(KEY_FIREBASE_UID, null);
    }

    public synchronized void clearSession() {
        prefs.edit().clear().commit();
    }

    public boolean isLoggedIn() {
        return prefs.getString(KEY_FIREBASE_UID, null) != null || getAccessToken() != null || prefs.contains(KEY_ROLE);
    }

    public boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(getUserRole());
    }

    public boolean isAlumni() {
        return "ALUMNI".equalsIgnoreCase(getUserRole());
    }

    public boolean isTeacher() {
        String role = getUserRole();
        return "TEACHER".equalsIgnoreCase(role) || "FACULTY".equalsIgnoreCase(role);
    }
}
