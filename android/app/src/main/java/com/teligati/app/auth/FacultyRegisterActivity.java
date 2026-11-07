package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class FacultyRegisterActivity extends AppCompatActivity {
    private EditText etName, etDept, etDesignation, etLab, etResearchArea, etEmail, etPassword;
    private Button btnCreate;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_register);

        firebaseHelper = FirebaseHelper.getInstance(this);

        etName         = findViewById(R.id.etRegFacultyName);
        etDept         = findViewById(R.id.etRegFacultyDept);
        etDesignation  = findViewById(R.id.etRegFacultyDesignation);
        etLab          = findViewById(R.id.etRegFacultyLab);
        etResearchArea = findViewById(R.id.etRegFacultyResearch);
        etEmail        = findViewById(R.id.etRegFacultyEmail);
        etPassword     = findViewById(R.id.etRegFacultyPassword);
        btnCreate      = findViewById(R.id.btnCreateFacultyAccount);

        btnCreate.setOnClickListener(v -> performRegister());

        findViewById(R.id.tvBackToFacultyLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, FacultyLoginActivity.class));
            finish();
        });
    }

    private void performRegister() {
        String name        = etName.getText().toString().trim();
        String dept        = etDept.getText().toString().trim();
        String designation = etDesignation != null ? etDesignation.getText().toString().trim() : "Professor";
        String lab          = etLab != null ? etLab.getText().toString().trim() : "";
        String researchArea = etResearchArea != null ? etResearchArea.getText().toString().trim() : "";
        String email       = etEmail.getText().toString().trim();
        String password    = etPassword != null ? etPassword.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Full Name is required");
            etName.requestFocus();
            return;
        }
        if (dept.isEmpty()) {
            etDept.setError("Department is required (e.g. CSE)");
            etDept.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("KUET faculty email is required");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            if (etPassword != null) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
            }
            return;
        }

        if (designation.isEmpty()) {
            designation = "Professor";
        }

        btnCreate.setEnabled(false);
        btnCreate.setText("Authenticating & Verifying...");

        final String finalDesignation = designation;

        firebaseHelper.registerUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String message) {
                // Verify the user's own authorization record in the authorized_faculty collection
                firebaseHelper.checkAuthorizedFaculty(email, new FirebaseHelper.ActionCallback() {
                    @Override
                    public void onSuccess(String authMsg) {
                        String uid = user != null ? user.getUid() : "";
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("uid", uid);
                        profile.put("name", name);
                        profile.put("department", dept);
                        profile.put("designation", finalDesignation);
                        profile.put("lab", lab);
                        profile.put("researchArea", researchArea);
                        profile.put("email", email);
                        profile.put("role", "TEACHER");
                        profile.put("createdAt", System.currentTimeMillis());

                        firebaseHelper.saveUserProfile(uid, profile, new FirebaseHelper.ActionCallback() {
                            @Override
                            public void onSuccess(String msg) {
                                btnCreate.setEnabled(true);
                                btnCreate.setText("Create Account");

                                SessionManager.getInstance(FacultyRegisterActivity.this)
                                        .saveFirebaseSession(uid, email, "TEACHER", name, dept, email);

                                Toast.makeText(FacultyRegisterActivity.this, "Faculty Account Created Successfully!", Toast.LENGTH_SHORT).show();
                                launchMain("TEACHER", email);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                handleUnauthorizedUser(user, errorMessage);
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        handleUnauthorizedUser(user, errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                btnCreate.setEnabled(true);
                btnCreate.setText("Create Account");

                new AlertDialog.Builder(FacultyRegisterActivity.this)
                        .setTitle("Registration Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void handleUnauthorizedUser(FirebaseUser user, String errorMessage) {
        btnCreate.setEnabled(true);
        btnCreate.setText("Create Account");

        if (user != null) {
            user.delete();
        }
        firebaseHelper.signOut(FacultyRegisterActivity.this);

        new AlertDialog.Builder(FacultyRegisterActivity.this)
                .setTitle("Faculty Authorization Required")
                .setMessage("Registration Denied: " + errorMessage + "\n\nOrdinary users cannot self-register as TEACHER. Authorized faculty members must be registered in the Firebase Console by university administration.")
                .setPositiveButton("OK", null)
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
