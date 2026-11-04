package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;

public class StudentRegisterActivity extends AppCompatActivity {
    private EditText etName, etRoll, etDept, etBatch, etCgpa, etEmail, etPassword;

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

        findViewById(R.id.btnCreateStudentAccount).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String roll = etRoll.getText().toString().trim();
            if (name.isEmpty() || roll.isEmpty()) {
                Toast.makeText(this, "Name and Roll number are required", Toast.LENGTH_SHORT).show();
                return;
            }
            launchMain("STUDENT", roll);
        });

        findViewById(R.id.tvBackToStudentLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
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
