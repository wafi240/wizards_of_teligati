package com.teligati.app.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FirebaseHelper provides a clean, self-contained interface for demonstrating
 * the 4 required Firebase services for university course evaluation:
 * 1. Firebase Authentication (Demo)
 * 2. Firebase Realtime Database
 * 3. Firebase Cloud Firestore
 * 4. Firebase Cloud Storage
 *
 * NOTE: The primary production backend for Wizards of Teligati is Spring Boot.
 * This class and its corresponding activity are kept isolated to avoid altering
 * existing Spring Boot authentication, Room database caching, or role routing.
 */
public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    public interface AuthCallback {
        void onSuccess(FirebaseUser user, String message);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface FirestoreCallback {
        void onSuccess(List<Map<String, Object>> items, String message);
        void onError(String errorMessage);
    }

    public interface ProfileCallback {
        void onSuccess(Map<String, Object> profile);
        void onError(String errorMessage);
    }

    public interface RollLookupCallback {
        void onFound(String email);
        void onNotFound();
        void onError(String error);
    }

    public interface StorageCallback {
        void onSuccess(String downloadUrl, Bitmap downloadedBitmap, String message);
        void onError(String errorMessage);
    }

    private static FirebaseHelper instance;
    private final Context appContext;
    private final FirebaseFirestore firestore;

    private FirebaseHelper(Context context) {
        this.appContext = context.getApplicationContext();
        ensureFirebaseApp(this.appContext);
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseHelper(context);
        }
        return instance;
    }

    /**
     * Ensures Firebase is initialized.
     */
    public static boolean ensureFirebaseApp(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FirebaseApp: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 1. FIREBASE AUTHENTICATION (PRIMARY AUTHENTICATION SYSTEM)
    // =========================================================================

    public FirebaseUser getCurrentUser() {
        try {
            return FirebaseAuth.getInstance().getCurrentUser();
        } catch (Exception e) {
            Log.w(TAG, "FirebaseAuth not ready: " + e.getMessage());
            return null;
        }
    }

    public void registerUser(String email, String password, AuthCallback callback) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            callback.onSuccess(user, "User registered successfully! UID: " + (user != null ? user.getUid() : ""));
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                            callback.onError(err);
                        }
                    });
        } catch (Exception e) {
            callback.onError("Firebase Auth initialization error: " + e.getMessage());
        }
    }

    public void registerDemoUser(String email, String password, AuthCallback callback) {
        registerUser(email, password, callback);
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            callback.onSuccess(user, "Signed in successfully as: " + (user != null ? user.getEmail() : ""));
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Sign-in failed.";
                            callback.onError(err);
                        }
                    });
        } catch (Exception e) {
            callback.onError("Firebase Auth initialization error: " + e.getMessage());
        }
    }

    public void loginDemoUser(String email, String password, AuthCallback callback) {
        loginUser(email, password, callback);
    }

    public void sendPasswordReset(String email, ActionCallback callback) {
        try {
            if (email == null || email.trim().isEmpty()) {
                callback.onError("Please provide a valid email address.");
                return;
            }
            FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess("Password reset instructions sent to: " + email.trim());
                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email.";
                            callback.onError(err);
                        }
                    });
        } catch (Exception e) {
            callback.onError("Password reset exception: " + e.getMessage());
        }
    }

    public void signOut(Context context) {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            Log.w(TAG, "FirebaseAuth sign out error: " + e.getMessage());
        }
        if (context != null) {
            try {
                com.teligati.app.data.SessionManager.getInstance(context).clearSession();
            } catch (Exception e) {
                Log.w(TAG, "SessionManager clear error: " + e.getMessage());
            }
        }
    }

    public void signOutDemoUser() {
        signOut(appContext);
    }

    // =========================================================================
    // USER PROFILE CRUD (FIRESTORE)
    // =========================================================================

    public static Map<String, Object> sanitizePublicProfile(String uid, Map<String, Object> profileData) {
        Map<String, Object> publicCard = new HashMap<>();
        publicCard.put("uid", uid);

        // Required field: name (1..100 chars)
        String name = "KUET Member";
        if (profileData.containsKey("name") && profileData.get("name") != null) {
            String n = profileData.get("name").toString().trim();
            if (!n.isEmpty()) {
                name = n.length() > 100 ? n.substring(0, 100) : n;
            }
        }
        publicCard.put("name", name);

        // Required field: role (STUDENT, ALUMNI, TEACHER)
        String rawRole = "STUDENT";
        if (profileData.containsKey("role") && profileData.get("role") != null) {
            rawRole = profileData.get("role").toString().trim().toUpperCase();
            if (rawRole.startsWith("ROLE_")) {
                rawRole = rawRole.substring(5);
            }
            if ("FACULTY".equals(rawRole)) {
                rawRole = "TEACHER";
            }
        }
        if (!"STUDENT".equals(rawRole) && !"ALUMNI".equals(rawRole) && !"TEACHER".equals(rawRole)) {
            rawRole = "STUDENT";
        }
        publicCard.put("role", rawRole);

        // Optional allowlisted public fields with strict length caps
        if (profileData.containsKey("department") && profileData.get("department") != null) {
            String dept = profileData.get("department").toString().trim();
            if (!dept.isEmpty()) {
                publicCard.put("department", dept.length() > 100 ? dept.substring(0, 100) : dept);
            }
        }

        if (profileData.containsKey("batch") && profileData.get("batch") != null) {
            String batch = profileData.get("batch").toString().trim();
            if (!batch.isEmpty()) {
                publicCard.put("batch", batch.length() > 50 ? batch.substring(0, 50) : batch);
            }
        }

        // Support both "company" and "currentCompany" alias
        String company = null;
        if (profileData.containsKey("company") && profileData.get("company") != null) {
            company = profileData.get("company").toString().trim();
        } else if (profileData.containsKey("currentCompany") && profileData.get("currentCompany") != null) {
            company = profileData.get("currentCompany").toString().trim();
        }
        if (company != null && !company.isEmpty()) {
            publicCard.put("company", company.length() > 100 ? company.substring(0, 100) : company);
        }

        // Support both "position" and "currentPosition" alias
        String position = null;
        if (profileData.containsKey("position") && profileData.get("position") != null) {
            position = profileData.get("position").toString().trim();
        } else if (profileData.containsKey("currentPosition") && profileData.get("currentPosition") != null) {
            position = profileData.get("currentPosition").toString().trim();
        }
        if (position != null && !position.isEmpty()) {
            publicCard.put("position", position.length() > 100 ? position.substring(0, 100) : position);
        }

        if (profileData.containsKey("designation") && profileData.get("designation") != null) {
            String desig = profileData.get("designation").toString().trim();
            if (!desig.isEmpty()) {
                publicCard.put("designation", desig.length() > 100 ? desig.substring(0, 100) : desig);
            }
        }

        // Support both "lab" and "labName" alias
        String lab = null;
        if (profileData.containsKey("lab") && profileData.get("lab") != null) {
            lab = profileData.get("lab").toString().trim();
        } else if (profileData.containsKey("labName") && profileData.get("labName") != null) {
            lab = profileData.get("labName").toString().trim();
        }
        if (lab != null && !lab.isEmpty()) {
            publicCard.put("lab", lab.length() > 100 ? lab.substring(0, 100) : lab);
        }

        // Support both "researchArea" and "researchFocus" alias
        String research = null;
        if (profileData.containsKey("researchArea") && profileData.get("researchArea") != null) {
            research = profileData.get("researchArea").toString().trim();
        } else if (profileData.containsKey("researchFocus") && profileData.get("researchFocus") != null) {
            research = profileData.get("researchFocus").toString().trim();
        }
        if (research != null && !research.isEmpty()) {
            publicCard.put("researchArea", research.length() > 200 ? research.substring(0, 200) : research);
        }

        if (profileData.containsKey("avatarUrl") && profileData.get("avatarUrl") != null) {
            String avatar = profileData.get("avatarUrl").toString().trim();
            if (!avatar.isEmpty()) {
                publicCard.put("avatarUrl", avatar.length() > 500 ? avatar.substring(0, 500) : avatar);
            }
        }

        return publicCard;
    }

    public void saveUserProfile(String uid, Map<String, Object> profileData, ActionCallback callback) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            // Build strictly sanitized public directory document conforming to isValidPublicProfile
            Map<String, Object> publicCard = sanitizePublicProfile(uid, profileData);

            // Execute atomic batch write to both private users collection and public directory
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            batch.set(db.collection("users").document(uid), profileData, com.google.firebase.firestore.SetOptions.merge());
            batch.set(db.collection("public_profiles").document(uid), publicCard, com.google.firebase.firestore.SetOptions.merge());

            batch.commit()
                    .addOnSuccessListener(aVoid -> callback.onSuccess("Firestore profile saved for UID: " + uid))
                    .addOnFailureListener(e -> callback.onError("Failed to save Firestore profile: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Firestore write exception: " + e.getMessage());
        }
    }

    public void checkAuthorizedFaculty(String email, ActionCallback callback) {
        try {
            if (email == null || email.trim().isEmpty()) {
                callback.onError("Please enter a valid institutional faculty email.");
                return;
            }
            final String cleanEmail = email.trim().toLowerCase();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("authorized_faculty").document(cleanEmail).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot != null && snapshot.exists()) {
                            callback.onSuccess("Authorized faculty member");
                        } else {
                            callback.onError("This email is not listed on the KUET Authorized Faculty roster. Faculty accounts must be pre-authorized in the Firebase Console by university administration.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to verify faculty authorization roster: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Faculty authorization check exception: " + e.getMessage());
        }
    }

    public void fetchUserProfile(String uid, ProfileCallback callback) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot != null && snapshot.exists() && snapshot.getData() != null) {
                            callback.onSuccess(snapshot.getData());
                        } else {
                            callback.onError("Profile not found in Firestore for UID: " + uid);
                        }
                    })
                    .addOnFailureListener(e -> callback.onError("Firestore fetch error: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Firestore fetch exception: " + e.getMessage());
        }
    }

    public void fetchPublicDirectoryProfiles(String roleFilter, FirestoreCallback callback) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Query query = db.collection("public_profiles");
            if (roleFilter != null && !roleFilter.trim().isEmpty()) {
                query = query.whereEqualTo("role", roleFilter.trim().toUpperCase());
            }
            query.limit(50).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Map<String, Object>> list = new ArrayList<>();
                        for (QueryDocumentSnapshot snap : queryDocumentSnapshots) {
                            Map<String, Object> data = new HashMap<>(snap.getData());
                            data.put("_id", snap.getId());
                            list.add(data);
                        }
                        callback.onSuccess(list, "Fetched " + list.size() + " public profile(s) from Firestore.");
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to fetch public directory: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Directory query exception: " + e.getMessage());
        }
    }

    public void findEmailByRoll(String roll, RollLookupCallback callback) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .whereEqualTo("roll", roll)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            String email = snapshots.getDocuments().get(0).getString("email");
                            if (email != null && !email.trim().isEmpty()) {
                                callback.onFound(email.trim());
                                return;
                            }
                        }
                        // Check kuetId fallback
                        db.collection("users")
                                .whereEqualTo("kuetId", roll)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(idSnapshots -> {
                                    if (idSnapshots != null && !idSnapshots.isEmpty()) {
                                        String email = idSnapshots.getDocuments().get(0).getString("email");
                                        if (email != null && !email.trim().isEmpty()) {
                                            callback.onFound(email.trim());
                                            return;
                                        }
                                    }
                                    callback.onNotFound();
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (Exception e) {
            callback.onError("Firestore lookup exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. FIREBASE REALTIME DATABASE (LIVE ACTIVITY / STATUS PING)
    // =========================================================================

    public DatabaseReference getStatusReference() {
        try {
            return FirebaseDatabase.getInstance().getReference("demo_activity_status");
        } catch (Exception e) {
            Log.e(TAG, "RTDB reference error: " + e.getMessage());
            return null;
        }
    }

    public void writeStatusPing(String statusText, String authorName, ActionCallback callback) {
        try {
            DatabaseReference ref = getStatusReference();
            if (ref == null) {
                callback.onError("Realtime Database not accessible.");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("statusText", statusText);
            data.put("author", authorName != null && !authorName.isEmpty() ? authorName : "KUETian");
            data.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            data.put("epochMillis", System.currentTimeMillis());

            ref.setValue(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    callback.onSuccess("Realtime status updated successfully to node: /demo_activity_status");
                } else {
                    String err = task.getException() != null ? task.getException().getMessage() : "Failed to write to Realtime DB.";
                    callback.onError(err);
                }
            });
        } catch (Exception e) {
            callback.onError("RTDB write exception: " + e.getMessage());
        }
    }

    public void listenToStatusUpdates(ValueEventListener listener) {
        try {
            DatabaseReference ref = getStatusReference();
            if (ref != null) {
                ref.addValueEventListener(listener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to attach RTDB listener: " + e.getMessage());
        }
    }

    public void removeStatusListener(ValueEventListener listener) {
        try {
            DatabaseReference ref = getStatusReference();
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove RTDB listener: " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. FIREBASE CLOUD FIRESTORE (STRUCTURED ANNOUNCEMENTS / PROJECT RECORDS)
    // =========================================================================

    public void addAnnouncement(String title, String description, String author, ActionCallback callback) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> doc = new HashMap<>();
            doc.put("title", title);
            doc.put("description", description);
            doc.put("author", author != null && !author.isEmpty() ? author : "Wizards Admin");
            doc.put("timestamp", System.currentTimeMillis());
            doc.put("formattedDate", new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date()));

            db.collection("demo_announcements")
                    .add(doc)
                    .addOnSuccessListener(documentReference -> {
                        callback.onSuccess("Announcement saved to Firestore with ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> callback.onError("Firestore write failed: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Firestore exception: " + e.getMessage());
        }
    }

    public void fetchAnnouncements(FirestoreCallback callback) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("demo_announcements")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Map<String, Object>> list = new ArrayList<>();
                        for (QueryDocumentSnapshot snap : queryDocumentSnapshots) {
                            Map<String, Object> data = new HashMap<>(snap.getData());
                            data.put("_id", snap.getId());
                            list.add(data);
                        }
                        callback.onSuccess(list, "Fetched " + list.size() + " announcement(s) from Firestore.");
                    })
                    .addOnFailureListener(e -> callback.onError("Firestore fetch failed: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Firestore exception: " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. FIREBASE CLOUD STORAGE (MEDIA / PROFILE AVATAR STORAGE)
    // =========================================================================

    public void uploadProfileImage(byte[] imageBytes, StorageCallback callback) {
        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            String path = "demo_media/demo_avatar.png";
            StorageReference storageRef = storage.getReference().child(path);

            storageRef.putBytes(imageBytes)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    callback.onSuccess(uri.toString(), null, "Image uploaded to Firebase Storage!\nPath: " + path);
                                })
                                .addOnFailureListener(e -> callback.onError("Upload succeeded, but could not get download URL: " + e.getMessage()));
                    })
                    .addOnFailureListener(e -> callback.onError("Storage upload failed: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Storage exception: " + e.getMessage());
        }
    }

    public void downloadProfileImage(StorageCallback callback) {
        try {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            String path = "demo_media/demo_avatar.png";
            StorageReference storageRef = storage.getReference().child(path);

            final long ONE_MEGABYTE = 1024 * 1024;
            storageRef.getBytes(ONE_MEGABYTE)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        storageRef.getDownloadUrl().addOnCompleteListener(task -> {
                            String url = task.isSuccessful() && task.getResult() != null ? task.getResult().toString() : "Stored at " + path;
                            callback.onSuccess(url, bitmap, "Image downloaded successfully from Firebase Storage (" + bytes.length + " bytes)");
                        });
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to download image from Storage: " + e.getMessage()));
        } catch (Exception e) {
            callback.onError("Storage download exception: " + e.getMessage());
        }
    }

    /**
     * Generates a clean demo avatar Bitmap in memory for demonstration purposes.
     * Prevents requiring file system/camera permissions on emulator.
     */
    public static Bitmap generateDemoAvatarBitmap(String initials) {
        int size = 240;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Circular background
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#1C2541")); // Indigo dark
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint);

        // Border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#00ADB5")); // Mystic Cyan
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8f);
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 4f, borderPaint);

        // Text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#00ADB5"));
        textPaint.setTextSize(64f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        float yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(initials != null && !initials.isEmpty() ? initials : "KUET", size / 2f, yPos, textPaint);

        return bitmap;
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}
