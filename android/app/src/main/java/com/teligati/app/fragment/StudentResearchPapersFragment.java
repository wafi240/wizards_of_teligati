package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teligati.app.R;
import com.teligati.app.adapter.ProjectAdapter;
import com.teligati.app.adapter.StudentResearchProposalAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.data.repository.ProfileRepository;
import com.teligati.app.model.ProjectItem;
import com.teligati.app.model.ResearchProposal;
import com.teligati.app.network.model.PagedResponse;
import com.teligati.app.network.model.TeacherDirectoryResponse;

import java.util.ArrayList;
import java.util.List;

public class StudentResearchPapersFragment extends Fragment {

    private RecyclerView rvPapers;
    private RecyclerView rvStudentProposals;
    private Button btnSubmitProposal;
    private Button btnTabAvailablePapers;
    private Button btnTabMyProposals;
    private EditText etSearch;
    private TextView tvEmptyPapers;
    private TextView tvEmptyProposals;
    private ProgressBar pbLoading;

    // Papers Data
    private ProjectAdapter papersAdapter;
    private final List<ProjectItem> allPapers = new ArrayList<>();
    private final List<ProjectItem> displayedPapers = new ArrayList<>();

    // Proposals Data
    private StudentResearchProposalAdapter proposalsAdapter;
    private final List<ResearchProposal> allProposals = new ArrayList<>();
    private final List<ResearchProposal> displayedProposals = new ArrayList<>();

