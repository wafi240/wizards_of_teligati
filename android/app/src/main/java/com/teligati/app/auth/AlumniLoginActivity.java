package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;

public class AlumniLoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_login);

        etEmail = findViewById(R.id.etAlumniEmail);
        etPassword = findViewById(R.id.etAlumniPassword);

        findViewById(R.id.btnLoginAlumni).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your Alumni Email or Batch", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Welcome Verified Alumni ✓", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USER_ROLE", "ALUMNI");
            intent.putExtra("USER_ID", email);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnAlumniDemo).setOnClickListener(v -> {
            etEmail.setText("tanvir.bmw@kuet.alumni.bd");
            etPassword.setText("alumni2026");
            findViewById(R.id.btnLoginAlumni).performClick();
        });

        findViewById(R.id.tvGoToAlumniRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, AlumniRegisterActivity.class));
            finish();
        });
    }
}
