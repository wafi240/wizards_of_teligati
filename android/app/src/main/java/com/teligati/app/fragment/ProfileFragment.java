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
import com.teligati.app.adapter.AlumniAdapter;
import com.teligati.app.adapter.OpportunityAdapter;
import com.teligati.app.adapter.ProjectAdapter;
import com.teligati.app.adapter.ReferenceRequestAdapter;
import com.teligati.app.adapter.ResearchProposalAdapter;
import com.teligati.app.adapter.StudentApplicantAdapter;
import com.teligati.app.adapter.TransactionAdapter;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.model.Opportunity;
import com.teligati.app.model.OpportunityCategory;
import com.teligati.app.model.ProjectItem;
import com.teligati.app.model.SampleData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {
    private double availableBalance = 18500.0;
    private double escrowBalance = 3500.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String role = "STUDENT";
        String userId = "2307003";

        if (getArguments() != null) {
            role = getArguments().getString("USER_ROLE", "STUDENT");
            userId = getArguments().getString("USER_ID", "2307003");
        }

        View view;
        if ("ALUMNI".equalsIgnoreCase(role)) {
            view = inflater.inflate(R.layout.fragment_alumni, container, false);
            setupAlumniView(view, userId);
        } else if ("FACULTY".equalsIgnoreCase(role) || "TEACHER".equalsIgnoreCase(role)) {
            view = inflater.inflate(R.layout.fragment_teacher, container, false);
            setupTeacherView(view, userId);
        } else {
            view = inflater.inflate(R.layout.fragment_student, container, false);
            setupStudentView(view, userId);
        }

        return view;
    }

    private void setupStudentView(View view, String userId) {
        TextView tvName = view.findViewById(R.id.tvStudentName);
        TextView tvMeta = view.findViewById(R.id.tvStudentMeta);
        TextView tvAvailable = view.findViewById(R.id.tvAvailableBalance);
        TextView tvEscrow = view.findViewById(R.id.tvEscrowBalance);
        Button btnExportResume = view.findViewById(R.id.btnExportResume);
        Button btnWithdraw = view.findViewById(R.id.btnWithdraw);
        Button btnReleaseEscrow = view.findViewById(R.id.btnReleaseEscrow);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        tvName.setText("Samun Sadab Wafi ✓ Verified Student");
        tvMeta.setText("Roll " + userId + " • CSE Batch 2K23");

        // Resume Exporter
        btnExportResume.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("📄 1-Page KUET Verified Tech Resume")
                    .setMessage("Name: Samun Sadab Wafi\nKUET Roll: " + userId + " • Batch: 2K23 (CSE)\nCGPA: 3.84 / 4.00\nClubs: SGIPC Executive, KUET Career Club\nSkills: Java, Kotlin, Spring Boot, Python, ROS\nAchievements: Rank 8 Intra-KUET Programming Contest\n\n[Ready to export / Share with Alumni]")
                    .setPositiveButton("Share Resume", (d, w) -> Toast.makeText(getContext(), "Resume link generated for Alumni employers!", Toast.LENGTH_SHORT).show())
                    .setNegativeButton("Close", null)
                    .show();
        });

        // Balance & Escrow
        btnWithdraw.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Withdraw Professional Earnings")
                    .setMessage("Available balance: ৳" + (int) availableBalance + "\nWithdraw directly to your verified Bank Account or Gateway?")
                    .setPositiveButton("Confirm Withdrawal", (d, w) -> Toast.makeText(getContext(), "Withdrawal request submitted to ledger!", Toast.LENGTH_SHORT).show())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnReleaseEscrow.setOnClickListener(v -> {
            if (escrowBalance > 0) {
                availableBalance += escrowBalance;
                escrowBalance = 0;
                tvAvailable.setText("Available: ৳" + (int) availableBalance);
                tvEscrow.setText("In Escrow: ৳" + (int) escrowBalance);
                Toast.makeText(getContext(), "Escrow payment released for completed task!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No pending escrow payments.", Toast.LENGTH_SHORT).show();
            }
        });

        // Segment 1: Available Jobs as Assistant
        RecyclerView rvAssistant = view.findViewById(R.id.rvStudentAssistantJobs);
        rvAssistant.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Opportunity> assistantJobs = new ArrayList<>();
        for (Opportunity opp : SampleData.getOpportunities()) {
            if (opp.getCategory() == OpportunityCategory.ASSISTANT_WORK) {
                assistantJobs.add(opp);
            }
        }
        rvAssistant.setAdapter(new OpportunityAdapter(assistantJobs));

        // Segment 2: Available Research Papers
        RecyclerView rvResearch = view.findViewById(R.id.rvStudentResearchPapers);
        rvResearch.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResearch.setAdapter(new ProjectAdapter(SampleData.getProjects()));

        // Segment 3: Jobs with Alumni Reference
        RecyclerView rvRefJobs = view.findViewById(R.id.rvStudentReferenceJobs);
        rvRefJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Opportunity> refJobs = new ArrayList<>();
        for (Opportunity opp : SampleData.getOpportunities()) {
            if (opp.getCategory() == OpportunityCategory.FULLTIME_JOBS || opp.getCategory() == OpportunityCategory.RESEARCH_COLLABORATION) {
                refJobs.add(opp);
            }
        }
        rvRefJobs.setAdapter(new OpportunityAdapter(refJobs));

        // Segment 4: Personal History (Transactions / Gigs)
        RecyclerView rvTx = view.findViewById(R.id.rvStudentTransactions);
        rvTx.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTx.setAdapter(new TransactionAdapter(SampleData.getTransactions()));

        // Segment 5: Alumni Network (Country / Company)
        RecyclerView rvAlumni = view.findViewById(R.id.rvStudentAlumni);
        rvAlumni.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAlumni.setAdapter(new AlumniAdapter(SampleData.getAlumniList()));

        setupLogout(btnLogout);
    }

    private void setupAlumniView(View view, String userId) {
        TextView tvName = view.findViewById(R.id.tvAlumniName);
        TextView tvMeta = view.findViewById(R.id.tvAlumniMeta);
        Button btnProfile = view.findViewById(R.id.btnAlumniProfile);
        Button btnPostJob = view.findViewById(R.id.btnAlumniPostJob);
        Button btnPostAssistant = view.findViewById(R.id.btnAlumniPostAssistantJob);
        RecyclerView rvRequests = view.findViewById(R.id.rvAlumniReferenceRequests);
        RecyclerView rvMyJobs = view.findViewById(R.id.rvAlumniMyJobs);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        tvName.setText("Engr. Tanvir Ahmed ✓ Verified Alumni");
        tvMeta.setText("Staff Engineer @ BMW Munich • CSE 2K12 (" + userId + ")");

        btnProfile.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Alumni Profile")
                    .setMessage("Name: Engr. Tanvir Ahmed\nDegree: B.Sc in CSE, KUET (Batch 2K12)\nCurrent Role: Staff Engineer @ BMW Group\nLocation: Munich, Germany\nKUET Mentorship: Active Mentor (24 Juniors Mentored)\nVouched: Verified by KUET Alumni Association Global")
                    .setPositiveButton("Close", null)
                    .show();
        });

        // Post Professional Job
        btnPostJob.setOnClickListener(v -> {
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
                            SampleData.getOpportunities().add(0, new Opportunity(
                                    System.currentTimeMillis(),
                                    title,
                                    OpportunityCategory.FULLTIME_JOBS,
                                    company.isEmpty() ? "BMW Munich / Remote" : company,
                                    "Munich, Germany / Remote",
                                    "Engr. Tanvir Ahmed",
                                    "Staff Engineer @ BMW (KUET Alumni)",
                                    salary.isEmpty() ? "Competitive" : salary,
                                    "Full-time Permanent",
                                    "Professional opportunity posted exclusively for KUET graduates.",
                                    Arrays.asList("Java", "Cloud", "Microservices"),
                                    3.20
                            ));
                            Toast.makeText(getContext(), "Job published! Visible under Careers & Home.", Toast.LENGTH_SHORT).show();
                            rvMyJobs.getAdapter().notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Post Paid Assistant Job with Money
        btnPostAssistant.setOnClickListener(v -> {
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
                            SampleData.getOpportunities().add(0, new Opportunity(
                                    System.currentTimeMillis(),
                                    title,
                                    OpportunityCategory.ASSISTANT_WORK,
                                    "Alumni Tech Advisory (Munich)",
                                    "Remote",
                                    "Engr. Tanvir Ahmed (CSE '12)",
                                    "Staff Engineer @ BMW (KUET Alumni)",
                                    money.isEmpty() ? "৳15,000 / month" : money,
                                    hours.isEmpty() ? "Flexible (10-12 hrs/week)" : hours,
                                    "Paid assistant role replacing tuition with engineering work.",
                                    Arrays.asList("Python", "SQL", "Git"),
                                    3.00
                            ));
                            Toast.makeText(getContext(), "Paid assistant job published with escrow guarantee!", Toast.LENGTH_SHORT).show();
                            rvMyJobs.getAdapter().notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Reference & Suggestion Requests Inbox
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRequests.setAdapter(new ReferenceRequestAdapter(SampleData.getReferenceRequests()));

        // Active Listings posted by Alumni
        rvMyJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Opportunity> alumniJobs = new ArrayList<>();
        for (Opportunity opp : SampleData.getOpportunities()) {
            if (opp.getCategory() == OpportunityCategory.ASSISTANT_WORK || opp.getCategory() == OpportunityCategory.FULLTIME_JOBS) {
                alumniJobs.add(opp);
            }
        }
        rvMyJobs.setAdapter(new OpportunityAdapter(alumniJobs));

        setupLogout(btnLogout);
    }

    private void setupTeacherView(View view, String userId) {
        TextView tvName = view.findViewById(R.id.tvTeacherName);
        TextView tvMeta = view.findViewById(R.id.tvTeacherMeta);
        Button btnProfile = view.findViewById(R.id.btnTeacherProfile);
        Button btnPostPaper = view.findViewById(R.id.btnTeacherPostPaper);
        Button btnPublicResearch = view.findViewById(R.id.btnTeacherPublicResearch);
        RecyclerView rvProposals = view.findViewById(R.id.rvTeacherProposals);
        RecyclerView rvStudents = view.findViewById(R.id.rvTeacherStudents);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        tvName.setText("Prof. Dr. K. M. Azharul Hasan ✓ Faculty");
        tvMeta.setText("Professor • Big Data & Intelligent Systems Lab (" + userId + ")");

        btnProfile.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Faculty Profile")
                    .setMessage("Name: Prof. Dr. K. M. Azharul Hasan\nDesignation: Professor & Former Head\nDepartment: Computer Science and Engineering, KUET\nResearch Focus: Cloud Computing, Big Data, Edge AI, Distributed Databases\nPublications: 100+ IEEE/ACM Journal and Conference Papers\nLab: Intelligent Systems & Big Data Lab")
                    .setPositiveButton("Close", null)
                    .show();
        });

        // Post Research Paper
        btnPostPaper.setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 20, 40, 10);

            final EditText etTitle = new EditText(getContext());
            etTitle.setHint("Paper Title (e.g. Real-Time Vision Transformer)");
            layout.addView(etTitle);

            final EditText etConference = new EditText(getContext());
            etConference.setHint("Target Venue (e.g. IEEE T-PAMI / CVPR 2027)");
            layout.addView(etConference);

            new AlertDialog.Builder(getContext())
                    .setTitle("Post Research Paper Call for Co-Authors")
                    .setView(layout)
                    .setPositiveButton("Publish Paper Call", (d, w) -> {
                        String title = etTitle.getText().toString().trim();
                        String conf = etConference.getText().toString().trim();
                        if (!title.isEmpty()) {
                            SampleData.getProjects().add(0, new ProjectItem(
                                    System.currentTimeMillis(),
                                    title,
                                    "Research Paper (" + (conf.isEmpty() ? "IEEE Conference" : conf) + ")",
                                    "Research paper open for KUET student co-authors and research assistants.",
                                    Arrays.asList("Python", "Deep Learning", "LaTeX"),
                                    "Prof. Dr. K. M. Azharul Hasan",
                                    "Big Data Lab, CSE",
                                    1,
                                    true,
                                    "",
                                    "10.1109/KUET." + System.currentTimeMillis() % 10000
                            ));
                            Toast.makeText(getContext(), "Research paper posted under Spellbook & Research tab!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Post Public Research Option
        btnPublicResearch.setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 20, 40, 10);

            final EditText etTitle = new EditText(getContext());
            etTitle.setHint("Research Topic (e.g. Distributed IoT Security)");
            layout.addView(etTitle);

            final EditText etStipend = new EditText(getContext());
            etStipend.setHint("Funded Stipend (e.g. ৳12,000 / month)");
            layout.addView(etStipend);

            new AlertDialog.Builder(getContext())
                    .setTitle("Post Public Research Opportunity")
                    .setView(layout)
                    .setPositiveButton("Publish to Students", (d, w) -> {
                        String title = etTitle.getText().toString().trim();
                        String stipend = etStipend.getText().toString().trim();
                        if (!title.isEmpty()) {
                            SampleData.getOpportunities().add(0, new Opportunity(
                                    System.currentTimeMillis(),
                                    title,
                                    OpportunityCategory.RESEARCH_COLLABORATION,
                                    "Intelligent Systems Lab (KUET)",
                                    "KUET Campus / Hybrid",
                                    "Prof. Dr. K. M. Azharul Hasan",
                                    "Professor • CSE Dept",
                                    stipend.isEmpty() ? "৳12,000 / month stipend" : stipend,
                                    "6 months (Co-authorship eligible)",
                                    "Open public research collaboration for undergraduate & postgraduate students.",
                                    Arrays.asList("Python", "PyTorch", "Research"),
                                    3.40
                            ));
                            Toast.makeText(getContext(), "Public research opportunity published for all KUET students!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Proposals RecyclerView
        rvProposals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProposals.setAdapter(new ResearchProposalAdapter(SampleData.getResearchProposals()));

        // Students Details RecyclerView
        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvStudents.setAdapter(new StudentApplicantAdapter(SampleData.getStudentApplicants()));

        setupLogout(btnLogout);
    }

    private void setupLogout(Button btnLogout) {
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AuthGatewayActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }
    }
}
