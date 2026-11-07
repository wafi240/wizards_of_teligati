package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.teligati.app.R;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.StudentProfileEntity;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.data.repository.ProfileRepository;
import com.teligati.app.network.model.ProfileResponse;
import com.teligati.app.network.model.ProfileUpdateRequest;

public class StudentPortfolioFragment extends Fragment {
    private AppRepository repository;
    private ProfileRepository profileRepository;
    private StudentProfileEntity currentProfile;

    private TextView tvName;
    private TextView tvRoll;
    private TextView tvSpecialization;
    private TextView tvSkills;
    private TextView tvProjects;
    private TextView tvLinks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_portfolio, container, false);
        repository = AppRepository.getInstance(requireContext());
        profileRepository = ProfileRepository.getInstance(requireContext());

        tvName = view.findViewById(R.id.tvPortfolioName);
        tvRoll = view.findViewById(R.id.tvPortfolioRoll);
        tvSpecialization = view.findViewById(R.id.tvPortfolioSpecialization);
        tvSkills = view.findViewById(R.id.tvPortfolioSkills);
        tvProjects = view.findViewById(R.id.tvPortfolioProjects);
        tvLinks = view.findViewById(R.id.tvPortfolioLinks);
        Button btnEdit = view.findViewById(R.id.btnEditPortfolio);

        loadProfile();

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> showEditPortfolioDialog());
        }

        return view;
    }

    private void loadProfile() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        String roll = session.getUserId();

        // 1. Initial local Room load
        repository.getStudentProfile(roll, profile -> {
            if (!isAdded()) return;
            if (profile != null) {
                currentProfile = profile;
                renderProfile();
            }
        });

        // 2. Authoritative remote sync via ProfileRepository (Firestore source of truth)
        if (session.isLoggedIn()) {
            profileRepository.fetchMyProfile(new ProfileRepository.Callback<ProfileResponse>() {
                @Override
                public void onSuccess(ProfileResponse p) {
                    if (!isAdded() || p == null) return;
                    String studentRoll = p.getKuetId() != null ? p.getKuetId() : roll;
                    double cgpa = p.getCgpa() != null ? p.getCgpa() : (currentProfile != null ? currentProfile.getCgpa() : 3.85);

                    currentProfile = new StudentProfileEntity(
                            studentRoll,
                            p.getName(),
                            p.getDepartment(),
                            p.getBatch() != null ? p.getBatch() : "2023",
                            cgpa,
                            p.getSkills(),
                            p.getProjects(),
                            p.getGithubUrl(),
                            p.getLinkedinUrl(),
                            p.getSpecialization(),
                            p.getClubs(),
                            p.getBio()
                    );
                    renderProfile();
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }
    }

    private void renderProfile() {
        if (currentProfile == null) return;
        if (tvName != null) tvName.setText(currentProfile.getName());
        if (tvRoll != null) {
            tvRoll.setText("KUET Roll " + currentProfile.getRoll() + " • " + currentProfile.getDepartment() + " " + currentProfile.getBatch() + " • CGPA " + currentProfile.getCgpa());
        }
        if (tvSpecialization != null && currentProfile.getSpecialization() != null) {
            tvSpecialization.setText("Specialization: " + currentProfile.getSpecialization());
        }
        if (tvSkills != null && currentProfile.getSkills() != null) {
            tvSkills.setText("Skills: " + currentProfile.getSkills());
        }
        if (tvProjects != null && currentProfile.getProjects() != null) {
            tvProjects.setText("Projects: " + currentProfile.getProjects());
        }
        if (tvLinks != null) {
            tvLinks.setText("Links: " + (currentProfile.getGithubUrl() != null ? currentProfile.getGithubUrl() : "github.com/kuet-wizards")
                    + " • " + (currentProfile.getLinkedinUrl() != null ? currentProfile.getLinkedinUrl() : "linkedin.com/in/kuet-student"));
        }
    }

    private void showEditPortfolioDialog() {
        if (getContext() == null || currentProfile == null) return;

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        final EditText etName = createInput("Full Name", currentProfile.getName());
        final EditText etCgpa = createInput("CGPA (0.00 - 4.00)", String.valueOf(currentProfile.getCgpa()));
        final EditText etSpec = createInput("Specialization / Research Focus", currentProfile.getSpecialization());
        final EditText etSkills = createInput("Technical Skills", currentProfile.getSkills());
        final EditText etProjects = createInput("Key Projects & Achievements", currentProfile.getProjects());
        final EditText etGithub = createInput("GitHub URL", currentProfile.getGithubUrl());
        final EditText etLinkedin = createInput("LinkedIn URL", currentProfile.getLinkedinUrl());

        layout.addView(etName);
        layout.addView(etCgpa);
        layout.addView(etSpec);
        layout.addView(etSkills);
        layout.addView(etProjects);
        layout.addView(etGithub);
        layout.addView(etLinkedin);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Verified Portfolio")
                .setView(scrollView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String cgpaStr = etCgpa.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double cgpa = currentProfile.getCgpa();
                    try {
                        cgpa = Double.parseDouble(cgpaStr);
                        if (cgpa < 0.0 || cgpa > 4.0) {
                            Toast.makeText(requireContext(), "CGPA must be between 0.00 and 4.00", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Invalid CGPA format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ProfileUpdateRequest updateReq = new ProfileUpdateRequest();
                    updateReq.setName(name);
                    updateReq.setCgpa(cgpa);
                    updateReq.setSpecialization(etSpec.getText().toString().trim());
                    updateReq.setSkills(etSkills.getText().toString().trim());
                    updateReq.setProjects(etProjects.getText().toString().trim());
                    updateReq.setGithubUrl(etGithub.getText().toString().trim());
                    updateReq.setLinkedinUrl(etLinkedin.getText().toString().trim());

                    final double finalCgpa = cgpa;
                    profileRepository.updateMyProfile(updateReq, new ProfileRepository.Callback<ProfileResponse>() {
                        @Override
                        public void onSuccess(ProfileResponse result) {
                            if (!isAdded()) return;
                            currentProfile.setName(name);
                            currentProfile.setCgpa(finalCgpa);
                            currentProfile.setSpecialization(updateReq.getSpecialization());
                            currentProfile.setSkills(updateReq.getSkills());
                            currentProfile.setProjects(updateReq.getProjects());
                            currentProfile.setGithubUrl(updateReq.getGithubUrl());
                            currentProfile.setLinkedinUrl(updateReq.getLinkedinUrl());

                            SessionManager.getInstance(requireContext()).updateUserDetails(name, currentProfile.getDepartment(), (float) finalCgpa);
                            renderProfile();
                            Toast.makeText(requireContext(), "Portfolio updated and synchronized with backend!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (!isAdded()) return;
                            // Resilient fallback: persist to Room locally
                            currentProfile.setName(name);
                            currentProfile.setCgpa(finalCgpa);
                            currentProfile.setSpecialization(updateReq.getSpecialization());
                            currentProfile.setSkills(updateReq.getSkills());
                            currentProfile.setProjects(updateReq.getProjects());
                            currentProfile.setGithubUrl(updateReq.getGithubUrl());
                            currentProfile.setLinkedinUrl(updateReq.getLinkedinUrl());

                            repository.updateStudentProfile(currentProfile, success -> {
                                if (!isAdded()) return;
                                SessionManager.getInstance(requireContext()).updateUserDetails(name, currentProfile.getDepartment(), (float) finalCgpa);
                                renderProfile();
                                Toast.makeText(requireContext(), "Saved locally (Network: " + errorMessage + ")", Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private EditText createInput(String hint, String value) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        if (value != null) et.setText(value);
        et.setPadding(24, 20, 24, 20);
        return et;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }
}
