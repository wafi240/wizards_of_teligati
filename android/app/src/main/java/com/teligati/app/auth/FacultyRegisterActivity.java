package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;

public class FacultyRegisterActivity extends AppCompatActivity {
    private EditText etName, etDept, etDesignation, etLab, etResearchArea, etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_register);

        etName        = findViewById(R.id.etRegFacultyName);
        etDept        = findViewById(R.id.etRegFacultyDept);
        etDesignation = findViewById(R.id.etRegFacultyDesignation);
        etLab         = findViewById(R.id.etRegFacultyLab);
        etResearchArea= findViewById(R.id.etRegFacultyResearch);
        etEmail       = findViewById(R.id.etRegFacultyEmail);

        findViewById(R.id.btnCreateFacultyAccount).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String dept = etDept.getText().toString().trim();
            if (name.isEmpty() || dept.isEmpty()) {
                Toast.makeText(this, "Name and Department are required", Toast.LENGTH_SHORT).show();
                return;
            }
            launchMain("FACULTY", name);
        });

        findViewById(R.id.tvBackToFacultyLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, FacultyLoginActivity.class));
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
