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

public class AlumniRegisterActivity extends AppCompatActivity {
    private EditText etName, etBatch, etCompany, etPosition, etCountry, etEmail, etPassword, etToken;
    private Button btnCreate;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_register);

        firebaseHelper = FirebaseHelper.getInstance(this);

        etName     = findViewById(R.id.etRegAlumniName);
        etBatch    = findViewById(R.id.etRegAlumniBatch);
        etCompany  = findViewById(R.id.etRegAlumniCompany);
        etPosition = findViewById(R.id.etRegAlumniPosition);
        etCountry  = findViewById(R.id.etRegAlumniCountry);
        etEmail    = findViewById(R.id.etRegAlumniEmail);
        etPassword = findViewById(R.id.etRegAlumniPassword);
        etToken    = findViewById(R.id.etRegAlumniToken);
        btnCreate  = findViewById(R.id.btnCreateAlumniAccount);

        btnCreate.setOnClickListener(v -> performRegister());

        findViewById(R.id.tvBackToAlumniLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, AlumniLoginActivity.class));
            finish();
        });
    }

    private void performRegister() {
        String name     = etName.getText().toString().trim();
        String deptBatch= etBatch != null ? etBatch.getText().toString().trim() : "CSE";
        String company  = etCompany != null ? etCompany.getText().toString().trim() : "";
        String position = etPosition != null ? etPosition.getText().toString().trim() : "";
        String country  = etCountry != null ? etCountry.getText().toString().trim() : "";
        String email    = etEmail.getText().toString().trim();
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";
        String token    = etToken != null ? etToken.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Full Name is required");
            etName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("KUET email is required");
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
        if (token.isEmpty()) {
            if (etToken != null) {
                etToken.setError("Alumni invitation token is required");
                etToken.requestFocus();
            }
            return;
        }

        // Extract department (e.g. from "CSE 2K12" -> "CSE")
        String department = "CSE";
        if (!deptBatch.isEmpty()) {
            String[] parts = deptBatch.split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                department = parts[0];
            }
        }

        final String finalDept = department;

        btnCreate.setEnabled(false);
        btnCreate.setText("Registering with Firebase...");

        firebaseHelper.registerUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String message) {
                String uid = user != null ? user.getUid() : "";
                Map<String, Object> profile = new HashMap<>();
                profile.put("uid", uid);
                profile.put("name", name);
                profile.put("batch", deptBatch);
                profile.put("department", finalDept);
                profile.put("company", company);
                profile.put("position", position);
                profile.put("country", country);
                profile.put("email", email);
                profile.put("token", token);
                profile.put("role", "ALUMNI");
                profile.put("createdAt", System.currentTimeMillis());

                firebaseHelper.saveUserProfile(uid, profile, new FirebaseHelper.ActionCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        btnCreate.setEnabled(true);
                        btnCreate.setText("Create Account");

                        SessionManager.getInstance(AlumniRegisterActivity.this)
                                .saveFirebaseSession(uid, email, "ALUMNI", name, finalDept, email);

                        Toast.makeText(AlumniRegisterActivity.this, "Alumni Account Created Successfully!", Toast.LENGTH_SHORT).show();
                        launchMain("ALUMNI", email);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnCreate.setEnabled(true);
                        btnCreate.setText("Create Account");

                        SessionManager.getInstance(AlumniRegisterActivity.this)
                                .saveFirebaseSession(uid, email, "ALUMNI", name, finalDept, email);

                        Toast.makeText(AlumniRegisterActivity.this, "Registered with Firebase! (Profile warning: " + errorMessage + ")", Toast.LENGTH_SHORT).show();
                        launchMain("ALUMNI", email);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                btnCreate.setEnabled(true);
                btnCreate.setText("Create Account");

                new AlertDialog.Builder(AlumniRegisterActivity.this)
                        .setTitle("Registration Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
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
