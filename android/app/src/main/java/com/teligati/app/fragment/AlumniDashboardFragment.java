package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.OpportunityAdapter;
import com.teligati.app.adapter.ReferenceRequestAdapter;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.JobPostingEntity;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.ReferenceRequest;
import java.util.ArrayList;
import java.util.List;

public class AlumniDashboardFragment extends Fragment {
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alumni, container, false);
        repository = AppRepository.getInstance(requireContext());
        setupView(view);
        return view;
    }

    private void setupView(View view) {
        String userId = "tanvir.munich@kuet.ac.bd";
        if (getArguments() != null && getArguments().getString("USER_ID") != null) {
            userId = getArguments().getString("USER_ID");
        }
        final String finalUserId = userId;

        TextView tvName = view.findViewById(R.id.tvAlumniName);
        TextView tvMeta = view.findViewById(R.id.tvAlumniMeta);
        Button btnProfile = view.findViewById(R.id.btnAlumniProfile);
        Button btnPostJob = view.findViewById(R.id.btnAlumniPostJob);
        Button btnPostAssistant = view.findViewById(R.id.btnAlumniPostAssistantJob);
        RecyclerView rvRequests = view.findViewById(R.id.rvAlumniReferenceRequests);
        RecyclerView rvMyJobs = view.findViewById(R.id.rvAlumniMyJobs);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        com.teligati.app.data.SessionManager session = com.teligati.app.data.SessionManager.getInstance(requireContext());
        String alumniName = session.getUserName();
        String alumniDept = session.getUserDept();
        boolean isVerified = session.isVerified();

        if (tvName != null) tvName.setText(alumniName + (isVerified ? " ✓ Verified Alumni" : " • Alumni"));
        if (tvMeta != null) tvMeta.setText(alumniDept + " • " + userId);

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (getContext() == null) return;
                repository.getAlumniProfile(finalUserId, profile -> {
                    String info = "Name: " + (profile != null ? profile.getName() : "Engr. Tanvir Ahmed") +
                            "\nRole: " + (profile != null ? profile.getCurrentPosition() : "Staff Engineer") +
                            "\nCompany: " + (profile != null ? profile.getCurrentCompany() : "BMW Group") +
                            "\nLocation: " + (profile != null ? profile.getCity() + ", " + profile.getCountry() : "Munich, Germany") +
                            "\nSkills: " + (profile != null ? profile.getSkills() : "Distributed Systems, Java, Cloud") +
                            "\nVerified by KUET Alumni Registry.";
                    new AlertDialog.Builder(getContext())
                            .setTitle("Alumni Profile")
                            .setMessage(info)
                            .setPositiveButton("Close", null)
                            .show();
                });
            });
        }

        // Post Professional Job -> Saved into Room
        if (btnPostJob != null) {
            btnPostJob.setOnClickListener(v -> {
                if (getContext() == null) return;
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 20, 40, 10);

                final EditText etTitle = new EditText(getContext());
                etTitle.setHint("Job Title (e.g. Software Engineer - Cloud)");
                layout.addView(etTitle);

                final EditText etCompany = new EditText(getContext());
                etCompany.setHint("Company (e.g. BMW Group, Munich / Remote)");
                layout.addView(etCompany);

                final EditText etSalary = new EditText(getContext());
                etSalary.setHint("Salary (e.g. €60,000 / year or ৳80,000 / month)");
                layout.addView(etSalary);

                new AlertDialog.Builder(getContext())
                        .setTitle("Post Professional Job for KUETians")
                        .setView(layout)
                        .setPositiveButton("Publish Job", (d, w) -> {
                            String title = etTitle.getText().toString().trim();
                            String company = etCompany.getText().toString().trim();
                            String salary = etSalary.getText().toString().trim();
                            if (!title.isEmpty()) {
                                JobPostingEntity newJob = new JobPostingEntity(
                                        0,
                                        title,
                                        "FULLTIME_JOBS",
                                        company.isEmpty() ? "BMW Munich / Remote" : company,
                                        "Munich, Germany / Remote",
                                        "Engr. Tanvir Ahmed",
                                        "Staff Engineer @ BMW (KUET Alumni)",
                                        salary.isEmpty() ? "Competitive" : salary,
                                        "Full-time Permanent",
                                        "Professional opportunity posted exclusively for KUET graduates.",
                                        "Java, Cloud, Microservices",
                                        3.20,
                                        false,
                                        finalUserId,
                                        System.currentTimeMillis()
                                );
                                repository.createJobPosting(newJob, id -> {
                                    Toast.makeText(getContext(), "Job published and saved to database!", Toast.LENGTH_SHORT).show();
                                    loadAlumniJobs(rvMyJobs, finalUserId);
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Post Paid Assistant Job with Money -> Saved into Room
        if (btnPostAssistant != null) {
            btnPostAssistant.setOnClickListener(v -> {
                if (getContext() == null) return;
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(40, 20, 40, 10);

                final EditText etTitle = new EditText(getContext());
                etTitle.setHint("Assistant Role (e.g. Python Script Automation)");
                layout.addView(etTitle);

                final EditText etMoney = new EditText(getContext());
                etMoney.setHint("Stipend (e.g. ৳15,000 / month)");
                layout.addView(etMoney);

                final EditText etHours = new EditText(getContext());
                etHours.setHint("Weekly Hours (e.g. 10 hrs/week flexible)");
                layout.addView(etHours);

                new AlertDialog.Builder(getContext())
                        .setTitle("Post Paid Assistant Job with Money")
                        .setView(layout)
                        .setPositiveButton("Publish Assistant Job", (d, w) -> {
                            String title = etTitle.getText().toString().trim();
                            String money = etMoney.getText().toString().trim();
                            String hours = etHours.getText().toString().trim();
                            if (!title.isEmpty()) {
                                JobPostingEntity newJob = new JobPostingEntity(
                                        0,
                                        title,
                                        "ASSISTANT_WORK",
                                        "Alumni Tech Advisory (Munich)",
                                        "Remote",
                                        "Engr. Tanvir Ahmed (CSE '12)",
                                        "Staff Engineer @ BMW (KUET Alumni)",
                                        money.isEmpty() ? "৳15,000 / month" : money,
                                        hours.isEmpty() ? "Flexible (10-12 hrs/week)" : hours,
                                        "Paid assistant role replacing tuition with engineering work.",
                                        "Python, SQL, Git",
                                        3.00,
                                        false,
                                        finalUserId,
                                        System.currentTimeMillis()
                                );
                                repository.createJobPosting(newJob, id -> {
                                    Toast.makeText(getContext(), "Paid assistant job published & saved to database!", Toast.LENGTH_SHORT).show();
                                    loadAlumniJobs(rvMyJobs, finalUserId);
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Reference Requests from Room
        if (rvRequests != null) {
            rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
            loadReferenceRequests(rvRequests);
        }

        // Active Listings from Room
        if (rvMyJobs != null) {
            rvMyJobs.setLayoutManager(new LinearLayoutManager(getContext()));
            loadAlumniJobs(rvMyJobs, finalUserId);
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getActivity() == null) return;
                com.teligati.app.data.repository.AuthRepository.getInstance(requireContext())
                        .logout(new com.teligati.app.data.repository.AuthRepository.Callback<Void>() {
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
                                Intent intent = new Intent(getActivity(), AuthGatewayActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                getActivity().finish();
                            }
                        });
            });
        }
    }

    private ReferenceRequestAdapter refAdapter;
    private List<ReferenceRequest> refList = new ArrayList<>();

    private void loadReferenceRequests(RecyclerView rv) {
        repository.getReferenceRequests(list -> {
            if (!isAdded() || rv == null) return;
            refList = DataMapper.toReferenceRequestList(list);
            refAdapter = new ReferenceRequestAdapter(refList, new ReferenceRequestAdapter.OnReferenceActionListener() {
                @Override
                public void onEndorse(ReferenceRequest req, int position) {
                    showEndorseDialog(req, position);
                }

                @Override
                public void onDecline(ReferenceRequest req, int position) {
                    showDeclineDialog(req, position);
                }

                @Override
                public void onGuidance(ReferenceRequest req, int position) {
                    showGuidanceDialog(req, position);
                }

                @Override
                public void onViewPortfolio(ReferenceRequest req, int position) {
                    showStudentPortfolioDialog(req);
                }
            });
            rv.setAdapter(refAdapter);
        });
    }

    private void showEndorseDialog(ReferenceRequest req, int position) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Endorse " + req.getStudentName())
                .setMessage("Submit official referral for " + req.getStudentName() + " to " + req.getTargetCompany() + "?")
                .setPositiveButton("Accept & Endorse", (d, w) -> {
                    repository.updateReferenceRequestStatus(req.getId(), "ACCEPTED", res -> {
                        if (!isAdded()) return;
                        req.setStatus("ACCEPTED");
                        if (refAdapter != null) {
                            refAdapter.notifyItemChanged(position);
                        }
                        Toast.makeText(requireContext(), "Referral confirmed and saved to database!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeclineDialog(ReferenceRequest req, int position) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Decline Request")
                .setMessage("Politely decline referral request from " + req.getStudentName() + "?")
                .setPositiveButton("Decline", (d, w) -> {
                    repository.updateReferenceRequestStatus(req.getId(), "DECLINED", res -> {
                        if (!isAdded()) return;
                        req.setStatus("DECLINED");
                        if (refAdapter != null) {
                            refAdapter.notifyItemChanged(position);
                        }
                        Toast.makeText(requireContext(), "Request marked as declined.", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGuidanceDialog(ReferenceRequest req, int position) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        final EditText etGuidance = new EditText(requireContext());
        etGuidance.setHint("Write career advice or preparation guidance...");
        etGuidance.setText("Hi " + req.getStudentName() + ", please prepare your system design & LeetCode fundamentals for " + req.getTargetCompany() + ".");
        etGuidance.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Mentorship for " + req.getStudentName())
                .setMessage("Send mentorship advice & suggestions directly to the student:")
                .setView(etGuidance)
                .setPositiveButton("Send Guidance", (dialog, which) -> {
                    String advice = etGuidance.getText().toString().trim();
                    if (advice.isEmpty()) return;

                    repository.sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            req.getStudentRoll(), req.getStudentName(),
                            "Career Guidance for " + req.getTargetCompany() + ": " + advice,
                            msgId -> {
                                if (!isAdded()) return;
                                req.setStatus("Guided");
                                if (refAdapter != null) {
                                    refAdapter.notifyItemChanged(position);
                                }
                                Toast.makeText(requireContext(), "Mentorship advice sent to student!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentPortfolioDialog(ReferenceRequest req) {
        if (getContext() == null) return;
        String info = "Student: " + req.getStudentName() + "\n"
                + "Department: " + req.getStudentDept() + " • Roll: " + req.getStudentRoll() + "\n"
                + "Target Company: " + req.getTargetCompany() + "\n"
                + "Status: " + req.getStatus() + "\n\n"
                + "Message:\n\"" + req.getMessage() + "\"";

        new AlertDialog.Builder(requireContext())
                .setTitle("Reference Applicant Portfolio")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    private void loadAlumniJobs(RecyclerView rv, String userId) {
        repository.getAlumniJobs(userId, list -> {
            if (rv != null) {
                rv.setAdapter(new OpportunityAdapter(DataMapper.toOpportunityList(list)));
            }
        });
    }
}
