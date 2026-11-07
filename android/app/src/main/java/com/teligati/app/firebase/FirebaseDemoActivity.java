package com.teligati.app.firebase;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.teligati.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FirebaseDemoActivity provides a standalone, interactive dashboard
 * to demonstrate the four Firebase services required for the KUET university course:
 * 1. Firebase Authentication
 * 2. Firebase Realtime Database
 * 3. Firebase Cloud Firestore
 * 4. Firebase Cloud Storage
 *
 * It is completely decoupled from the primary Spring Boot application backend.
 */
public class FirebaseDemoActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private ValueEventListener rtdbListener;

    // UI - Auth
    private EditText etAuthEmail;
    private EditText etAuthPassword;
    private Button btnAuthRegister;
    private Button btnAuthLogin;
    private Button btnAuthLogout;
    private TextView tvAuthStatus;

    // UI - RTDB
    private EditText etRtdbStatus;
    private Button btnRtdbUpdate;
    private TextView tvRtdbLiveValue;

    // UI - Firestore
    private EditText etFirestoreTitle;
    private EditText etFirestoreDesc;
    private Button btnFirestoreAdd;
    private Button btnFirestoreFetch;
    private TextView tvFirestoreOutput;

    // UI - Storage
    private ImageView ivStorageAvatar;
    private Button btnStorageUpload;
    private Button btnStorageDownload;
    private TextView tvStorageStatus;

    // UI - Log
    private TextView tvEventLog;
    private Button btnClearLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_demo);

        firebaseHelper = FirebaseHelper.getInstance(this);

        initViews();
        setupToolbar();
        setupAuthListeners();
        setupRtdbListeners();
        setupFirestoreListeners();
        setupStorageListeners();
        setupLogView();

        refreshAuthStatus();
        appendLog("SYS", "Firebase Course Demo initialized. Ready for evaluation.");
    }

    private void initViews() {
        etAuthEmail = findViewById(R.id.etAuthEmail);
        etAuthPassword = findViewById(R.id.etAuthPassword);
        btnAuthRegister = findViewById(R.id.btnAuthRegister);
        btnAuthLogin = findViewById(R.id.btnAuthLogin);
        btnAuthLogout = findViewById(R.id.btnAuthLogout);
        tvAuthStatus = findViewById(R.id.tvAuthStatus);

        etRtdbStatus = findViewById(R.id.etRtdbStatus);
        btnRtdbUpdate = findViewById(R.id.btnRtdbUpdate);
        tvRtdbLiveValue = findViewById(R.id.tvRtdbLiveValue);

        etFirestoreTitle = findViewById(R.id.etFirestoreTitle);
        etFirestoreDesc = findViewById(R.id.etFirestoreDesc);
        btnFirestoreAdd = findViewById(R.id.btnFirestoreAdd);
        btnFirestoreFetch = findViewById(R.id.btnFirestoreFetch);
        tvFirestoreOutput = findViewById(R.id.tvFirestoreOutput);
        tvFirestoreOutput.setMovementMethod(new ScrollingMovementMethod());

        ivStorageAvatar = findViewById(R.id.ivStorageAvatar);
        btnStorageUpload = findViewById(R.id.btnStorageUpload);
        btnStorageDownload = findViewById(R.id.btnStorageDownload);
        tvStorageStatus = findViewById(R.id.tvStorageStatus);

        tvEventLog = findViewById(R.id.tvEventLog);
        tvEventLog.setMovementMethod(new ScrollingMovementMethod());
        btnClearLog = findViewById(R.id.btnClearLog);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarFirebase);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    // =========================================================================
    // 1. AUTHENTICATION DEMO LOGIC
    // =========================================================================

    private void setupAuthListeners() {
        btnAuthRegister.setOnClickListener(v -> {
            String email = etAuthEmail.getText().toString().trim();
            String password = etAuthPassword.getText().toString().trim();

            if (email.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "Enter valid email & 6+ char password", Toast.LENGTH_SHORT).show();
                return;
            }

            appendLog("AUTH", "Initiating registration for: " + email);
            btnAuthRegister.setEnabled(false);

            firebaseHelper.registerDemoUser(email, password, new FirebaseHelper.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user, String message) {
                    btnAuthRegister.setEnabled(true);
                    appendLog("AUTH", "SUCCESS: " + message);
                    Toast.makeText(FirebaseDemoActivity.this, message, Toast.LENGTH_SHORT).show();
                    refreshAuthStatus();
                }

                @Override
                public void onError(String errorMessage) {
                    btnAuthRegister.setEnabled(true);
                    appendLog("AUTH", "ERROR: " + errorMessage);
                    Toast.makeText(FirebaseDemoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnAuthLogin.setOnClickListener(v -> {
            String email = etAuthEmail.getText().toString().trim();
            String password = etAuthPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            appendLog("AUTH", "Attempting login for: " + email);
            btnAuthLogin.setEnabled(false);

            firebaseHelper.loginDemoUser(email, password, new FirebaseHelper.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user, String message) {
                    btnAuthLogin.setEnabled(true);
                    appendLog("AUTH", "SUCCESS: " + message);
                    Toast.makeText(FirebaseDemoActivity.this, message, Toast.LENGTH_SHORT).show();
                    refreshAuthStatus();
                }

                @Override
                public void onError(String errorMessage) {
                    btnAuthLogin.setEnabled(true);
                    appendLog("AUTH", "ERROR: " + errorMessage);
                    Toast.makeText(FirebaseDemoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnAuthLogout.setOnClickListener(v -> {
            firebaseHelper.signOutDemoUser();
            appendLog("AUTH", "Signed out demo user.");
            Toast.makeText(this, "Signed out from Firebase Auth", Toast.LENGTH_SHORT).show();
            refreshAuthStatus();
        });
    }

    private void refreshAuthStatus() {
        FirebaseUser user = firebaseHelper.getCurrentUser();
        if (user != null) {
            String status = "Firebase User: " + user.getEmail() + "\nUID: " + user.getUid();
            tvAuthStatus.setText(status);
            tvAuthStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.mystic_cyan));
        } else {
            tvAuthStatus.setText("Auth Status: Not logged in (Guest demo state)");
            tvAuthStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    // =========================================================================
    // 2. REALTIME DATABASE DEMO LOGIC
    // =========================================================================

    private void setupRtdbListeners() {
        btnRtdbUpdate.setOnClickListener(v -> {
            String statusText = etRtdbStatus.getText().toString().trim();
            if (statusText.isEmpty()) {
                Toast.makeText(this, "Enter status text", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = firebaseHelper.getCurrentUser();
            String author = user != null && user.getEmail() != null ? user.getEmail() : "KUET Student Demo";

            appendLog("RTDB", "Writing status update to /demo_activity_status...");
            btnRtdbUpdate.setEnabled(false);

            firebaseHelper.writeStatusPing(statusText, author, new FirebaseHelper.ActionCallback() {
                @Override
                public void onSuccess(String message) {
                    btnRtdbUpdate.setEnabled(true);
                    appendLog("RTDB", "SUCCESS: " + message);
                    Toast.makeText(FirebaseDemoActivity.this, "RTDB Updated!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    btnRtdbUpdate.setEnabled(true);
                    appendLog("RTDB", "ERROR: " + errorMessage);
                    Toast.makeText(FirebaseDemoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        // Set up real-time listener for live sync demonstration
        rtdbListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String statusText = snapshot.child("statusText").getValue(String.class);
                    String author = snapshot.child("author").getValue(String.class);
                    String timestamp = snapshot.child("timestamp").getValue(String.class);

                    String display = "📢 " + (statusText != null ? statusText : "No status") +
                            "\n👤 By: " + (author != null ? author : "Unknown") +
                            "  ⏰ " + (timestamp != null ? timestamp : "N/A");
                    tvRtdbLiveValue.setText(display);
                    appendLog("RTDB_SYNC", "Realtime update received: \"" + statusText + "\"");
                } else {
                    tvRtdbLiveValue.setText("Node /demo_activity_status is empty.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                appendLog("RTDB_ERROR", "Listener cancelled: " + error.getMessage());
            }
        };
    }

    // =========================================================================
    // 3. FIRESTORE DEMO LOGIC
    // =========================================================================

    private void setupFirestoreListeners() {
        btnFirestoreAdd.setOnClickListener(v -> {
            String title = etFirestoreTitle.getText().toString().trim();
            String desc = etFirestoreDesc.getText().toString().trim();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Enter announcement title and details", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = firebaseHelper.getCurrentUser();
            String author = user != null && user.getEmail() != null ? user.getEmail() : "KUET Authority";

            appendLog("FIRESTORE", "Adding document to collection 'demo_announcements'...");
            btnFirestoreAdd.setEnabled(false);

            firebaseHelper.addAnnouncement(title, desc, author, new FirebaseHelper.ActionCallback() {
                @Override
                public void onSuccess(String message) {
                    btnFirestoreAdd.setEnabled(true);
                    appendLog("FIRESTORE", "SUCCESS: " + message);
                    Toast.makeText(FirebaseDemoActivity.this, "Saved to Firestore!", Toast.LENGTH_SHORT).show();
                    // Auto refresh list
                    fetchFirestoreAnnouncements();
                }

                @Override
                public void onError(String errorMessage) {
                    btnFirestoreAdd.setEnabled(true);
                    appendLog("FIRESTORE", "ERROR: " + errorMessage);
                    Toast.makeText(FirebaseDemoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnFirestoreFetch.setOnClickListener(v -> fetchFirestoreAnnouncements());
    }

    private void fetchFirestoreAnnouncements() {
        appendLog("FIRESTORE", "Fetching documents from 'demo_announcements'...");
        btnFirestoreFetch.setEnabled(false);

        firebaseHelper.fetchAnnouncements(new FirebaseHelper.FirestoreCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> items, String message) {
                btnFirestoreFetch.setEnabled(true);
                appendLog("FIRESTORE", "SUCCESS: " + message);

                if (items.isEmpty()) {
                    tvFirestoreOutput.setText("No announcements found in Firestore collection.");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                int idx = 1;
                for (Map<String, Object> doc : items) {
                    sb.append(idx++).append(". ").append(doc.get("title")).append("\n");
                    sb.append("   ").append(doc.get("description")).append("\n");
                    sb.append("   [Author: ").append(doc.get("author")).append(" | ")
                            .append(doc.get("formattedDate")).append("]\n\n");
                }
                tvFirestoreOutput.setText(sb.toString().trim());
            }

            @Override
            public void onError(String errorMessage) {
                btnFirestoreFetch.setEnabled(true);
                appendLog("FIRESTORE", "ERROR: " + errorMessage);
                Toast.makeText(FirebaseDemoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // =========================================================================
    // 4. STORAGE DEMO LOGIC
    // =========================================================================

    private void setupStorageListeners() {
        btnStorageUpload.setOnClickListener(v -> {
            appendLog("STORAGE", "Generating demo avatar bitmap in-memory...");
            Bitmap avatarBitmap = FirebaseHelper.generateDemoAvatarBitmap("KUET");
            ivStorageAvatar.setImageBitmap(avatarBitmap);

            byte[] bytes = FirebaseHelper.bitmapToByteArray(avatarBitmap);
            appendLog("STORAGE", "Uploading sample avatar (" + bytes.length + " bytes) to Firebase Storage...");
            btnStorageUpload.setEnabled(false);

            firebaseHelper.uploadProfileImage(bytes, new FirebaseHelper.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl, Bitmap downloadedBitmap, String message) {
                    btnStorageUpload.setEnabled(true);
                    appendLog("STORAGE", "SUCCESS: " + message);
                    tvStorageStatus.setText("Uploaded!\nURL: " + downloadUrl);
                    Toast.makeText(FirebaseDemoActivity.this, "Avatar Uploaded to Storage!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    btnStorageUpload.setEnabled(true);
                    appendLog("STORAGE", "ERROR: " + errorMessage);
                    tvStorageStatus.setText("Upload Failed: " + errorMessage);
                    Toast.makeText(FirebaseDemoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        btnStorageDownload.setOnClickListener(v -> {
            appendLog("STORAGE", "Downloading demo avatar from Firebase Storage...");
            btnStorageDownload.setEnabled(false);

            firebaseHelper.downloadProfileImage(new FirebaseHelper.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl, Bitmap downloadedBitmap, String message) {
                    btnStorageDownload.setEnabled(true);
                    appendLog("STORAGE", "SUCCESS: " + message);
                    if (downloadedBitmap != null) {
                        ivStorageAvatar.setImageBitmap(downloadedBitmap);
                    }
                    tvStorageStatus.setText("Downloaded successfully!\nLocation: " + downloadUrl);
                    Toast.makeText(FirebaseDemoActivity.this, "Avatar downloaded!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    btnStorageDownload.setEnabled(true);
                    appendLog("STORAGE", "ERROR: " + errorMessage);
                    tvStorageStatus.setText("Download Failed: " + errorMessage);
                    Toast.makeText(FirebaseDemoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // =========================================================================
    // 5. EVENT / DIAGNOSTIC LOG
    // =========================================================================

    private void setupLogView() {
        btnClearLog.setOnClickListener(v -> tvEventLog.setText("Log cleared."));
    }

    private void appendLog(String tag, String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timestamp + "][" + tag + "] " + message + "\n";
        tvEventLog.append(logEntry);

        // Auto-scroll to bottom of log
        final int scrollAmount = tvEventLog.getLayout() != null ?
                tvEventLog.getLayout().getLineTop(tvEventLog.getLineCount()) - tvEventLog.getHeight() : 0;
        if (scrollAmount > 0) {
            tvEventLog.scrollTo(0, scrollAmount);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (rtdbListener != null) {
            firebaseHelper.listenToStatusUpdates(rtdbListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (rtdbListener != null) {
            firebaseHelper.removeStatusListener(rtdbListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAuthStatus();
    }
}
