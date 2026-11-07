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
import com.teligati.app.adapter.ResearchProposalAdapter;
import com.teligati.app.adapter.StudentApplicantAdapter;
import com.teligati.app.auth.AuthGatewayActivity;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.entity.JobPostingEntity;
import com.teligati.app.data.entity.ResearchPaperEntity;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.ResearchProposal;
import com.teligati.app.model.StudentApplicant;
import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardFragment extends Fragment {
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher, container, false);
        repository = AppRepository.getInstance(requireContext());
        setupView(view);
        return view;
    }

    private void setupView(View view) {
        String userId = "azharul@cse.kuet.ac.bd";
        if (getArguments() != null && getArguments().getString("USER_ID") != null) {
            userId = getArguments().getString("USER_ID");
        }
        final String finalUserId = userId;

        TextView tvName = view.findViewById(R.id.tvTeacherName);
        TextView tvMeta = view.findViewById(R.id.tvTeacherMeta);
        Button btnProfile = view.findViewById(R.id.btnTeacherProfile);
        Button btnPostPaper = view.findViewById(R.id.btnTeacherPostPaper);
        Button btnPublicResearch = view.findViewById(R.id.btnTeacherPublicResearch);
        RecyclerView rvProposals = view.findViewById(R.id.rvTeacherProposals);
        RecyclerView rvStudents = view.findViewById(R.id.rvTeacherStudents);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        com.teligati.app.data.SessionManager session = com.teligati.app.data.SessionManager.getInstance(requireContext());
        String teacherName = session.getUserName();
        String teacherDept = session.getUserDept();
        boolean isVerified = session.isVerified();

        if (tvName != null) tvName.setText(teacherName + (isVerified ? " ✓ Faculty" : " • Faculty"));
        if (tvMeta != null) tvMeta.setText(teacherDept + " • " + userId);

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (getContext() == null) return;
                repository.getTeacherProfile(finalUserId, profile -> {
                    String info = "Name: " + (profile != null ? profile.getName() : "Prof. Dr. K. M. Azharul Hasan") +
                            "\nDesignation: " + (profile != null ? profile.getDesignation() : "Professor") +
                            "\nDepartment: " + (profile != null ? profile.getDepartment() : "CSE, KUET") +
                            "\nFocus: " + (profile != null ? profile.getResearchFocus() : "Cloud, Big Data, Edge AI") +
                            "\nLab: " + (profile != null ? profile.getLabName() : "Big Data Lab") +
                            "\nVerified Faculty Member.";
                    new AlertDialog.Builder(getContext())
                            .setTitle("Faculty Profile")
                            .setMessage(info)
                            .setPositiveButton("Close", null)
                            .show();
                });
            });
        }

        // Post Research Paper Call -> Saved into Room
        if (btnPostPaper != null) {
            btnPostPaper.setOnClickListener(v -> {
                if (getContext() == null) return;
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
                                ResearchPaperEntity newPaper = new ResearchPaperEntity(
                                        0,
                                        title,
                                        "Research Call (" + (conf.isEmpty() ? "IEEE Conference" : conf) + ")",
                                        "Research paper open for KUET student co-authors and research assistants.",
                                        "Python, Deep Learning, LaTeX",
                                        "Prof. Dr. K. M. Azharul Hasan",
                                        "Big Data Lab, CSE",
                                        finalUserId,
                                        1,
                                        true,
                                        "",
                                        "10.1109/KUET." + (System.currentTimeMillis() % 10000),
                                        System.currentTimeMillis()
                                );
                                repository.publishResearchPaper(newPaper, id -> {
                                    Toast.makeText(getContext(), "Research paper posted & saved to database!", Toast.LENGTH_SHORT).show();
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Post Public Research Option -> Saved into Room
        if (btnPublicResearch != null) {
            btnPublicResearch.setOnClickListener(v -> {
                if (getContext() == null) return;
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
                                JobPostingEntity newOpp = new JobPostingEntity(
                                        0,
                                        title,
                                        "RESEARCH_COLLABORATION",
                                        "Intelligent Systems Lab (KUET)",
                                        "KUET Campus / Hybrid",
                                        "Prof. Dr. K. M. Azharul Hasan",
                                        "Professor • CSE Dept",
                                        stipend.isEmpty() ? "৳12,000 / month stipend" : stipend,
                                        "6 months (Co-authorship eligible)",
                                        "Open public research collaboration for undergraduate & postgraduate students.",
                                        "Python, PyTorch, Research",
                                        3.40,
                                        false,
                                        finalUserId,
                                        System.currentTimeMillis()
                                );
                                repository.createJobPosting(newOpp, id -> {
                                    Toast.makeText(getContext(), "Public research opening published & saved to database!", Toast.LENGTH_SHORT).show();
                                });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        Button btnReferences = view.findViewById(R.id.btnTeacherReferenceRequests);
        if (btnReferences != null) {
            btnReferences.setOnClickListener(v -> showTeacherReferenceRequestsDialog());
        }

        // Proposals from Room
        if (rvProposals != null) {
            rvProposals.setLayoutManager(new LinearLayoutManager(getContext()));
            loadProposals(rvProposals);
        }

        // Students Details from Room
        if (rvStudents != null) {
            rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
            loadStudents(rvStudents);
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

    private ResearchProposalAdapter proposalAdapter;
    private StudentApplicantAdapter studentAdapter;
    private List<ResearchProposal> proposalList = new ArrayList<>();
    private List<StudentApplicant> studentList = new ArrayList<>();

    private void loadProposals(RecyclerView rv) {
        repository.getResearchProposals(list -> {
            if (!isAdded() || rv == null) return;
            proposalList = DataMapper.toResearchProposalList(list);
            proposalAdapter = new ResearchProposalAdapter(proposalList, new ResearchProposalAdapter.OnProposalActionListener() {
                @Override
                public void onAccept(ResearchProposal prop, int position) {
                    showProposalDecision(prop, "Accepted", position);
                }

                @Override
                public void onRequestRevision(ResearchProposal prop, int position) {
                    showProposalDecision(prop, "Revision Requested", position);
                }

                @Override
                public void onReject(ResearchProposal prop, int position) {
                    showProposalDecision(prop, "Rejected", position);
                }

                @Override
                public void onViewDetails(ResearchProposal prop, int position) {
                    showProposalDetailsDialog(prop);
                }
            });
            rv.setAdapter(proposalAdapter);
        });
    }

    private void showProposalDecision(ResearchProposal prop, String action, int position) {
        if (getContext() == null) return;
        final EditText etFeedback = new EditText(requireContext());
        etFeedback.setHint("Academic feedback to student...");
        if ("Accepted".equalsIgnoreCase(action)) {
            etFeedback.setText("Accepted for lab supervision. Please visit the Intelligent Systems Lab on Sunday.");
        } else if (action.toLowerCase().contains("revision")) {
            etFeedback.setText("Refine section on methodology and baseline comparisons before re-submitting.");
        } else {
            etFeedback.setText("Currently unable to supervise this topic due to lab capacity.");
        }
        etFeedback.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Mark Proposal as " + action)
                .setMessage("Proposal: \"" + prop.getTopic() + "\"\nStudent: " + prop.getStudentName() + " (Roll " + prop.getStudentRoll() + ")\n\nFeedback:")
                .setView(etFeedback)
                .setPositiveButton("Confirm " + action, (dialog, which) -> {
                    String feedback = etFeedback.getText().toString().trim();
                    repository.updateResearchProposalStatus(prop.getId(), action, feedback, res -> {
                        if (!isAdded()) return;
                        prop.setStatus(action);
                        if (proposalAdapter != null) {
                            proposalAdapter.notifyItemChanged(position);
                        }
                        Toast.makeText(requireContext(), "Proposal " + action + " & saved to database!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProposalDetailsDialog(ResearchProposal prop) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Proposal: " + prop.getTopic())
                .setMessage("Student: " + prop.getStudentName() + " (Roll " + prop.getStudentRoll() + ")\n"
                        + "Degree: " + prop.getDegreeType() + "\n"
                        + "Status: " + prop.getStatus() + "\n\n"
                        + "Abstract:\n" + prop.getProposalAbstract())
                .setPositiveButton("Close", null)
                .show();
    }

    private void loadStudents(RecyclerView rv) {
        repository.getStudentApplicants(list -> {
            if (!isAdded() || rv == null) return;
            studentList = DataMapper.toStudentApplicantList(list);
            studentAdapter = new StudentApplicantAdapter(studentList, new StudentApplicantAdapter.OnStudentResearcherListener() {
                @Override
                public void onAccept(StudentApplicant item, int position) {
                    showStudentAcceptDialog(item, position);
                }

                @Override
                public void onReject(StudentApplicant item, int position) {
                    showStudentRejectDialog(item, position);
                }

                @Override
                public void onMessage(StudentApplicant item, int position) {
                    showStudentMessageDialog(item);
                }

                @Override
                public void onViewPortfolio(StudentApplicant item, int position) {
                    showStudentPortfolioDialog(item);
                }
            });
            rv.setAdapter(studentAdapter);
        });
    }

    private void showStudentAcceptDialog(StudentApplicant item, int position) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Recruit Student for Lab")
                .setMessage("Accept " + item.getName() + " (Roll " + item.getRoll() + ", CGPA " + item.getCgpa() + ") for research collaboration in " + item.getLabInterest() + "?")
                .setPositiveButton("Accept into Lab", (dialog, which) -> {
                    repository.reviewResearchApplication(item.getId(), "ACCEPTED", "Accepted into lab for " + item.getLabInterest(), res -> {
                        if (!isAdded()) return;
                        item.setStatus("Accepted");
                        if (studentAdapter != null) {
                            studentAdapter.notifyItemChanged(position);
                        }
                        Toast.makeText(requireContext(), item.getName() + " accepted into lab and notified!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentRejectDialog(StudentApplicant item, int position) {
        if (getContext() == null) return;
        final EditText etFeedback = new EditText(requireContext());
        etFeedback.setHint("Feedback / Revision notes");
        etFeedback.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Review Decision: " + item.getName())
                .setMessage("Choose whether to decline this application or request revisions:")
                .setView(etFeedback)
                .setPositiveButton("Decline", (dialog, which) -> {
                    String feedback = etFeedback.getText().toString().trim();
                    if (feedback.isEmpty()) feedback = "Declined by faculty.";
                    repository.reviewResearchApplication(item.getId(), "REJECTED", feedback, res -> {
                        if (!isAdded()) return;
                        item.setStatus("Declined");
                        if (studentAdapter != null) {
                            studentAdapter.notifyItemChanged(position);
                        }
                        Toast.makeText(requireContext(), "Application declined.", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNeutralButton("Request Revision", (dialog, which) -> {
                    String feedback = etFeedback.getText().toString().trim();
                    if (feedback.isEmpty()) feedback = "Please provide additional details regarding your research background.";
                    repository.reviewResearchApplication(item.getId(), "REVISION_REQUESTED", feedback, res -> {
                        if (!isAdded()) return;
                        item.setStatus("Revision Requested");
                        if (studentAdapter != null) {
                            studentAdapter.notifyItemChanged(position);
                        }
                        Toast.makeText(requireContext(), "Revision requested with feedback.", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentMessageDialog(StudentApplicant item) {
        if (getContext() == null) return;
        com.teligati.app.data.SessionManager session = com.teligati.app.data.SessionManager.getInstance(requireContext());
        final EditText etMsg = new EditText(requireContext());
        etMsg.setHint("Write a message to " + item.getName());
        etMsg.setText("Regarding your research interest in " + item.getLabInterest() + ": Please come to the lab on Sunday.");
        etMsg.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Message " + item.getName())
                .setMessage("Student: " + item.getName() + " (Roll " + item.getRoll() + ")")
                .setView(etMsg)
                .setPositiveButton("Send", (dialog, which) -> {
                    String content = etMsg.getText().toString().trim();
                    if (content.isEmpty()) return;
                    repository.sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            item.getRoll(), item.getName(),
                            content,
                            msgId -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Message sent to student!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentPortfolioDialog(StudentApplicant item) {
        if (getContext() == null) return;
        String info = "Student: " + item.getName() + "\n"
                + "Department: " + item.getDepartment() + " • Roll: " + item.getRoll() + "\n"
                + "CGPA: " + item.getCgpa() + " / 4.00 (Verified)\n"
                + "Research Focus: " + item.getLabInterest() + "\n"
                + "Technical Skills: " + item.getSkills() + "\n"
                + "Status: " + item.getStatus();

        new AlertDialog.Builder(requireContext())
                .setTitle("Student Portfolio")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showTeacherReferenceRequestsDialog() {
        if (getContext() == null) return;
        repository.getAlumniReferenceRequests("ALL", "ALL", requests -> {
            if (!isAdded() || requests == null || requests.isEmpty()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Academic Reference Requests")
                        .setMessage("No incoming student reference or recommendation requests at this time.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            String[] items = new String[requests.size()];
            for (int i = 0; i < requests.size(); i++) {
                com.teligati.app.data.entity.ReferenceRequestEntity r = requests.get(i);
                items[i] = "[" + r.getStatus() + "] " + r.getStudentName() + " — " + r.getTargetCompany();
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Academic Reference Requests (" + requests.size() + ")")
                    .setItems(items, (dialog, which) -> {
                        com.teligati.app.data.entity.ReferenceRequestEntity selected = requests.get(which);
                        showReferenceDecisionDialog(selected);
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });
    }

    private void showReferenceDecisionDialog(com.teligati.app.data.entity.ReferenceRequestEntity req) {
        if (getContext() == null || req == null) return;

        String details = "Student: " + req.getStudentName() + " (" + req.getStudentRoll() + " • " + req.getStudentDept() + ")\n"
                + "Target Institution: " + req.getTargetCompany() + "\n"
                + "Purpose: " + (req.getPurpose() != null ? req.getPurpose() : "Academic Reference") + "\n"
                + "Status: " + req.getStatus() + "\n\n"
                + "Student Pitch:\n\"" + req.getMessage() + "\"";

        if (req.getRecommendationText() != null && !req.getRecommendationText().isBlank()) {
            details += "\n\nSubmitted Recommendation:\n\"" + req.getRecommendationText() + "\"";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("Academic Reference Review")
                .setMessage(details);

        String s = req.getStatus() != null ? req.getStatus().toUpperCase() : "PENDING";
        if ("ACCEPTED".equals(s) || "ENDORSED".equals(s)) {
            builder.setPositiveButton("Write Recommendation", (d, w) -> showWriteTeacherRecommendationDialog(req));
            builder.setNeutralButton("Decline", (d, w) -> {
                repository.updateReferenceRequestStatus(req.getId(), "DECLINED", success -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Request marked as DECLINED", Toast.LENGTH_SHORT).show();
                });
            });
        } else if ("PENDING".equals(s)) {
            builder.setPositiveButton("Accept & Write Letter", (d, w) -> {
                repository.updateReferenceRequestStatus(req.getId(), "ACCEPTED", success -> {
                    if (!isAdded()) return;
                    showWriteTeacherRecommendationDialog(req);
                });
            });
            builder.setNeutralButton("Decline", (d, w) -> {
                repository.updateReferenceRequestStatus(req.getId(), "DECLINED", success -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Request marked as DECLINED", Toast.LENGTH_SHORT).show();
                });
            });
        } else {
            builder.setPositiveButton("View Details", null);
        }

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showWriteTeacherRecommendationDialog(com.teligati.app.data.entity.ReferenceRequestEntity req) {
        if (getContext() == null || req == null) return;
        final EditText etLetter = new EditText(requireContext());
        etLetter.setHint("Write official academic recommendation letter...");
        etLetter.setText(req.getRecommendationText() != null ? req.getRecommendationText() :
                "To the Selection Committee: I am pleased to provide my highest recommendation for " + req.getStudentName() + ". As a student in our department at KUET, their academic rigor and dedication have been exceptional.");
        etLetter.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Academic Recommendation Letter")
                .setMessage("Submit official recommendation for " + req.getStudentName() + " to " + req.getTargetCompany() + ":")
                .setView(etLetter)
                .setPositiveButton("Submit Letter", (d, w) -> {
                    String letter = etLetter.getText().toString().trim();
                    if (letter.isEmpty()) {
                        Toast.makeText(requireContext(), "Letter cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.submitRecommendation(req.getId(), letter, "Official Academic Recommendation from KUET Faculty", success -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Academic recommendation submitted and completed!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
