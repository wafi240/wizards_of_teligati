package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.OpportunityAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.JobApplicationEntity;
import com.teligati.app.data.entity.JobPostingEntity;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.Opportunity;
import java.util.ArrayList;
import java.util.List;

public class AlumniAssistantJobsFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvJobs;
    private EditText etSearch;
    private TextView tvEmpty;
    private OpportunityAdapter adapter;
    private List<Opportunity> allJobs = new ArrayList<>();
    private List<Opportunity> displayedJobs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alumni_assistant_jobs, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvJobs = view.findViewById(R.id.rvAlumniAssistantJobs);
        etSearch = view.findViewById(R.id.etSearchAssistantJobs);
        tvEmpty = view.findViewById(R.id.tvEmptyAssistantJobs);
        Button btnPost = view.findViewById(R.id.btnPostAssistantJob);

        rvJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OpportunityAdapter(displayedJobs, new OpportunityAdapter.OnOpportunityActionListener() {
            @Override
            public void onApply(Opportunity opp, int position) {
                showManageJobDialog(opp, position);
            }

            @Override
            public void onViewDetails(Opportunity opp, int position) {
                showManageJobDialog(opp, position);
            }
        });
        rvJobs.setAdapter(adapter);

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> showCreateAssistantJobDialog());
        }

        setupSearch();
        loadJobs();

        return view;
    }

    private void loadJobs() {
        if (!isAdded()) return;
        repository.getAssistantJobs(jobs -> {
            if (!isAdded()) return;
            allJobs = DataMapper.toOpportunityList(jobs);
            filterJobs(etSearch != null ? etSearch.getText().toString() : "");
        });
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterJobs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterJobs(String query) {
        displayedJobs.clear();
        String q = query.toLowerCase().trim();
        for (Opportunity o : allJobs) {
            if (q.isEmpty()
                    || o.getTitle().toLowerCase().contains(q)
                    || o.getOrganization().toLowerCase().contains(q)
                    || o.getDescription().toLowerCase().contains(q)) {
                displayedJobs.add(o);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedJobs.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showCreateAssistantJobDialog() {
        if (getContext() == null) return;
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        final EditText etTitle = createInput("Role Title (e.g. Python Script Automation Assistant)", "");
        final EditText etOrg = createInput("Organization / Advisory", "KUET Alumni Tech Venture");
        final EditText etStipend = createInput("Compensation / Stipend (e.g. ৳15,000 / month)", "৳15,000 / month");
        final EditText etDuration = createInput("Duration / Hours (e.g. Flexible 10 hrs/week)", "Flexible 10 hrs/week");
        final EditText etRequirements = createInput("Required Skills (e.g. Python, SQL, REST APIs)", "Python, Git");
        final EditText etDesc = createInput("Task Description", "Assist with automation pipelines and data scraping.");

        layout.addView(etTitle);
        layout.addView(etOrg);
        layout.addView(etStipend);
        layout.addView(etDuration);
        layout.addView(etRequirements);
        layout.addView(etDesc);

        SessionManager session = SessionManager.getInstance(requireContext());

        new AlertDialog.Builder(requireContext())
                .setTitle("Post Paid Assistant Job")
                .setView(scrollView)
                .setPositiveButton("Publish & Escrow", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String pay = etStipend.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Role Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JobPostingEntity job = new JobPostingEntity(
                            0,
                            title,
                            "ASSISTANT_WORK",
                            etOrg.getText().toString().trim().isEmpty() ? "Alumni Tech Advisory" : etOrg.getText().toString().trim(),
                            "Remote",
                            session.getUserName(),
                            "Alumni Lead",
                            pay.isEmpty() ? "৳15,000 / month" : pay,
                            etDuration.getText().toString().trim().isEmpty() ? "Flexible 10 hrs/wk" : etDuration.getText().toString().trim(),
                            etDesc.getText().toString().trim(),
                            etRequirements.getText().toString().trim(),
                            3.0,
                            false,
                            session.getUserId(),
                            System.currentTimeMillis()
                    );

                    repository.createJobPosting(job, id -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Assistant opportunity created! [Demo Escrow Ready — Simulation Only]", Toast.LENGTH_LONG).show();
                        loadJobs();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageJobDialog(Opportunity opp, int position) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(opp.getTitle())
                .setMessage("Organization: " + opp.getOrganization() + "\n"
                        + "Stipend: " + opp.getCompensation() + "\n"
                        + "Duration: " + opp.getDuration() + "\n\n"
                        + "Description:\n" + opp.getDescription())
                .setPositiveButton("View Applicants", (dialog, which) -> showApplicantsDialog(opp))
                .setNeutralButton("Edit", (dialog, which) -> showEditAssistantJobDialog(opp))
                .setNegativeButton("Delete Posting", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Assistant Opportunity")
                            .setMessage("Remove '" + opp.getTitle() + "' from active listings?")
                            .setPositiveButton("Delete", (d, w) -> {
                                String callerId = SessionManager.getInstance(requireContext()).getUserId();
                                repository.deleteJobPosting(opp.getId(), callerId, success -> {
                                    if (!isAdded()) return;
                                    if (Boolean.TRUE.equals(success)) {
                                        Toast.makeText(requireContext(), "Posting removed from Room database.", Toast.LENGTH_SHORT).show();
                                        loadJobs();
                                    } else {
                                        Toast.makeText(requireContext(), "Unauthorized: You can only remove your own postings.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }

    private void showEditAssistantJobDialog(Opportunity opp) {
        if (getContext() == null) return;
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        final EditText etTitle = createInput("Role Title", opp.getTitle());
        final EditText etOrg = createInput("Organization", opp.getOrganization());
        final EditText etStipend = createInput("Stipend", opp.getCompensation());
        final EditText etDuration = createInput("Duration", opp.getDuration());
        final EditText etDesc = createInput("Description", opp.getDescription());

        layout.addView(etTitle);
        layout.addView(etOrg);
        layout.addView(etStipend);
        layout.addView(etDuration);
        layout.addView(etDesc);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Assistant Opportunity")
                .setView(scrollView)
                .setPositiveButton("Save Updates", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JobPostingEntity entity = new JobPostingEntity(
                            opp.getId(),
                            title,
                            opp.getCategory().name(),
                            etOrg.getText().toString().trim(),
                            opp.getLocation(),
                            opp.getPostedByName(),
                            opp.getPosterRole(),
                            etStipend.getText().toString().trim(),
                            etDuration.getText().toString().trim(),
                            etDesc.getText().toString().trim(),
                            "Updated requirements",
                            3.0,
                            opp.isApplied(),
                            SessionManager.getInstance(requireContext()).getUserId(),
                            System.currentTimeMillis()
                    );
                    repository.updateJobPosting(entity, res -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Assistant job updated in Room database!", Toast.LENGTH_SHORT).show();
                        loadJobs();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showApplicantsDialog(Opportunity opp) {
        if (getContext() == null) return;
        repository.getApplicationsForJob(opp.getId(), apps -> {
            if (!isAdded()) return;
            if (apps == null || apps.isEmpty()) {
                Toast.makeText(requireContext(), "No students have applied for this position yet.", Toast.LENGTH_LONG).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (JobApplicationEntity app : apps) {
                sb.append("• ").append(app.getApplicantName())
                        .append(" (Roll ").append(app.getApplicantRoll())
                        .append(", CGPA ").append(app.getApplicantCgpa()).append(")\n")
                        .append("  Status: ").append(app.getStatus()).append("\n")
                        .append("  Statement: \"").append(app.getStatement()).append("\"\n\n");
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Applicants for " + opp.getTitle())
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("OK", null)
                    .show();
        });
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
        loadJobs();
    }
}
