package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;

public class FacultyLoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_login);

        etEmail    = findViewById(R.id.etFacultyEmail);
        etPassword = findViewById(R.id.etFacultyPassword);

        findViewById(R.id.btnLoginFaculty).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your institutional email", Toast.LENGTH_SHORT).show();
                return;
            }
            launchMain("FACULTY", email);
        });

        findViewById(R.id.btnFacultyDemo).setOnClickListener(v -> {
            etEmail.setText("azharul@cse.kuet.ac.bd");
            etPassword.setText("demo");
            findViewById(R.id.btnLoginFaculty).performClick();
        });

        findViewById(R.id.tvGoToFacultyRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, FacultyRegisterActivity.class));
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
