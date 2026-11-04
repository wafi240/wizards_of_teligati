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

        etName = findViewById(R.id.etRegStudentName);
        etRoll = findViewById(R.id.etRegStudentRoll);
        etDept = findViewById(R.id.etRegStudentDept);
        etBatch = findViewById(R.id.etRegStudentBatch);
        etCgpa = findViewById(R.id.etRegStudentCgpa);
        etEmail = findViewById(R.id.etRegStudentEmail);
        etPassword = findViewById(R.id.etRegStudentPassword);

        findViewById(R.id.btnCreateStudentAccount).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String roll = etRoll.getText().toString().trim();
            if (name.isEmpty() || roll.isEmpty()) {
                Toast.makeText(this, "Please fill in your Name and Roll number", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Student Account Created for " + name + " ✓", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USER_ROLE", "STUDENT");
            intent.putExtra("USER_ID", roll);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.tvBackToStudentLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentLoginActivity.class));
            finish();
        });
    }
}
