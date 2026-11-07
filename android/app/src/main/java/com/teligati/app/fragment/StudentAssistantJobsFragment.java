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
import com.teligati.app.adapter.OpportunityAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.Opportunity;
import java.util.ArrayList;
import java.util.List;

public class StudentAssistantJobsFragment extends Fragment {
    private RecyclerView rvJobs;
    private EditText etSearch;
    private TextView tvEmptyState;
    private OpportunityAdapter adapter;
    private List<Opportunity> allOpportunities = new ArrayList<>();
    private List<Opportunity> displayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_assistant_jobs, container, false);

        rvJobs = view.findViewById(R.id.rvAssistantJobs);
        etSearch = view.findViewById(R.id.etSearchJobs);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        rvJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OpportunityAdapter(displayedList, new OpportunityAdapter.OnOpportunityActionListener() {
            @Override
            public void onApply(Opportunity opp, int position) {
                showApplyDialog(opp, position);
            }

            @Override
            public void onViewDetails(Opportunity opp, int position) {
                if (getContext() != null) {
                    com.teligati.app.OpportunityDetailActivity.start(requireContext(), opp);
                }
            }
        });
        rvJobs.setAdapter(adapter);

        setupSearch();
        loadData();

        return view;
    }

    private void loadData() {
        if (!isAdded()) return;
        AppRepository repo = AppRepository.getInstance(requireContext());
        String roll = SessionManager.getInstance(requireContext()).getUserId();

        repo.getAssistantJobs(entities -> {
            if (!isAdded()) return;
            allOpportunities = DataMapper.toOpportunityList(entities);

            // Check applied status from Room for current student
            repo.getJobApplicationsByStudent(roll, apps -> {
                if (!isAdded()) return;
                for (Opportunity opp : allOpportunities) {
                    for (com.teligati.app.data.entity.JobApplicationEntity app : apps) {
                        if (app.getJobId() == opp.getId()) {
                            opp.setApplied(true);
                            break;
                        }
                    }
                }
                filterJobs(etSearch != null ? etSearch.getText().toString() : "");
            });
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
        displayedList.clear();
        String q = query.toLowerCase().trim();
        for (Opportunity opp : allOpportunities) {
            if (q.isEmpty()
                    || opp.getTitle().toLowerCase().contains(q)
                    || opp.getOrganization().toLowerCase().contains(q)
                    || opp.getPostedByName().toLowerCase().contains(q)
                    || opp.getDescription().toLowerCase().contains(q)) {
                displayedList.add(opp);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showJobDetailsDialog(Opportunity opp, int position) {
        if (getContext() == null) return;
        String message = "Organization: " + opp.getOrganization() + "\n"
                + "Location: " + opp.getLocation() + "\n"
                + "Posted By: " + opp.getPostedByName() + " (" + opp.getPosterRole() + ")\n"
                + "Compensation: " + opp.getCompensation() + "\n"
                + "Duration: " + opp.getDuration() + "\n\n"
                + "Description:\n" + opp.getDescription();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(opp.getTitle())
                .setMessage(message)
                .setNegativeButton("Close", null);

        if (!opp.isApplied()) {
            builder.setPositiveButton("Apply Now", (dialog, which) -> showApplyDialog(opp, position));
        } else {
            builder.setPositiveButton("Applied ✓", null);
        }
        builder.show();
    }

    private void showApplyDialog(Opportunity opp, int position) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        if (!session.isVerified()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⏳ Verification Required")
                    .setMessage("Your account is pending institutional email verification (pending external SMTP service deployment). Applying for assistant jobs is restricted until verified.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        AppRepository repo = AppRepository.getInstance(requireContext());
        String roll = session.getUserId();

        // 1. Prevent duplicate application
        repo.checkApplicationExists(opp.getId(), roll, alreadyApplied -> {
            if (!isAdded()) return;
            if (alreadyApplied) {
                Toast.makeText(requireContext(), "You have already applied for this position.", Toast.LENGTH_LONG).show();
                opp.setApplied(true);
                adapter.notifyItemChanged(position);
                return;
            }

            // 2. Display submission pitch dialog
            final EditText etStatement = new EditText(requireContext());
            etStatement.setHint("Why are you a good fit for this role?");
            etStatement.setText("I am enthusiastic to contribute my skills to this project and commit required hours.");
            etStatement.setPadding(32, 24, 32, 24);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Apply for " + opp.getTitle())
                    .setMessage("Applicant: " + session.getUserName() + " (Roll " + roll + ", Dept " + session.getUserDept() + ", CGPA " + session.getUserCgpa() + ")\n\nCover Note:")
                    .setView(etStatement)
                    .setPositiveButton("Submit Application", (dialog, which) -> {
                        String statement = etStatement.getText().toString().trim();
                        if (statement.isEmpty()) {
                            statement = "Standard KUET verified applicant submission.";
                        }
                        repo.applyForJob(
                                opp.getId(), roll, session.getUserName(), session.getUserDept(),
                                session.getUserCgpa(), "Java, Python, ROS, Git", statement,
                                success -> {
                                    if (!isAdded()) return;
                                    if (success) {
                                        opp.setApplied(true);
                                        adapter.notifyItemChanged(position);
                                        Toast.makeText(requireContext(), "Application submitted and saved to Room!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Application failed: already applied.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
