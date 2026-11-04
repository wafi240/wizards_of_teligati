package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.R;
import com.teligati.app.adapter.TransactionAdapter;
import com.teligati.app.model.Opportunity;
import com.teligati.app.model.OpportunityCategory;
import com.teligati.app.model.SampleData;
import java.util.Arrays;

public class ProfileFragment extends Fragment {
    private double availableBalance = 18500.0;
    private double escrowBalance = 3500.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        RecyclerView rvTx = view.findViewById(R.id.rvTransactions);
        TextView tvAvailable = view.findViewById(R.id.tvAvailableBalance);
        TextView tvEscrow = view.findViewById(R.id.tvEscrowBalance);
        TextView tvUserName = view.findViewById(R.id.tvProfileUserName);
        TextView tvUserMeta = view.findViewById(R.id.tvProfileUserMeta);
        Button btnWithdraw = view.findViewById(R.id.btnWithdraw);
        Button btnReleaseEscrow = view.findViewById(R.id.btnReleaseEscrow);
        Button btnExportResume = view.findViewById(R.id.btnExportResume);
        Button btnPostGig = view.findViewById(R.id.btnPostGig);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        if (getArguments() != null) {
            String role = getArguments().getString("USER_ROLE", "STUDENT");
            String id = getArguments().getString("USER_ID", "2307003");
            if ("ALUMNI".equals(role)) {
                tvUserName.setText("Engr. Tanvir Ahmed ✓ Verified Alumni");
                tvUserMeta.setText("BMW Munich (Germany) • CSE 2K12 Alumni");
            } else if ("FACULTY".equals(role)) {
                tvUserName.setText("Prof. Dr. K. M. Azharul Hasan ✓ Faculty");
                tvUserMeta.setText("Professor • Big Data Lab • Dept of CSE");
            } else {
                tvUserName.setText("Samun Sadab Wafi ✓ Verified Student");
                tvUserMeta.setText("Roll " + id + " • CSE Batch 2K23");
            }
        }

        rvTx.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTx.setAdapter(new TransactionAdapter(SampleData.getTransactions()));

        btnWithdraw.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Withdraw Professional Earnings")
                    .setMessage("Available balance: ৳" + (int)availableBalance + "\nWithdraw directly to your verified Bank Account or Gateway?")
                    .setPositiveButton("Confirm Withdrawal", (d, w) -> {
                        Toast.makeText(getContext(), "Withdrawal request submitted to ledger!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Release Escrow Micro-Bounty
        btnReleaseEscrow.setOnClickListener(v -> {
            if (escrowBalance > 0) {
                availableBalance += escrowBalance;
                escrowBalance = 0;
                tvAvailable.setText("Available: ৳" + (int)availableBalance);
                tvEscrow.setText("In Escrow: ৳" + (int)escrowBalance);
                Toast.makeText(getContext(), "Escrow payment released for completed task!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No pending escrow payments.", Toast.LENGTH_SHORT).show();
            }
        });

        // 1-Page PDF Resume / Portfolio Exporter
        btnExportResume.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("📄 1-Page KUET Verified Tech Resume")
                    .setMessage("Name: Samun Sadab Wafi\nKUET Roll: 2307003 • Batch: 2K23 (CSE)\nCGPA: 3.84 / 4.00\nClubs: SGIPC Executive, KUET Career Club\nSkills: Java, Kotlin, Spring Boot, Python, ROS\nAchievements: Rank 8 Intra-KUET Programming Contest\n\n[Ready to export / Share with Alumni]")
                    .setPositiveButton("Share Resume", (d, w) -> {
                        Toast.makeText(getContext(), "Resume link generated for Alumni employers!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });

        // Post a gig dialog
        btnPostGig.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, null);
            EditText etTitle = new EditText(getContext());
            etTitle.setHint("Title (e.g. Research Assistant, Script Bugfix)");

            new AlertDialog.Builder(getContext())
                    .setTitle("Post an Opportunity / Assistant Work")
                    .setView(etTitle)
                    .setPositiveButton("Publish", (d, w) -> {
                        String title = etTitle.getText().toString().trim();
                        if (!title.isEmpty()) {
                            SampleData.getOpportunities().add(0, new Opportunity(
                                    System.currentTimeMillis(),
                                    title,
                                    OpportunityCategory.ASSISTANT_WORK,
                                    "KUET Verified Network",
                                    "Remote",
                                    "Verified Poster",
                                    "KUET Community",
                                    "৳10,000 / month",
                                    "Flexible",
                                    "Newly published opportunity on Wizards of Teligati.",
                                    Arrays.asList("Python", "Communication"),
                                    3.0
                            ));
                            Toast.makeText(getContext(), "Published! Visible under Careers tab.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AuthGatewayActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
}
