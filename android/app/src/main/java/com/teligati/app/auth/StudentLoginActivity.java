package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;

public class StudentLoginActivity extends AppCompatActivity {
    private EditText etRoll, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        etRoll = findViewById(R.id.etStudentRoll);
        etPassword = findViewById(R.id.etStudentPassword);

        findViewById(R.id.btnLoginStudent).setOnClickListener(v -> {
            String roll = etRoll.getText().toString().trim();
            String pwd = etPassword.getText().toString().trim();
            if (roll.isEmpty()) {
                Toast.makeText(this, "Please enter your KUET Roll number", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Welcome Student Roll " + roll + " ✓", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USER_ROLE", "STUDENT");
            intent.putExtra("USER_ID", roll);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnStudentDemo).setOnClickListener(v -> {
            etRoll.setText("2307003");
            etPassword.setText("student2026");
            findViewById(R.id.btnLoginStudent).performClick();
        });

        findViewById(R.id.tvGoToStudentRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentRegisterActivity.class));
            finish();
        });
    }
}
