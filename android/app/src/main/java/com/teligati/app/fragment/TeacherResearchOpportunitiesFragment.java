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

public class TeacherResearchOpportunitiesFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvOpportunities;
    private EditText etSearch;
    private TextView tvEmpty;
    private OpportunityAdapter adapter;
    private List<Opportunity> allOpportunities = new ArrayList<>();
    private List<Opportunity> displayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_research_opportunities, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvOpportunities = view.findViewById(R.id.rvTeacherOpportunities);
        etSearch = view.findViewById(R.id.etSearchResearchOpp);
        tvEmpty = view.findViewById(R.id.tvEmptyResearchOpp);
        Button btnPost = view.findViewById(R.id.btnPostResearchOpportunity);

        rvOpportunities.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OpportunityAdapter(displayedList, new OpportunityAdapter.OnOpportunityActionListener() {
            @Override
            public void onApply(Opportunity opp, int position) {
                showManageOpportunityDialog(opp, position);
            }

            @Override
            public void onViewDetails(Opportunity opp, int position) {
                showManageOpportunityDialog(opp, position);
            }
        });
        rvOpportunities.setAdapter(adapter);

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> showCreateOpportunityDialog());
        }

        setupSearch();
        loadOpportunities();

        return view;
    }

    private void loadOpportunities() {
        if (!isAdded()) return;
        repository.getResearchOpportunities(jobs -> {
            if (!isAdded()) return;
            allOpportunities = DataMapper.toOpportunityList(jobs);
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
        for (Opportunity o : allOpportunities) {
            if (q.isEmpty()
                    || o.getTitle().toLowerCase().contains(q)
                    || o.getOrganization().toLowerCase().contains(q)
                    || o.getDescription().toLowerCase().contains(q)) {
                displayedList.add(o);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showCreateOpportunityDialog() {
        if (getContext() == null) return;
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        final EditText etTitle = createInput("Research Title (e.g. Distributed Consensus in Edge AI)", "");
        final EditText etLab = createInput("Research Lab / Department", "Intelligent Systems & Big Data Lab");
        final EditText etFunding = createInput("Funding / Stipend (e.g. ৳12,000 / month)", "৳12,000 / month");
        final EditText etDuration = createInput("Duration & Positions (e.g. 6 Months • 2 Positions)", "6 Months Duration");
        final EditText etRequirements = createInput("Qualifications (e.g. Min CGPA 3.4, Python, PyTorch)", "Python, PyTorch, Linux");
        final EditText etDesc = createInput("Scope & Objectives", "Undergraduate research assistantship with potential IEEE conference co-authorship.");

        layout.addView(etTitle);
        layout.addView(etLab);
        layout.addView(etFunding);
        layout.addView(etDuration);
        layout.addView(etRequirements);
        layout.addView(etDesc);

        SessionManager session = SessionManager.getInstance(requireContext());

        new AlertDialog.Builder(requireContext())
                .setTitle("Post Research Opportunity")
                .setView(scrollView)
                .setPositiveButton("Publish to Portal", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JobPostingEntity opp = new JobPostingEntity(
                            0,
                            title,
                            "RESEARCH_COLLABORATION",
                            etLab.getText().toString().trim().isEmpty() ? "KUET Research Lab" : etLab.getText().toString().trim(),
                            "KUET Campus",
                            session.getUserName(),
                            "Faculty Lead",
                            etFunding.getText().toString().trim().isEmpty() ? "Funded Stipend" : etFunding.getText().toString().trim(),
                            etDuration.getText().toString().trim().isEmpty() ? "6 Months" : etDuration.getText().toString().trim(),
                            etDesc.getText().toString().trim(),
                            etRequirements.getText().toString().trim(),
                            3.4,
                            false,
                            session.getUserId(),
                            System.currentTimeMillis()
                    );

                    repository.createJobPosting(opp, id -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Research opening posted and stored in Room database!", Toast.LENGTH_SHORT).show();
                        loadOpportunities();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManageOpportunityDialog(Opportunity opp, int position) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(opp.getTitle())
                .setMessage("Lab: " + opp.getOrganization() + "\n"
                        + "Funding: " + opp.getCompensation() + "\n"
                        + "Duration: " + opp.getDuration() + "\n\n"
                        + "Scope:\n" + opp.getDescription())
                .setPositiveButton("View Applications", (dialog, which) -> showApplicationsDialog(opp))
                .setNeutralButton("Edit", (dialog, which) -> showEditOpportunityDialog(opp))
                .setNegativeButton("Delete Opening", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Research Opening")
                            .setMessage("Remove '" + opp.getTitle() + "' from active research calls?")
                            .setPositiveButton("Delete", (d, w) -> {
                                repository.deleteJobPosting(opp.getId(), success -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Research opening deleted from Room.", Toast.LENGTH_SHORT).show();
                                    loadOpportunities();
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }

    private void showEditOpportunityDialog(Opportunity opp) {
        if (getContext() == null) return;
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        final EditText etTitle = createInput("Title", opp.getTitle());
        final EditText etLab = createInput("Lab", opp.getOrganization());
        final EditText etPay = createInput("Stipend / Funding", opp.getCompensation());
        final EditText etDuration = createInput("Duration", opp.getDuration());
        final EditText etDesc = createInput("Scope", opp.getDescription());

        layout.addView(etTitle);
        layout.addView(etLab);
        layout.addView(etPay);
        layout.addView(etDuration);
        layout.addView(etDesc);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Research Opportunity")
                .setView(scrollView)
                .setPositiveButton("Save Updates", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) return;

                    JobPostingEntity entity = new JobPostingEntity(
                            opp.getId(),
                            title,
                            opp.getCategory().name(),
                            etLab.getText().toString().trim(),
                            opp.getLocation(),
                            opp.getPostedByName(),
                            opp.getPosterRole(),
                            etPay.getText().toString().trim(),
                            etDuration.getText().toString().trim(),
                            etDesc.getText().toString().trim(),
                            "Min CGPA 3.4",
                            3.4,
                            opp.isApplied(),
                            SessionManager.getInstance(requireContext()).getUserId(),
                            System.currentTimeMillis()
                    );

                    repository.updateJobPosting(entity, res -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Research opening updated in Room database!", Toast.LENGTH_SHORT).show();
                        loadOpportunities();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showApplicationsDialog(Opportunity opp) {
        if (getContext() == null) return;
        repository.getApplicationsForJob(opp.getId(), apps -> {
            if (!isAdded()) return;
            if (apps == null || apps.isEmpty()) {
                Toast.makeText(requireContext(), "No students have applied for this opportunity yet.", Toast.LENGTH_LONG).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (JobApplicationEntity app : apps) {
                sb.append("• ").append(app.getApplicantName())
                        .append(" (Roll ").append(app.getApplicantRoll()).append(", CGPA ").append(app.getApplicantCgpa()).append(")\n")
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
        loadOpportunities();
    }
}
