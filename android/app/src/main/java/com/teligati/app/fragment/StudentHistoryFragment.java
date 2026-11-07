package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.teligati.app.adapter.StudentHistoryAdapter;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentHistoryFragment extends Fragment {
    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private StudentHistoryAdapter adapter;
    private final List<StudentHistoryAdapter.HistoryEntry> historyList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_history, container, false);
        rvHistory = view.findViewById(R.id.rvStudentHistory);
        tvEmpty = view.findViewById(R.id.tvEmptyHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentHistoryAdapter(historyList, (item, position) -> showHistoryDetailsDialog(item));
        rvHistory.setAdapter(adapter);

        loadHistory();
        return view;
    }

    private void loadHistory() {
        if (!isAdded()) return;
        AppRepository repo = AppRepository.getInstance(requireContext());
        String roll = SessionManager.getInstance(requireContext()).getUserId();

        historyList.clear();

        // 1. Fetch Job Applications
        repo.getJobApplicationsByStudent(roll, jobApps -> {
            if (!isAdded()) return;
            if (jobApps != null) {
                for (com.teligati.app.data.entity.JobApplicationEntity app : jobApps) {
                    historyList.add(new StudentHistoryAdapter.HistoryEntry(
                            app.getId(),
                            "💼 Job Application",
                            app.getJobTitle(),
                            "Applicant: " + app.getApplicantName() + " (" + app.getApplicantDept() + ")",
                            app.getStatus(),
                            app.getAppliedAt()
                    ));
                }
            }

            // 2. Fetch Research Applications
            repo.getResearchApplicationsByStudent(roll, researchApps -> {
                if (!isAdded()) return;
                if (researchApps != null) {
                    for (com.teligati.app.data.entity.ResearchApplicationEntity rApp : researchApps) {
                        historyList.add(new StudentHistoryAdapter.HistoryEntry(
                            rApp.getId(),
                            "🔬 Research Collaboration",
                            rApp.getOpportunityTitle(),
                            "Statement: " + rApp.getLabInterest(),
                            rApp.getStatus(),
                            rApp.getAppliedAt()
                        ));
                    }
                }

                // 3. Fetch Reference Requests
                repo.getStudentReferenceRequests(roll, refRequests -> {
                    if (!isAdded()) return;
                    if (refRequests != null) {
                        for (com.teligati.app.data.entity.ReferenceRequestEntity req : refRequests) {
                            String recName = (req.getRecipientName() != null && !req.getRecipientName().isBlank())
                                    ? req.getRecipientName() : req.getAlumniId();
                            String sub = "Referee: " + recName
                                    + (req.getPurpose() != null ? " • " + req.getPurpose() : "")
                                    + "\n\"" + req.getMessage() + "\"";
                            if (req.getRecommendationText() != null && !req.getRecommendationText().isBlank()) {
                                sub += "\n\nRecommendation Letter:\n\"" + req.getRecommendationText() + "\"";
                            }
                            historyList.add(new StudentHistoryAdapter.HistoryEntry(
                                req.getId(),
                                "🤝 Reference Request",
                                req.getTargetCompany(),
                                sub,
                                req.getStatus(),
                                req.getRequestedAt()
                            ));
                        }
                    }

                    // 4. Fetch Demo Financial Transactions
                    repo.getStudentHistory(roll, txList -> {
                        if (!isAdded()) return;
                        if (txList != null) {
                            for (com.teligati.app.data.entity.TransactionEntity tx : txList) {
                                historyList.add(new StudentHistoryAdapter.HistoryEntry(
                                    "💰 Demo " + (tx.getCategory() != null ? tx.getCategory() : "Transaction"),
                                    tx.getTitle() != null ? tx.getTitle() : "Simulated Transaction",
                                    "Amount: ৳" + (int) tx.getAmount() + " • [Simulation Only]",
                                    tx.getStatus(),
                                    System.currentTimeMillis()
                                ));
                            }
                        }

                        // Sort newest first
                        Collections.sort(historyList, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                        adapter.notifyDataSetChanged();
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    });
                });
            });
        });
        repo.syncTransactions(null);
    }

    private void showHistoryDetailsDialog(StudentHistoryAdapter.HistoryEntry item) {
        if (getContext() == null || item == null) return;
        AppRepository repo = AppRepository.getInstance(requireContext());

        if (item.category != null && item.category.contains("Research")) {
            String status = item.status != null ? item.status.trim() : "PENDING";
            boolean isRevisionRequested = "REVISION_REQUESTED".equalsIgnoreCase(status) || status.toLowerCase().contains("revision");
            boolean isPending = "PENDING".equalsIgnoreCase(status) || "APPLIED".equalsIgnoreCase(status) || "UNDER_REVIEW".equalsIgnoreCase(status);

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle(item.title)
                    .setMessage("Type: " + item.category + "\n"
                            + "Status: " + status + "\n\n"
                            + item.subtitle);

            if (isRevisionRequested) {
                builder.setPositiveButton("Revise & Resubmit", (dialog, which) -> {
                    final EditText etRevise = new EditText(requireContext());
                    etRevise.setHint("Revised statement & additional qualifications");
                    etRevise.setText(item.subtitle != null && item.subtitle.startsWith("Statement: ") ? item.subtitle.substring(11) : "");
                    etRevise.setPadding(32, 24, 32, 24);

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Resubmit Research Application")
                            .setMessage("Update your statement addressing faculty review notes:")
                            .setView(etRevise)
                            .setPositiveButton("Resubmit", (d, w) -> {
                                String updated = etRevise.getText().toString().trim();
                                if (updated.isEmpty()) {
                                    Toast.makeText(requireContext(), "Statement cannot be empty", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                repo.resubmitResearchApplication(item.id, updated, "Updated Skills", success -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Application resubmitted successfully!", Toast.LENGTH_SHORT).show();
                                    loadHistory();
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }

            if (isPending) {
                builder.setNeutralButton("Withdraw Application", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Withdraw Application")
                            .setMessage("Are you sure you want to withdraw your research application for '" + item.title + "'?")
                            .setPositiveButton("Withdraw", (d, w) -> {
                                repo.withdrawResearchApplication(item.id, success -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Research application withdrawn.", Toast.LENGTH_SHORT).show();
                                    loadHistory();
                                });
                            })
                            .setNegativeButton("Keep Active", null)
                            .show();
                });
            }

            builder.setNegativeButton("Close", null);
            builder.show();
        } else if (item.category != null && item.category.contains("Reference")) {
            String status = item.status != null ? item.status.trim().toUpperCase() : "PENDING";
            boolean isPending = "PENDING".equalsIgnoreCase(status);

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle(item.title)
                    .setMessage("Type: " + item.category + "\n"
                            + "Status: " + status + "\n\n"
                            + item.subtitle);

            if (isPending) {
                builder.setNeutralButton("Cancel Request", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Cancel Reference Request")
                            .setMessage("Are you sure you want to cancel this reference request for '" + item.title + "'?")
                            .setPositiveButton("Cancel Request", (d, w) -> {
                                repo.cancelReferenceRequest(item.id, success -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Reference request cancelled.", Toast.LENGTH_SHORT).show();
                                    loadHistory();
                                });
                            })
                            .setNegativeButton("Keep Pending", null)
                            .show();
                });
            }

            builder.setPositiveButton("Close", null);
            builder.show();
        } else if (item.category != null && item.category.contains("Job")) {
            String status = item.status != null ? item.status.trim().toUpperCase() : "PENDING";
            boolean isPending = "PENDING".equalsIgnoreCase(status) || "APPLIED".equalsIgnoreCase(status) || "UNDER_REVIEW".equalsIgnoreCase(status) || status.contains("SHORTLIST");

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle(item.title)
                    .setMessage("Type: " + item.category + "\n"
                            + "Status: " + status + "\n\n"
                            + item.subtitle);

            if (isPending) {
                builder.setNeutralButton("Withdraw Application", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Withdraw Job Application")
                            .setMessage("Are you sure you want to withdraw your application for '" + item.title + "'?")
                            .setPositiveButton("Withdraw", (d, w) -> {
                                repo.withdrawJobApplication(item.id, success -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(requireContext(), "Job application withdrawn.", Toast.LENGTH_SHORT).show();
                                    loadHistory();
                                });
                            })
                            .setNegativeButton("Keep Active", null)
                            .show();
                });
            }

            builder.setPositiveButton("Close", null);
            builder.show();
        } else {
            new AlertDialog.Builder(requireContext())
                    .setTitle(item.title)
                    .setMessage("Category: " + item.category + "\n"
                            + "Status: " + item.status + "\n\n"
                            + item.subtitle)
                    .setPositiveButton("Close", null)
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }
}
