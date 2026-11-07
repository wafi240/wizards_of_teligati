package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.AlumniJobApplicantAdapter;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.JobApplicationEntity;
import com.teligati.app.data.repository.AppRepository;
import java.util.ArrayList;
import java.util.List;

public class AlumniStudentApplicantsFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvApplicants;
    private EditText etSearch;
    private TextView tvEmpty;
    private AlumniJobApplicantAdapter adapter;
    private List<JobApplicationEntity> allApplicants = new ArrayList<>();
    private List<JobApplicationEntity> displayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alumni_student_applicants, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvApplicants = view.findViewById(R.id.rvStudentApplicants);
        etSearch = view.findViewById(R.id.etSearchApplicants);
        tvEmpty = view.findViewById(R.id.tvEmptyApplicants);

        rvApplicants.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlumniJobApplicantAdapter(displayedList, new AlumniJobApplicantAdapter.OnApplicantActionListener() {
            @Override
            public void onAccept(JobApplicationEntity app, int position) {
                updateStatus(app, "Accepted", position);
            }

            @Override
            public void onShortlist(JobApplicationEntity app, int position) {
                updateStatus(app, "Shortlisted", position);
            }

            @Override
            public void onReject(JobApplicationEntity app, int position) {
                updateStatus(app, "Rejected", position);
            }

            @Override
            public void onMessage(JobApplicationEntity app, int position) {
                showSendMessageDialog(app);
            }

            @Override
            public void onViewPortfolio(JobApplicationEntity app, int position) {
                showStudentPortfolioDialog(app);
            }
        });
        rvApplicants.setAdapter(adapter);

        setupSearch();
        loadApplicants();

        return view;
    }

    private void loadApplicants() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        repository.getAlumniJobApplications(session.getUserId(), session.getUserName(), apps -> {
            if (!isAdded()) return;
            allApplicants = apps != null ? apps : new ArrayList<>();
            filter(etSearch != null ? etSearch.getText().toString() : "");
        });
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        displayedList.clear();
        String q = query.toLowerCase().trim();
        for (JobApplicationEntity a : allApplicants) {
            if (q.isEmpty()
                    || a.getApplicantName().toLowerCase().contains(q)
                    || a.getApplicantRoll().toLowerCase().contains(q)
                    || a.getJobTitle().toLowerCase().contains(q)
                    || a.getApplicantSkills().toLowerCase().contains(q)) {
                displayedList.add(a);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStatus(JobApplicationEntity app, String newStatus, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Mark as " + newStatus)
                .setMessage("Update application status for " + app.getApplicantName() + " (Roll " + app.getApplicantRoll() + ") to " + newStatus + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    repository.updateJobApplicationStatus(app.getId(), newStatus, res -> {
                        if (!isAdded()) return;
                        app.setStatus(newStatus);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Application marked as " + newStatus + " and notification sent!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSendMessageDialog(JobApplicationEntity app) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());

        final EditText etMsg = new EditText(requireContext());
        etMsg.setHint("Write a message to " + app.getApplicantName());
        etMsg.setText("Regarding your application for " + app.getJobTitle() + ": ");
        etMsg.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Message " + app.getApplicantName())
                .setMessage("Student: Roll " + app.getApplicantRoll() + " (" + app.getApplicantDept() + ")")
                .setView(etMsg)
                .setPositiveButton("Send", (dialog, which) -> {
                    String content = etMsg.getText().toString().trim();
                    if (content.isEmpty()) return;
                    repository.sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            app.getApplicantRoll(), app.getApplicantName(),
                            content,
                            msgId -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Message sent to " + app.getApplicantName() + "!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentPortfolioDialog(JobApplicationEntity app) {
        if (getContext() == null) return;
        repository.getStudentProfile(app.getApplicantRoll(), profile -> {
            if (!isAdded()) return;
            StringBuilder sb = new StringBuilder();
            sb.append("Applicant: ").append(app.getApplicantName()).append("\n")
                    .append("Roll: ").append(app.getApplicantRoll()).append(" • ").append(app.getApplicantDept()).append("\n")
                    .append("CGPA: ").append(app.getApplicantCgpa()).append("\n\n")
                    .append("Position: ").append(app.getJobTitle()).append("\n")
                    .append("Statement:\n\"").append(app.getStatement()).append("\"\n\n")
                    .append("Skills: ").append(app.getApplicantSkills()).append("\n");

            if (profile != null) {
                if (profile.getProjects() != null) sb.append("Projects: ").append(profile.getProjects()).append("\n");
                if (profile.getGithubUrl() != null) sb.append("GitHub: ").append(profile.getGithubUrl()).append("\n");
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Verified Applicant Portfolio")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadApplicants();
    }
}
