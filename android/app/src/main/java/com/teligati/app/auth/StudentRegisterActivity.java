package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;
import com.teligati.app.data.SessionManager;
import com.teligati.app.firebase.FirebaseHelper;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class StudentRegisterActivity extends AppCompatActivity {
    private EditText etName, etRoll, etDept, etBatch, etCgpa, etEmail, etPassword;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        etName     = findViewById(R.id.etRegStudentName);
        etRoll     = findViewById(R.id.etRegStudentRoll);
        etDept     = findViewById(R.id.etRegStudentDept);
        etBatch    = findViewById(R.id.etRegStudentBatch);
        etCgpa     = findViewById(R.id.etRegStudentCgpa);
        etEmail    = findViewById(R.id.etRegStudentEmail);
        etPassword = findViewById(R.id.etRegStudentPassword);
        btnCreate  = findViewById(R.id.btnCreateStudentAccount);

        btnCreate.setOnClickListener(v -> performRegister());

        findViewById(R.id.tvBackToStudentLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
        });
    }

    private void performRegister() {
        String name     = etName.getText().toString().trim();
        String roll     = etRoll.getText().toString().trim();
        String dept     = etDept.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Full Name is required");
            etName.requestFocus();
            return;
        }
        if (roll.isEmpty()) {
            etRoll.setError("Roll number is required");
            etRoll.requestFocus();
            return;
        }
        if (dept.isEmpty()) {
            etDept.setError("Department is required (e.g. Computer Science and Engineering)");
            etDept.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("KUET student email required (e.g. roll@stud.kuet.ac.bd)");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        btnCreate.setEnabled(false);
        btnCreate.setText("Registering with Firebase...");

        FirebaseHelper firebaseHelper = FirebaseHelper.getInstance(this);
        firebaseHelper.registerUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user, String message) {
                String uid = user != null ? user.getUid() : "";
                Map<String, Object> profile = new HashMap<>();
                profile.put("uid", uid);
                profile.put("name", name);
                profile.put("roll", roll);
                profile.put("kuetId", roll);
                profile.put("department", dept);
                profile.put("batch", etBatch != null ? etBatch.getText().toString().trim() : "2K23");
                profile.put("cgpa", etCgpa != null ? etCgpa.getText().toString().trim() : "3.84");
                profile.put("email", email);
                profile.put("role", "STUDENT");
                profile.put("createdAt", System.currentTimeMillis());

                firebaseHelper.saveUserProfile(uid, profile, new FirebaseHelper.ActionCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        btnCreate.setEnabled(true);
                        btnCreate.setText("Create Student Account");

                        SessionManager.getInstance(StudentRegisterActivity.this)
                                .saveFirebaseSession(uid, email, "STUDENT", name, dept, roll);

                        new AlertDialog.Builder(StudentRegisterActivity.this)
                                .setTitle("Registration Successful")
                                .setMessage("Your student account has been created via Firebase Authentication & Firestore!\n\nUID: " + uid)
                                .setPositiveButton("Enter Platform", (dialog, which) -> {
                                    launchMain("STUDENT", roll);
                                })
                                .setCancelable(false)
                                .show();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnCreate.setEnabled(true);
                        btnCreate.setText("Create Student Account");

                        SessionManager.getInstance(StudentRegisterActivity.this)
                                .saveFirebaseSession(uid, email, "STUDENT", name, dept, roll);

                        new AlertDialog.Builder(StudentRegisterActivity.this)
                                .setTitle("Account Created")
                                .setMessage("Firebase Authentication succeeded, but profile warning: " + errorMessage)
                                .setPositiveButton("Continue", (dialog, which) -> launchMain("STUDENT", roll))
                                .show();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                btnCreate.setEnabled(true);
                btnCreate.setText("Create Student Account");

                new AlertDialog.Builder(StudentRegisterActivity.this)
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
