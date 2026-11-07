package com.teligati.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.tabs.TabLayout;
import com.teligati.app.data.SessionManager;
import com.teligati.app.firebase.FirebaseHelper;
import com.teligati.app.model.UserRole;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private UserRole selectedRole = UserRole.STUDENT;
    private EditText etIdentifier;
    private EditText etPassword;
    private TextView tvRolePrompt;
    private TextView tvExtraLabel;
    private EditText etExtraField;
    private Button btnLogin;
    private Button btnDemoLogin;
    private TextView tvForgot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TabLayout tabLayout = findViewById(R.id.tabRoleSelector);
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        tvRolePrompt = findViewById(R.id.tvRolePrompt);
        tvExtraLabel = findViewById(R.id.tvExtraLabel);
        etExtraField = findViewById(R.id.etExtraField);
        btnLogin = findViewById(R.id.btnLogin);
        btnDemoLogin = findViewById(R.id.btnDemoLogin);
        tvForgot = findViewById(R.id.tvForgot);

        updateRoleUI(UserRole.STUDENT);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        updateRoleUI(UserRole.STUDENT);
                        break;
                    case 1:
                        updateRoleUI(UserRole.ALUMNI);
                        break;
                    case 2:
                        updateRoleUI(UserRole.FACULTY);
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnLogin.setOnClickListener(v -> performLogin());

        btnDemoLogin.setOnClickListener(v -> {
            autoFillDemoCredentials();
            performLogin();
        });

        tvForgot.setOnClickListener(v -> {
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("KUET Verification & Recovery")
                    .setMessage("To reset credentials or request manual verification, enter your institutional roll or webmail. Teligati Admin will verify against KUET registrar records.")
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void updateRoleUI(UserRole role) {
        this.selectedRole = role;
        switch (role) {
            case STUDENT:
                tvRolePrompt.setText("🎓 Student Authentication");
                etIdentifier.setHint("Student Roll (e.g. 2307003)");
                tvExtraLabel.setText("Department & Batch");
                etExtraField.setText("CSE 2K23");
                btnDemoLogin.setText("1-Tap Demo: Log in as Samun (Student)");
                break;
            case ALUMNI:
                tvRolePrompt.setText("💼 Alumni Authentication");
                etIdentifier.setHint("Alumni Email or Batch (e.g. 2K12)");
                tvExtraLabel.setText("Current Company & Country");
                etExtraField.setText("BMW Group (Munich, Germany)");
                btnDemoLogin.setText("1-Tap Demo: Log in as Engr. Tanvir (Alumni)");
                break;
            case FACULTY:
                tvRolePrompt.setText("🏛️ Faculty & Teacher Authentication");
                etIdentifier.setHint("Institutional Email (e.g. azharul@cse.kuet.ac.bd)");
                tvExtraLabel.setText("Designation & Lab");
                etExtraField.setText("Professor • Big Data Lab");
                btnDemoLogin.setText("1-Tap Demo: Log in as Prof. Azharul (Faculty)");
                break;
        }
    }

    private void autoFillDemoCredentials() {
        etIdentifier.setText(selectedRole.getDefaultId());
        etPassword.setText("demo");
    }

    private void performLogin() {
        String id = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (id.isEmpty()) {
            Toast.makeText(this, "Please enter your ID or Email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Authenticating with Firebase...");

        String email = id.contains("@") ? id : (id + "@stud.kuet.ac.bd");
        FirebaseHelper helper = FirebaseHelper.getInstance(this);

        helper.loginUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String message) {
                String uid = user != null ? user.getUid() : "";
                helper.fetchUserProfile(uid, new FirebaseHelper.ProfileCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> profile) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Log In");

                        String role = selectedRole.name();
                        if (profile.containsKey("role") && profile.get("role") != null) {
                            role = SessionManager.normalizeRole(profile.get("role").toString());
                        }

                        SessionManager.getInstance(LoginActivity.this)
                                .saveFirebaseSession(uid, email, role, profile.get("name") != null ? profile.get("name").toString() : "KUET User", "CSE", id);

                        Toast.makeText(LoginActivity.this, "Verified as " + selectedRole.getRoleTitle() + " ✓", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("USER_ROLE", role);
                        intent.putExtra("USER_ID", id);
                        intent.putExtra("USER_EXTRA", etExtraField.getText().toString());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Log In");

                        String role = selectedRole.name();
                        SessionManager.getInstance(LoginActivity.this)
                                .saveFirebaseSession(uid, email, role, "KUET User", "CSE", id);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("USER_ROLE", role);
                        intent.putExtra("USER_ID", id);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In");

                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Authentication Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}
