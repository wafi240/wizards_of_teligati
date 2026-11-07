package com.teligati.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.teligati.app.MainActivity;
import com.teligati.app.databinding.ActivityAuthGatewayBinding;
import com.teligati.app.firebase.FirebaseDemoActivity;
import com.teligati.app.network.ApiClient;

/**
 * AuthGatewayActivity serves as the launcher entry point for the application.
 * Demonstrates:
 * 1. View Binding (ActivityAuthGatewayBinding)
 * 2. Explicit Intent navigation with Intent extras
 * 3. Event handling for role-based selection
 */
public class AuthGatewayActivity extends AppCompatActivity {
    private ActivityAuthGatewayBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthGatewayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        updateServerDisplay();

        if (binding.layoutServerConfig != null) {
            binding.layoutServerConfig.setOnClickListener(v -> showServerConfigDialog());
        }

        // Student actions
        binding.btnStudentLogin.setOnClickListener(v -> navigateTo(StudentLoginActivity.class));
        binding.btnStudentRegister.setOnClickListener(v -> navigateTo(StudentRegisterActivity.class));

        // Alumni actions
        binding.btnAlumniLogin.setOnClickListener(v -> navigateTo(AlumniLoginActivity.class));
        binding.btnAlumniRegister.setOnClickListener(v -> navigateTo(AlumniRegisterActivity.class));

        // Faculty actions
        binding.btnFacultyLogin.setOnClickListener(v -> navigateTo(FacultyLoginActivity.class));
        binding.btnFacultyRegister.setOnClickListener(v -> navigateTo(FacultyRegisterActivity.class));

        // Quick Guest Preview (Explicit Intent with Extras)
        binding.btnGuestPreview.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("USER_ROLE", "STUDENT");
            intent.putExtra("USER_ID", "2307003");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Firebase Course Demo (Explicit Intent)
        if (binding.btnOpenFirebaseDemo != null) {
            binding.btnOpenFirebaseDemo.setOnClickListener(v -> {
                startActivity(new Intent(this, FirebaseDemoActivity.class));
            });
        }
    }

    private void updateServerDisplay() {
        if (binding.tvServerConfig != null) {
            String currentUrl = ApiClient.resolveBaseUrl(this);
            String deviceType = ApiClient.isEmulator() ? "Emulator" : "Phone";
            binding.tvServerConfig.setText("🌐 " + deviceType + " Server: " + currentUrl + " (Tap to change)");
        }
    }

    private void showServerConfigDialog() {
        EditText input = new EditText(this);
        input.setText(ApiClient.resolveBaseUrl(this));
        input.setSelection(input.getText().length());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Configure Backend Server URL")
                .setMessage("Enter the backend URL (e.g. http://192.168.0.64:8080/):\nDevice detected: " + (ApiClient.isEmulator() ? "Android Emulator" : "Physical Device"))
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newUrl = input.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        ApiClient.updateBaseUrl(this, newUrl);
                        updateServerDisplay();
                        Toast.makeText(this, "Updated backend URL to: " + ApiClient.resolveBaseUrl(this), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Reset Default", (d, w) -> {
                    ApiClient.updateBaseUrl(this, null);
                    updateServerDisplay();
                    Toast.makeText(this, "Reset to default: " + ApiClient.resolveBaseUrl(this), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateTo(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
        finish();
    }
}
