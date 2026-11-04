package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;

public class AlumniRegisterActivity extends AppCompatActivity {
    private EditText etName, etBatch, etCompany, etPosition, etCountry, etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_register);

        etName = findViewById(R.id.etRegAlumniName);
        etBatch = findViewById(R.id.etRegAlumniBatch);
        etCompany = findViewById(R.id.etRegAlumniCompany);
        etPosition = findViewById(R.id.etRegAlumniPosition);
        etCountry = findViewById(R.id.etRegAlumniCountry);
        etEmail = findViewById(R.id.etRegAlumniEmail);

        findViewById(R.id.btnCreateAlumniAccount).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String company = etCompany.getText().toString().trim();
            if (name.isEmpty() || company.isEmpty()) {
                Toast.makeText(this, "Please enter your Name and Current Company", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Alumni Account Registered for " + name + " ✓", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USER_ROLE", "ALUMNI");
            intent.putExtra("USER_ID", name);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.tvBackToAlumniLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, AlumniLoginActivity.class));
            finish();
        });
    }
}
