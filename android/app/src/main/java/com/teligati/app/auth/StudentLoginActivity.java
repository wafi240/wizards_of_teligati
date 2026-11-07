package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.teligati.app.MainActivity;
import com.teligati.app.R;
import com.teligati.app.data.SessionManager;
import com.teligati.app.firebase.FirebaseHelper;

import java.util.HashMap;
import java.util.Map;

public class StudentLoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        firebaseHelper = FirebaseHelper.getInstance(this);

        etEmail    = findViewById(R.id.etStudentEmail);
        etPassword = findViewById(R.id.etStudentPassword);
        btnLogin   = findViewById(R.id.btnLoginStudent);

        btnLogin.setOnClickListener(v -> performLogin(false));

        findViewById(R.id.btnStudentDemo).setOnClickListener(v -> {
            etEmail.setText("2307003@stud.kuet.ac.bd");
            etPassword.setText("demo123");
            performLogin(true);
        });

        TextView tvForgot = findViewById(R.id.tvStudentForgotPassword);
        if (tvForgot != null) {
            tvForgot.setOnClickListener(v -> showPasswordResetDialog());
        }

        findViewById(R.id.tvGoToStudentRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentRegisterActivity.class));
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(StudentLoginActivity.this, AuthGatewayActivity.class));
                finish();
            }
        });
    }

    private void performLogin(boolean isDemo) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Enter your password", Toast.LENGTH_SHORT).show();
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Authenticating with Firebase...");

        proceedFirebaseAuth(email, password, isDemo);
    }

    private void proceedFirebaseAuth(String email, String password, boolean isDemo) {
        firebaseHelper.loginUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String message) {
                if (user == null) {
                    onAuthFailure("Firebase user record is empty.");
                    return;
                }
                String uid = user.getUid();
                fetchProfileAndNavigate(uid, email);
            }

            @Override
            public void onError(String errorMessage) {
                if (isDemo && (errorMessage.contains("no user record")
                        || errorMessage.contains("INVALID_LOGIN_CREDENTIALS")
                        || errorMessage.contains("user-not-found")
                        || errorMessage.toLowerCase().contains("credential")
                        || errorMessage.toLowerCase().contains("incorrect")
                        || errorMessage.toLowerCase().contains("malformed")
                        || errorMessage.toLowerCase().contains("password"))) {
                    // Auto-provision demo account in Firebase Auth
                    autoProvisionDemoStudent(email, password);
                } else {
                    onAuthFailure(errorMessage);
                }
            }
        });
    }

    private void autoProvisionDemoStudent(String email, String password) {
        firebaseHelper.registerUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String message) {
                String uid = user != null ? user.getUid() : "";
                Map<String, Object> profile = new HashMap<>();
                profile.put("uid", uid);
                profile.put("name", "Samun Sadab Wafi");
                profile.put("roll", "2307003");
                profile.put("kuetId", "2307003");
                profile.put("department", "CSE");
                profile.put("batch", "2K23");
                profile.put("cgpa", "3.84");
                profile.put("email", email);
                profile.put("role", "STUDENT");
                profile.put("createdAt", System.currentTimeMillis());

                firebaseHelper.saveUserProfile(uid, profile, new FirebaseHelper.ActionCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        fetchProfileAndNavigate(uid, email);
                    }

                    @Override
                    public void onError(String err) {
                        fetchProfileAndNavigate(uid, email);
                    }
                });
            }

            @Override
            public void onError(String err) {
                onAuthFailure("Demo student provisioning failed: " + err);
            }
        });
    }

    private void fetchProfileAndNavigate(String uid, String email) {
        firebaseHelper.fetchUserProfile(uid, new FirebaseHelper.ProfileCallback() {
            @Override
            public void onSuccess(Map<String, Object> profile) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In");

                String role = "STUDENT";
                if (profile.containsKey("role") && profile.get("role") != null) {
                    role = SessionManager.normalizeRole(profile.get("role").toString());
                }

                if (!"STUDENT".equalsIgnoreCase(role)) {
                    firebaseHelper.signOut(StudentLoginActivity.this);
                    new AlertDialog.Builder(StudentLoginActivity.this)
                            .setTitle("Role Mismatch")
                            .setMessage("This account is registered in Firestore as " + role + ". Please use the " + role + " Login screen.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                String name = profile.containsKey("name") && profile.get("name") != null ? profile.get("name").toString() : "Student";
                String dept = profile.containsKey("department") && profile.get("department") != null ? profile.get("department").toString() : "CSE";

                String effectiveId = null;
                if (profile.containsKey("roll") && profile.get("roll") != null && !profile.get("roll").toString().trim().isEmpty()) {
                    effectiveId = profile.get("roll").toString().trim();
                } else if (profile.containsKey("kuetId") && profile.get("kuetId") != null && !profile.get("kuetId").toString().trim().isEmpty()) {
                    effectiveId = profile.get("kuetId").toString().trim();
                } else if (email != null && email.contains("@") && email.split("@")[0].matches("\\d+")) {
                    effectiveId = email.split("@")[0].trim();
                } else {
                    effectiveId = (email != null && !email.trim().isEmpty()) ? email.trim() : "2307003";
                }

                SessionManager.getInstance(StudentLoginActivity.this)
                        .saveFirebaseSession(uid, email, "STUDENT", name, dept, effectiveId);

                Toast.makeText(StudentLoginActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                launchMain("STUDENT", effectiveId);
            }

            @Override
            public void onError(String errorMessage) {
                // Handle missing Firestore profile gracefully
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In");

                String roll = (email != null && email.contains("@") && email.split("@")[0].matches("\\d+"))
                        ? email.split("@")[0].trim() : "2307003";

                // Self-heal Firestore profile with STUDENT role
                Map<String, Object> fallbackProfile = new HashMap<>();
                fallbackProfile.put("uid", uid);
                fallbackProfile.put("name", "KUET Student");
                fallbackProfile.put("email", email);
                fallbackProfile.put("role", "STUDENT");
                fallbackProfile.put("roll", roll);
                fallbackProfile.put("createdAt", System.currentTimeMillis());
                firebaseHelper.saveUserProfile(uid, fallbackProfile, new FirebaseHelper.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {}
                    @Override
                    public void onError(String err) {}
                });

                SessionManager.getInstance(StudentLoginActivity.this)
                        .saveFirebaseSession(uid, email, "STUDENT", "KUET Student", "CSE", roll);

                Toast.makeText(StudentLoginActivity.this, "Welcome back! (Profile initialized)", Toast.LENGTH_SHORT).show();
                launchMain("STUDENT", roll);
            }
        });
    }

    private void onAuthFailure(String errorMessage) {
        btnLogin.setEnabled(true);
        btnLogin.setText("Log In");

        new AlertDialog.Builder(StudentLoginActivity.this)
                .setTitle("Authentication Failed")
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPasswordResetDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter your registered email address");
        String current = etEmail.getText().toString().trim();
        if (current.contains("@")) {
            input.setText(current);
        }
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email to receive a Firebase password reset link:")
                .setView(input)
                .setPositiveButton("Send Reset Link", (d, w) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        firebaseHelper.sendPasswordReset(email, new FirebaseHelper.ActionCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(StudentLoginActivity.this, message, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(StudentLoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchMain(String role, String id) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("USER_ID", id);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
