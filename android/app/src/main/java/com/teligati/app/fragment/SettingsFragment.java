package com.teligati.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.teligati.app.R;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.data.repository.AuthRepository;
import com.teligati.app.network.ApiClient;

public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button btnFirebase = view.findViewById(R.id.btnFirebaseDemoSettings);
        if (btnFirebase != null) {
            btnFirebase.setOnClickListener(v -> {
                if (getContext() != null) {
                    startActivity(new Intent(getContext(), com.teligati.app.firebase.FirebaseDemoActivity.class));
                }
            });
        }

        Button btnBroadcast = view.findViewById(R.id.btnBroadcastDemoSettings);
        if (btnBroadcast != null) {
            btnBroadcast.setOnClickListener(v -> showBroadcastDemoDialog());
        }

        Button btnStudyService = view.findViewById(R.id.btnStudyServiceSettings);
        if (btnStudyService != null) {
            btnStudyService.setOnClickListener(v -> showStudyServiceDialog(btnStudyService));
        }

        Button btnVisitWeb = view.findViewById(R.id.btnVisitKuetWebsite);
        if (btnVisitWeb != null) {
            btnVisitWeb.setOnClickListener(v -> {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.kuet.ac.bd"));
                startActivity(webIntent);
            });
        }

        Button btnServer = view.findViewById(R.id.btnServerConfigSettings);
        if (btnServer != null) {
            btnServer.setText("🌐 Server: " + ApiClient.resolveBaseUrl(requireContext()));
            btnServer.setOnClickListener(v -> showServerConfigDialog(btnServer));
        }

        Button btnLogout = view.findViewById(R.id.btnLogoutSettings);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getActivity() == null) return;
                AuthRepository.getInstance(requireContext())
                        .logout(new AuthRepository.Callback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                if (getActivity() == null) return;
                                Intent intent = new Intent(getActivity(), AuthGatewayActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                getActivity().finish();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                if (getActivity() == null) return;
                                Toast.makeText(getContext(), "Logged out locally: " + errorMessage, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getActivity(), AuthGatewayActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                getActivity().finish();
                            }
                        });
            });
        }

        return view;
    }

    private void showServerConfigDialog(Button btnServer) {
        if (getContext() == null) return;
        EditText input = new EditText(getContext());
        input.setText(ApiClient.resolveBaseUrl(getContext()));
        input.setSelection(input.getText().length());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle("Backend Server URL")
                .setMessage("Enter the backend URL (must end with /):\nDevice detected: " + (ApiClient.isEmulator() ? "Android Emulator" : "Physical Device"))
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newUrl = input.getText().toString().trim();
                    if (!newUrl.isEmpty() && getContext() != null) {
                        ApiClient.updateBaseUrl(requireContext(), newUrl);
                        btnServer.setText("🌐 Server: " + ApiClient.resolveBaseUrl(requireContext()));
                        Toast.makeText(getContext(), "Backend URL updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Reset Default", (d, w) -> {
                    if (getContext() != null) {
                        ApiClient.updateBaseUrl(requireContext(), null);
                        btnServer.setText("🌐 Server: " + ApiClient.resolveBaseUrl(requireContext()));
                        Toast.makeText(getContext(), "Reset to default!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBroadcastDemoDialog() {
        if (getContext() == null) return;
        String[] options = {
                "✈️ Simulate Airplane Mode ON",
                "📶 Simulate Airplane Mode OFF",
                "🔋 Simulate Battery Low Warning",
                "⚡ Simulate Battery Recovered"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Broadcast Receiver Demo")
                .setItems(options, (dialog, which) -> {
                    if (getContext() == null) return;
                    switch (which) {
                        case 0:
                            com.teligati.app.receiver.SystemEventReceiver.simulateAirplaneMode(requireContext(), true);
                            break;
                        case 1:
                            com.teligati.app.receiver.SystemEventReceiver.simulateAirplaneMode(requireContext(), false);
                            break;
                        case 2:
                            com.teligati.app.receiver.SystemEventReceiver.simulateBatteryLow(requireContext());
                            break;
                        case 3:
                            com.teligati.app.receiver.SystemEventReceiver.handleBatteryOkay(requireContext());
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showStudyServiceDialog(Button btnStudyService) {
        if (getContext() == null) return;
        boolean isRunning = com.teligati.app.service.StudySessionService.isServiceRunning();
        int seconds = com.teligati.app.service.StudySessionService.getElapsedSeconds();

        String statusMessage = isRunning
                ? "Foreground Service is RUNNING\nActive Time: " + com.teligati.app.service.StudySessionService.formatTime(seconds) + "\n\nA persistent notification is active in the notification tray with an embedded stop action."
                : "Foreground Service is STOPPED\n\nTap Start to launch the study session service with persistent notification.";

        new AlertDialog.Builder(requireContext())
                .setTitle("⏱️ Study Session Foreground Service")
                .setMessage(statusMessage)
                .setPositiveButton(isRunning ? "⏹️ Stop Service" : "▶️ Start Service", (dialog, which) -> {
                    if (isRunning) {
                        com.teligati.app.service.StudySessionService.stopSession(requireContext());
                    } else {
                        com.teligati.app.service.StudySessionService.startSession(requireContext());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
