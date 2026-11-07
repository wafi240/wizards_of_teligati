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

public class AlumniLoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_login);

        firebaseHelper = FirebaseHelper.getInstance(this);

        etEmail    = findViewById(R.id.etAlumniEmail);
        etPassword = findViewById(R.id.etAlumniPassword);
        btnLogin   = findViewById(R.id.btnLoginAlumni);

        btnLogin.setOnClickListener(v -> performLogin(false));

        findViewById(R.id.btnAlumniDemo).setOnClickListener(v -> {
            etEmail.setText("tanvir.munich@kuet.ac.bd");
            etPassword.setText("demo123");
            performLogin(true);
        });

        TextView tvForgot = findViewById(R.id.tvAlumniForgotPassword);
        if (tvForgot != null) {
            tvForgot.setOnClickListener(v -> showPasswordResetDialog());
        }

        findViewById(R.id.tvGoToAlumniRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, AlumniRegisterActivity.class));
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(AlumniLoginActivity.this, AuthGatewayActivity.class));
                finish();
            }
        });
    }

    private void performLogin(boolean isDemo) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your KUET alumni email", Toast.LENGTH_SHORT).show();
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
                    autoProvisionDemoAlumni(email, password);
                } else {
                    onAuthFailure(errorMessage);
                }
            }
        });
    }

    private void autoProvisionDemoAlumni(String email, String password) {
        firebaseHelper.registerUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String message) {
                String uid = user != null ? user.getUid() : "";
                Map<String, Object> profile = new HashMap<>();
                profile.put("uid", uid);
                profile.put("name", "Engr. Tanvir Ahmed");
                profile.put("department", "CSE '15");
                profile.put("batch", "2K15");
                profile.put("company", "BMW Group (Munich, Germany)");
                profile.put("position", "Senior Robotics Engineer");
                profile.put("country", "Germany");
                profile.put("email", email);
                profile.put("role", "ALUMNI");
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
                onAuthFailure("Demo alumni provisioning failed: " + err);
            }
        });
    }

    private void fetchProfileAndNavigate(String uid, String email) {
        firebaseHelper.fetchUserProfile(uid, new FirebaseHelper.ProfileCallback() {
            @Override
            public void onSuccess(Map<String, Object> profile) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In as Alumni");

                String role = "ALUMNI";
                if (profile.containsKey("role") && profile.get("role") != null) {
                    role = SessionManager.normalizeRole(profile.get("role").toString());
                }

                if (!"ALUMNI".equalsIgnoreCase(role)) {
                    firebaseHelper.signOut(AlumniLoginActivity.this);
                    new AlertDialog.Builder(AlumniLoginActivity.this)
                            .setTitle("Role Mismatch")
                            .setMessage("This account is registered in Firestore as " + role + ". Please use the " + role + " Login screen.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                String name = profile.containsKey("name") && profile.get("name") != null ? profile.get("name").toString() : "Alumni";
                String dept = profile.containsKey("department") && profile.get("department") != null ? profile.get("department").toString() : "CSE '15";

                SessionManager.getInstance(AlumniLoginActivity.this)
                        .saveFirebaseSession(uid, email, "ALUMNI", name, dept, email);

                Toast.makeText(AlumniLoginActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                launchMain("ALUMNI", email);
            }

            @Override
            public void onError(String errorMessage) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In as Alumni");

                // Self-heal Firestore profile with ALUMNI role
                Map<String, Object> fallbackProfile = new HashMap<>();
                fallbackProfile.put("uid", uid);
                fallbackProfile.put("name", "KUET Alumni");
                fallbackProfile.put("email", email);
                fallbackProfile.put("role", "ALUMNI");
                fallbackProfile.put("createdAt", System.currentTimeMillis());
                firebaseHelper.saveUserProfile(uid, fallbackProfile, new FirebaseHelper.ActionCallback() {
                    @Override
                    public void onSuccess(String message) {}
                    @Override
                    public void onError(String err) {}
                });

                SessionManager.getInstance(AlumniLoginActivity.this)
                        .saveFirebaseSession(uid, email, "ALUMNI", "KUET Alumni", "CSE", email);

                Toast.makeText(AlumniLoginActivity.this, "Welcome back! (Profile initialized)", Toast.LENGTH_SHORT).show();
                launchMain("ALUMNI", email);
            }
        });
    }

    private void onAuthFailure(String errorMessage) {
        btnLogin.setEnabled(true);
        btnLogin.setText("Log In as Alumni");

        new AlertDialog.Builder(AlumniLoginActivity.this)
                .setTitle("Authentication Failed")
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPasswordResetDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter your registered alumni email");
        String current = etEmail.getText().toString().trim();
        if (!current.isEmpty()) {
            input.setText(current);
        }
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your registered email to receive a Firebase password reset link:")
                .setView(input)
                .setPositiveButton("Send Reset Link", (d, w) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        firebaseHelper.sendPasswordReset(email, new FirebaseHelper.ActionCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(AlumniLoginActivity.this, message, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(AlumniLoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
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