    private boolean isProposalsTabActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_research_papers, container, false);

        btnSubmitProposal = view.findViewById(R.id.btnSubmitProposal);
        btnTabAvailablePapers = view.findViewById(R.id.btnTabAvailablePapers);
        btnTabMyProposals = view.findViewById(R.id.btnTabMyProposals);
        etSearch = view.findViewById(R.id.etSearchPapers);
        pbLoading = view.findViewById(R.id.pbProposalsLoading);

        tvEmptyPapers = view.findViewById(R.id.tvEmptyPapers);
        tvEmptyProposals = view.findViewById(R.id.tvEmptyProposals);

        rvPapers = view.findViewById(R.id.rvResearchPapers);
        rvStudentProposals = view.findViewById(R.id.rvStudentProposals);

        // Setup Papers RecyclerView
        rvPapers.setLayoutManager(new LinearLayoutManager(getContext()));
        papersAdapter = new ProjectAdapter(displayedPapers, new ProjectAdapter.OnProjectActionListener() {
            @Override
            public void onCollaborate(ProjectItem item, int position) {
                showExpressInterestDialog(item, position);
            }

            @Override
            public void onViewDetails(ProjectItem item, int position) {
                showPaperDetailsDialog(item, position);
            }
        });
        rvPapers.setAdapter(papersAdapter);

        // Setup Proposals RecyclerView
        rvStudentProposals.setLayoutManager(new LinearLayoutManager(getContext()));
        proposalsAdapter = new StudentResearchProposalAdapter(displayedProposals, proposal -> showProposalDetailsDialog(proposal));
        rvStudentProposals.setAdapter(proposalsAdapter);

        // Listeners
        btnSubmitProposal.setOnClickListener(v -> showSubmitProposalDialog());
        btnTabAvailablePapers.setOnClickListener(v -> switchToPapersTab());
        btnTabMyProposals.setOnClickListener(v -> switchToProposalsTab());

        setupSearch();
        loadPapers();
        loadMyProposals();

        return view;
    }

    private void switchToPapersTab() {
        isProposalsTabActive = false;
        btnTabAvailablePapers.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00ADB5")));
        btnTabAvailablePapers.setTextColor(Color.WHITE);
        btnTabMyProposals.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141E33")));
        btnTabMyProposals.setTextColor(Color.parseColor("#94A3B8"));

        rvPapers.setVisibility(View.VISIBLE);
        rvStudentProposals.setVisibility(View.GONE);
        tvEmptyProposals.setVisibility(View.GONE);

        if (etSearch != null) {
            etSearch.setHint("Search by title, author, or research area...");
            filterPapers(etSearch.getText().toString());
        }
    }

    private void switchToProposalsTab() {
        isProposalsTabActive = true;
        btnTabMyProposals.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00ADB5")));
        btnTabMyProposals.setTextColor(Color.WHITE);
        btnTabAvailablePapers.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141E33")));
        btnTabAvailablePapers.setTextColor(Color.parseColor("#94A3B8"));

        rvPapers.setVisibility(View.GONE);
        tvEmptyPapers.setVisibility(View.GONE);
        rvStudentProposals.setVisibility(View.VISIBLE);

        if (etSearch != null) {
            etSearch.setHint("Search your proposals by title or area...");
            filterProposals(etSearch.getText().toString());
        }
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isProposalsTabActive) {
                    filterProposals(s.toString());
                } else {
                    filterPapers(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadPapers() {
        if (!isAdded()) return;
        AppRepository.getInstance(requireContext()).getResearchPapers(entities -> {
            if (!isAdded()) return;
            allPapers.clear();
            allPapers.addAll(DataMapper.toProjectList(entities));
            if (!isProposalsTabActive) {
                filterPapers(etSearch != null ? etSearch.getText().toString() : "");
            }
        });
    }

    private void loadMyProposals() {
        if (!isAdded()) return;
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);

        SessionManager session = SessionManager.getInstance(requireContext());
        String roll = session.getUserId();
        String email = session.getUserEmail();

        AppRepository.getInstance(requireContext()).getMyResearchProposals(roll, email, entities -> {
            if (!isAdded()) return;
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);

            allProposals.clear();
            allProposals.addAll(DataMapper.toProposalList(entities));
            if (isProposalsTabActive) {
                filterProposals(etSearch != null ? etSearch.getText().toString() : "");
            }
        });
    }

    private void filterPapers(String query) {
        displayedPapers.clear();
        String q = query.toLowerCase().trim();
        for (ProjectItem p : allPapers) {
            boolean matchesTech = false;
            for (String t : p.getTechnologies()) {
                if (t.toLowerCase().contains(q)) {
                    matchesTech = true;
                    break;
                }
            }
            if (q.isEmpty()
                    || p.getName().toLowerCase().contains(q)
                    || p.getOwnerName().toLowerCase().contains(q)
                    || p.getOwnerDept().toLowerCase().contains(q)
                    || p.getDescription().toLowerCase().contains(q)
                    || matchesTech) {
                displayedPapers.add(p);
            }
        }
        papersAdapter.notifyDataSetChanged();
        if (tvEmptyPapers != null) {
            tvEmptyPapers.setVisibility(displayedPapers.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void filterProposals(String query) {
        displayedProposals.clear();
        String q = query.toLowerCase().trim();
        for (ResearchProposal p : allProposals) {
            if (q.isEmpty()
                    || p.getTopic().toLowerCase().contains(q)
                    || (p.getDegreeType() != null && p.getDegreeType().toLowerCase().contains(q))
                    || (p.getProposalAbstract() != null && p.getProposalAbstract().toLowerCase().contains(q))
                    || (p.getStatus() != null && p.getStatus().toLowerCase().contains(q))) {
                displayedProposals.add(p);
            }
        }
        proposalsAdapter.notifyDataSetChanged();
        if (tvEmptyProposals != null) {
            tvEmptyProposals.setVisibility(displayedProposals.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private static class FacultyItem {
        final long id;
        final String name;
        final String title;
        final String uid;

        FacultyItem(long id, String name, String title, String uid) {
            this.id = id;
            this.name = name;
            this.title = title;
            this.uid = uid;
        }

        FacultyItem(long id, String name, String title) {
            this(id, name, title, null);
        }

        @Override
        public String toString() {
            return name + (title != null && !title.isEmpty() ? " (" + title + ")" : "");
        }
    }

    private void showSubmitProposalDialog() {
        if (getContext() == null) return;

        SessionManager session = SessionManager.getInstance(requireContext());
        if (!session.isVerified()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⏳ Verification Required")
                    .setMessage("Your student account is pending institutional verification. Research proposal submissions require a verified academic account.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        // 1. Research Title
        TextView tvTitleLabel = createLabel("Research Proposal Title *");
        final EditText etTopic = createInput("e.g. Distributed Consensus in Edge AI", "");
        layout.addView(tvTitleLabel);
        layout.addView(etTopic);

        // 2. Research Area / Degree Type
        TextView tvAreaLabel = createLabel("Research Area / Degree Type *");
        final EditText etDegree = createInput("e.g. Undergraduate Thesis, Edge AI, Distributed Systems", "Undergraduate Thesis");
        layout.addView(tvAreaLabel);
        layout.addView(etDegree);

        // 3. Preferred Faculty Supervisor
        TextView tvSupervisorLabel = createLabel("Preferred Faculty Supervisor *");
        layout.addView(tvSupervisorLabel);

        final Spinner spnFaculty = new Spinner(requireContext());
        spnFaculty.setBackgroundResource(R.drawable.bg_cyber_input);
        spnFaculty.setPadding(24, 18, 24, 18);

        final List<FacultyItem> facultyList = new ArrayList<>();
        // Seed default fallback supervisor Dr. Azharul (UID: faculty_1, ID: 3)
        facultyList.add(new FacultyItem(3L, "Dr. Azharul (Faculty)", "Professor & Head, CSE", "faculty_1"));

        final ArrayAdapter<FacultyItem> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, facultyList);
        spnFaculty.setAdapter(spinnerAdapter);
        layout.addView(spnFaculty);

        // Asynchronously load real teacher directory to expand spinner if online
        try {
            ProfileRepository.getInstance(requireContext()).getTeacherDirectory(
                    "", "", null, null, 0, 50,
                    new ProfileRepository.Callback<PagedResponse<TeacherDirectoryResponse>>() {
                        @Override
                        public void onSuccess(PagedResponse<TeacherDirectoryResponse> result) {
                            if (!isAdded() || result == null || result.getContent() == null) return;
                            boolean updated = false;
                            for (TeacherDirectoryResponse t : result.getContent()) {
                                if (t.getUserId() != null && t.getUserId() != 3L) {
                                    facultyList.add(new FacultyItem(t.getUserId(), t.getName(), t.getDesignation(), t.getUid()));
                                    updated = true;
                                }
                            }
                            if (updated) {
                                spinnerAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Fallback already pre-seeded
                        }
                    }
            );
        } catch (Exception ignored) {}

        // 4. Proposal Description / Abstract
        TextView tvAbstractLabel = createLabel("Research Abstract & Proposed Scope *");
        final EditText etAbstract = createInput("Describe your research hypothesis, proposed methodology, baseline experiments, and required resources...", "");
        etAbstract.setMinLines(4);
        etAbstract.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        layout.addView(tvAbstractLabel);
        layout.addView(etAbstract);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Submit Research Proposal")
                .setView(scrollView)
                .setPositiveButton("Submit Proposal", null) // Overridden below to validate before closing
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSubmit.setOnClickListener(v -> {
                String topic = etTopic.getText().toString().trim();
                String degree = etDegree.getText().toString().trim();
                String abstractText = etAbstract.getText().toString().trim();

                if (topic.isEmpty()) {
                    etTopic.setError("Research title is required");
                    etTopic.requestFocus();
                    return;
                }
                if (degree.isEmpty()) {
                    etDegree.setError("Research area is required");
                    etDegree.requestFocus();
                    return;
                }
                if (abstractText.isEmpty()) {
                    etAbstract.setError("Proposal abstract is required");
                    etAbstract.requestFocus();
                    return;
                }

                FacultyItem selectedFaculty = (FacultyItem) spnFaculty.getSelectedItem();
                String targetFacultyUid = (selectedFaculty != null && selectedFaculty.uid != null && !selectedFaculty.uid.isEmpty())
                        ? selectedFaculty.uid
                        : String.valueOf(selectedFaculty != null ? selectedFaculty.id : 3L);

                btnSubmit.setEnabled(false);
                btnSubmit.setText("Submitting...");

                String studentRoll = session.getUserId();
                String studentName = session.getUserName();

                AppRepository.getInstance(requireContext()).submitResearchProposal(
                        studentRoll,
                        studentName,
                        topic,
                        abstractText,
                        degree,
                        targetFacultyUid,
                        new AppRepository.ProposalSubmissionCallback() {
                            @Override
                            public void onSuccess() {
                                if (!isAdded()) return;
                                dialog.dismiss();
                                Toast.makeText(requireContext(), "✅ Research proposal submitted successfully to faculty!", Toast.LENGTH_LONG).show();
                                switchToProposalsTab();
                                loadMyProposals();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                if (!isAdded()) return;
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Submit Proposal");
                                Toast.makeText(requireContext(), "Submission failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                );
            });
        });

        dialog.show();
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#00ADB5")); // Mystic cyan
        tv.setTextSize(12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(4, 12, 4, 4);
        return tv;
    }

    private EditText createInput(String hint, String value) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        if (value != null && !value.isEmpty()) et.setText(value);
        et.setBackgroundResource(R.drawable.bg_cyber_input);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.parseColor("#94A3B8"));
        et.setTextSize(13);
        et.setPadding(24, 20, 24, 20);
        return et;
    }

    private void showProposalDetailsDialog(ResearchProposal proposal) {
        if (getContext() == null || proposal == null) return;
        String facultyInfo = (proposal.getFacultyName() != null && !proposal.getFacultyName().isEmpty())
                ? "\nFaculty Supervisor: " + proposal.getFacultyName()
                : "";
        String feedbackInfo = (proposal.getFeedback() != null && !proposal.getFeedback().isEmpty() && !"Under review".equalsIgnoreCase(proposal.getFeedback().trim()))
                ? "\n\n💬 Advisor Feedback:\n" + proposal.getFeedback()
                : "";

        new AlertDialog.Builder(requireContext())
                .setTitle(proposal.getTopic())
                .setMessage("Research Area: " + proposal.getDegreeType() + "\n"
                        + "Submission Date: " + proposal.getFormattedDate() + "\n"
                        + "Status: " + proposal.getStatus()
                        + facultyInfo + "\n\n"
                        + "Abstract & Scope:\n" + proposal.getProposalAbstract()
                        + feedbackInfo)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showPaperDetailsDialog(ProjectItem item, int position) {
        if (getContext() == null) return;
        String message = "Faculty / Lab: " + item.getOwnerName() + " (" + item.getOwnerDept() + ")\n"
                + "Category: " + item.getType() + "\n"
                + "Area / Technologies: " + String.join(", ", item.getTechnologies()) + "\n"
                + "Collaborators: " + item.getContributorsCount() + "\n\n"
                + "Abstract & Scope:\n" + item.getDescription();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setMessage(message)
                .setNeutralButton("View Document / Link", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://scholar.google.com"));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "DOI link: https://scholar.google.com", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("Express Interest", (dialog, which) -> showExpressInterestDialog(item, position))
                .setNegativeButton("Close", null);

        builder.show();
    }

    private void showExpressInterestDialog(ProjectItem item, int position) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        if (!session.isVerified()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⏳ Verification Required")
                    .setMessage("Your account is pending institutional verification. Research collaboration and proposal submissions are restricted until verified.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        AppRepository repo = AppRepository.getInstance(requireContext());
        String roll = session.getUserId();

        repo.checkResearchApplicationExists(item.getId(), roll, alreadyInterested -> {
            if (!isAdded()) return;
            if (alreadyInterested) {
                Toast.makeText(requireContext(), "You have already expressed interest for this paper.", Toast.LENGTH_LONG).show();
                return;
            }

            final EditText etPitch = new EditText(requireContext());
            etPitch.setHint("State your research background & relevant skills");
            etPitch.setText("Interested in co-authoring and experimental benchmarks. Experienced in Python, PyTorch, and systems programming.");
            etPitch.setPadding(32, 24, 32, 24);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Collaborate on " + item.getName())
                    .setMessage("Send interest to " + item.getOwnerName() + " (" + item.getOwnerDept() + "):\nApplicant: " + session.getUserName() + " (Roll " + roll + ", Dept " + session.getUserDept() + ")")
                    .setView(etPitch)
                    .setPositiveButton("Submit Statement", (dialog, which) -> {
                        String note = etPitch.getText().toString().trim();
                        if (note.isEmpty()) note = "Interested in lab collaboration.";
                        repo.expressInterestInResearch(
                                item.getId(), item.getName(), roll, session.getUserName(), session.getUserDept(),
                                session.getUserCgpa(), "Research, Benchmarks, Writing", note,
                                success -> {
                                    if (!isAdded()) return;
                                    if (success) {
                                        Toast.makeText(requireContext(), "Research collaboration interest registered in Room!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Could not submit: already registered.", Toast.LENGTH_SHORT).show();
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
        loadPapers();
        loadMyProposals();
    }
}
