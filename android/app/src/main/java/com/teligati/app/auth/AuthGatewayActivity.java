package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.teligati.app.MainActivity;
import com.teligati.app.R;

public class AuthGatewayActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_gateway);

        // Student actions
        findViewById(R.id.btnStudentLogin).setOnClickListener(v -> 
            startActivity(new Intent(this, StudentLoginActivity.class)));
        findViewById(R.id.btnStudentRegister).setOnClickListener(v -> 
            startActivity(new Intent(this, StudentRegisterActivity.class)));

        // Alumni actions
        findViewById(R.id.btnAlumniLogin).setOnClickListener(v -> 
            startActivity(new Intent(this, AlumniLoginActivity.class)));
        findViewById(R.id.btnAlumniRegister).setOnClickListener(v -> 
            startActivity(new Intent(this, AlumniRegisterActivity.class)));

        // Faculty actions
        findViewById(R.id.btnFacultyLogin).setOnClickListener(v -> 
            startActivity(new Intent(this, FacultyLoginActivity.class)));
        findViewById(R.id.btnFacultyRegister).setOnClickListener(v -> 
            startActivity(new Intent(this, FacultyRegisterActivity.class)));

        // Quick Guest Preview
        findViewById(R.id.btnGuestPreview).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USER_ROLE", "STUDENT");
            intent.putExtra("USER_ID", "2307003");
            startActivity(intent);
        });
    }
}
