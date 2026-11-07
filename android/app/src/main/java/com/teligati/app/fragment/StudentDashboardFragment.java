package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.AlumniAdapter;
import com.teligati.app.adapter.OpportunityAdapter;
import com.teligati.app.adapter.ProjectAdapter;
import com.teligati.app.adapter.TransactionAdapter;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.repository.AppRepository;

public class StudentDashboardFragment extends Fragment {
    private double availableBalance = 18500.0;
    private double escrowBalance = 3500.0;
    private AppRepository repository;

    private TextView tvStudyTimer;
    private TextView tvStudyStatus;
    private Button btnStartStudy;
    private Button btnStopStudy;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student, container, false);
        repository = AppRepository.getInstance(requireContext());
        setupView(view);
        return view;
    }

    private void setupView(View view) {
        String userId = "2307003";
        if (getArguments() != null && getArguments().getString("USER_ID") != null) {
            userId = getArguments().getString("USER_ID");
        }
        final String finalUserId = userId;

        TextView tvName = view.findViewById(R.id.tvStudentName);
        TextView tvMeta = view.findViewById(R.id.tvStudentMeta);
        TextView tvAvailable = view.findViewById(R.id.tvAvailableBalance);
        TextView tvEscrow = view.findViewById(R.id.tvEscrowBalance);
        Button btnExportResume = view.findViewById(R.id.btnExportResume);
        Button btnWithdraw = view.findViewById(R.id.btnWithdraw);
        Button btnReleaseEscrow = view.findViewById(R.id.btnReleaseEscrow);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // Foreground Service: Study Session UI
        tvStudyTimer = view.findViewById(R.id.tvStudySessionTimer);
        tvStudyStatus = view.findViewById(R.id.tvStudySessionStatus);
        btnStartStudy = view.findViewById(R.id.btnStartStudySession);
        btnStopStudy = view.findViewById(R.id.btnStopStudySession);

        if (btnStartStudy != null) {
            btnStartStudy.setOnClickListener(v -> {
                com.teligati.app.service.StudySessionService.startSession(requireContext());
                updateStudySessionUI(true, com.teligati.app.service.StudySessionService.getElapsedSeconds());
            });
        }

        if (btnStopStudy != null) {
            btnStopStudy.setOnClickListener(v -> {
                com.teligati.app.service.StudySessionService.stopSession(requireContext());
                updateStudySessionUI(false, com.teligati.app.service.StudySessionService.getElapsedSeconds());
            });
        }

        com.teligati.app.data.SessionManager session = com.teligati.app.data.SessionManager.getInstance(requireContext());
        boolean isVerified = session.isVerified();
        String studentName = session.getUserName();
        String studentDept = session.getUserDept();

        androidx.cardview.widget.CardView cardNotice = view.findViewById(R.id.cardVerificationNotice);
        if (cardNotice != null) {
            cardNotice.setVisibility(isVerified ? View.GONE : View.VISIBLE);
        }

        if (tvName != null) {
            tvName.setText(studentName + (isVerified ? " ✓ Verified Student" : " ⏳ Verification Pending"));
        }
        if (tvMeta != null) {
            tvMeta.setText("Roll " + userId + " • " + studentDept);
        }

        // Load balance from Room first (fast display)
        repository.getUser(finalUserId, user -> {
            if (user != null) {
                availableBalance = user.getAvailableBalance();
                escrowBalance = user.getEscrowBalance();
                if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
            }
        });

        // Sync with backend demo wallet
        repository.syncWallet(wallet -> {
            if (!isAdded()) return;
            if (wallet != null) {
                availableBalance = wallet.getAvailableBalance();
                escrowBalance = wallet.getEscrowBalance();
                if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
            }
        });

        if (btnExportResume != null) {
            btnExportResume.setOnClickListener(v -> {
                if (getContext() == null) return;
                String resumeText = "🎓 1-Page KUET Verified Tech Resume\n" +
                        "Name: Samun Sadab Wafi\n" +
                        "KUET Roll: " + finalUserId + " • Batch: 2K23 (CSE)\n" +
                        "CGPA: 3.84 / 4.00\n" +
                        "Clubs: SGIPC Executive, KUET Career Club\n" +
                        "Skills: Java, Kotlin, Spring Boot, Python, ROS\n" +
                        "Achievements: Rank 8 Intra-KUET Programming Contest\n\n" +
                        "[Verified on Wizards of Teligati]";

                new AlertDialog.Builder(getContext())
                        .setTitle("📄 1-Page KUET Verified Tech Resume")
                        .setMessage(resumeText)
                        .setPositiveButton("Share via...", (d, w) -> {
                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "KUET Portfolio: Samun Sadab Wafi");
                            sendIntent.putExtra(Intent.EXTRA_TEXT, resumeText);
                            sendIntent.setType("text/plain");
                            startActivity(Intent.createChooser(sendIntent, "Share KUET Portfolio / Resume via"));
                        })
                        .setNegativeButton("Close", null)
                        .show();
            });
        }

        if (btnWithdraw != null) {
            btnWithdraw.setOnClickListener(v -> {
                if (getContext() == null) return;
                new AlertDialog.Builder(getContext())
                        .setTitle("Withdraw Demo Earnings")
                        .setMessage("DEMO WALLET — NO REAL MONEY INVOLVED\n"
                                + "DEMO MODE — All balances and transactions are simulated. No real money is transferred.\n\n"
                                + "Available simulated balance: ৳" + (int) availableBalance + "\n\n"
                                + "Simulate withdrawal of ৳1,000 to demo student bank account?")
                        .setPositiveButton("Simulate Withdrawal", (d, w) -> {
                            repository.simulateWithdraw(1000.0, "SIMULATED_STUDENT_BANK", "Demo earnings withdrawal", res -> {
                                if (!isAdded()) return;
                                if (res != null) {
                                    availableBalance = res.getAvailableBalance();
                                    escrowBalance = res.getEscrowBalance();
                                    if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                                    if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
                                    Toast.makeText(getContext(), "Simulated withdrawal successful! [Demo Only]", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "Withdrawal failed: insufficient demo balance", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnReleaseEscrow != null) {
            btnReleaseEscrow.setOnClickListener(v -> {
                if (getContext() == null) return;
                if (escrowBalance > 0) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Release Simulated Escrow")
                            .setMessage("DEMO WALLET — NO REAL MONEY INVOLVED\n"
                                    + "DEMO MODE — All balances and transactions are simulated.\n\n"
                                    + "Current locked escrow: ৳" + (int) escrowBalance + "\n\n"
                                    + "Confirm release of simulated escrow to available balance?")
                            .setPositiveButton("Confirm Release", (d, w) -> {
                                availableBalance += escrowBalance;
                                escrowBalance = 0;
                                if (tvAvailable != null) tvAvailable.setText("Available: ৳" + (int) availableBalance);
                                if (tvEscrow != null) tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
                                repository.updateUserBalance(finalUserId, availableBalance, escrowBalance, res -> {
                                    Toast.makeText(getContext(), "Simulated escrow payment released! [Demo Only]", Toast.LENGTH_SHORT).show();
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(getContext(), "No pending escrow payments in demo wallet.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Segment 1: Assistant Jobs from Room
        RecyclerView rvAssistant = view.findViewById(R.id.rvStudentAssistantJobs);
        if (rvAssistant != null) {
            rvAssistant.setLayoutManager(new LinearLayoutManager(getContext()));
            repository.getAssistantJobs(jobs -> rvAssistant.setAdapter(new OpportunityAdapter(DataMapper.toOpportunityList(jobs))));
        }

        // Segment 2: Research Papers from Room
        RecyclerView rvResearch = view.findViewById(R.id.rvStudentResearchPapers);
        if (rvResearch != null) {
            rvResearch.setLayoutManager(new LinearLayoutManager(getContext()));
            repository.getResearchPapers(papers -> rvResearch.setAdapter(new ProjectAdapter(DataMapper.toProjectItemList(papers))));
        }

        // Segment 3: Reference Opportunities from Room
        RecyclerView rvRefJobs = view.findViewById(R.id.rvStudentReferenceJobs);
        if (rvRefJobs != null) {
            rvRefJobs.setLayoutManager(new LinearLayoutManager(getContext()));
            repository.getReferenceOpportunities(jobs -> rvRefJobs.setAdapter(new OpportunityAdapter(DataMapper.toOpportunityList(jobs))));
        }

        // Segment 4: Transactions from Room
        RecyclerView rvTx = view.findViewById(R.id.rvStudentTransactions);
        if (rvTx != null) {
            rvTx.setLayoutManager(new LinearLayoutManager(getContext()));
            repository.getStudentHistory(finalUserId, txs -> rvTx.setAdapter(new TransactionAdapter(DataMapper.toTransactionRecordList(txs))));
        }

        // Segment 5: Alumni Network from Room
        RecyclerView rvAlumni = view.findViewById(R.id.rvStudentAlumni);
        if (rvAlumni != null) {
            rvAlumni.setLayoutManager(new LinearLayoutManager(getContext()));
            repository.getAlumniDirectory(list -> rvAlumni.setAdapter(new AlumniAdapter(DataMapper.toAlumniProfileList(list))));
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

    @Override
    public void onResume() {
        super.onResume();
        com.teligati.app.service.StudySessionService.setSessionListener(new com.teligati.app.service.StudySessionService.SessionListener() {
            @Override
            public void onTick(int totalSeconds) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateStudySessionUI(true, totalSeconds));
                }
            }

            @Override
            public void onSessionEnded(int totalSeconds) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateStudySessionUI(false, totalSeconds));
                }
            }
        });
        updateStudySessionUI(com.teligati.app.service.StudySessionService.isServiceRunning(),
                com.teligati.app.service.StudySessionService.getElapsedSeconds());
    }

    @Override
    public void onStop() {
        super.onStop();
        com.teligati.app.service.StudySessionService.setSessionListener(null);
    }

    private void updateStudySessionUI(boolean running, int seconds) {
        if (!isAdded()) return;
        if (tvStudyTimer != null) {
            tvStudyTimer.setText(com.teligati.app.service.StudySessionService.formatTime(seconds));
        }
        if (tvStudyStatus != null) {
            if (running) {
                tvStudyStatus.setText("Status: Active • Persistent notification in tray");
                tvStudyStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.emerald_green));
            } else {
                tvStudyStatus.setText(seconds > 0 ? "Status: Completed (" + com.teligati.app.service.StudySessionService.formatTime(seconds) + ")" : "Status: Idle (Tap Start to run persistent foreground service)");
                tvStudyStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        }
        if (btnStartStudy != null) btnStartStudy.setEnabled(!running);
        if (btnStopStudy != null) btnStopStudy.setEnabled(running);
    }
}
