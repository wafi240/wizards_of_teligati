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
import com.teligati.app.adapter.ResearchProposalAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.ResearchProposal;
import java.util.ArrayList;
import java.util.List;

public class TeacherProposalsFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvProposals;
    private EditText etSearch;
    private TextView tvEmpty;
    private ResearchProposalAdapter adapter;
    private List<ResearchProposal> allProposals = new ArrayList<>();
    private List<ResearchProposal> displayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_proposals, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvProposals = view.findViewById(R.id.rvProposals);
        etSearch = view.findViewById(R.id.etSearchProposals);
        tvEmpty = view.findViewById(R.id.tvEmptyProposals);

        rvProposals.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ResearchProposalAdapter(displayedList, new ResearchProposalAdapter.OnProposalActionListener() {
            @Override
            public void onAccept(ResearchProposal prop, int position) {
                showDecisionDialog(prop, "Accepted", position);
            }

            @Override
            public void onRequestRevision(ResearchProposal prop, int position) {
                showDecisionDialog(prop, "Revision Requested", position);
            }

            @Override
            public void onReject(ResearchProposal prop, int position) {
                showDecisionDialog(prop, "Rejected", position);
            }

            @Override
            public void onViewDetails(ResearchProposal prop, int position) {
                showProposalDetails(prop);
            }
        });
        rvProposals.setAdapter(adapter);

        setupSearch();
        loadProposals();

        return view;
    }

    private void loadProposals() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        repository.getTeacherProposals(session.getUserId(), session.getUserEmail(), entities -> {
            if (!isAdded()) return;
            allProposals = DataMapper.toProposalList(entities);
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
        for (ResearchProposal p : allProposals) {
            if (q.isEmpty()
                    || p.getTopic().toLowerCase().contains(q)
                    || p.getStudentName().toLowerCase().contains(q)
                    || p.getStudentRoll().toLowerCase().contains(q)
                    || p.getProposalAbstract().toLowerCase().contains(q)) {
                displayedList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showDecisionDialog(ResearchProposal prop, String action, int position) {
        if (getContext() == null) return;

        final EditText etFeedback = new EditText(requireContext());
        etFeedback.setHint("Provide academic feedback or instructions to student...");
        if ("Accepted".equals(action)) {
            etFeedback.setText("Accepted for lab supervision. Please visit the Intelligent Systems Lab on Sunday.");
        } else if ("Revision Requested".equals(action)) {
            etFeedback.setText("Refine section on methodology and baseline comparisons before re-submitting.");
        } else {
            etFeedback.setText("Currently unable to take on this topic due to lab capacity.");
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
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Proposal " + action + " and student notified via Room!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProposalDetails(ResearchProposal prop) {
        if (getContext() == null) return;
        String details = "Topic: " + prop.getTopic() + "\n"
                + "Degree Type: " + prop.getDegreeType() + "\n"
                + "Student: " + prop.getStudentName() + " (Roll " + prop.getStudentRoll() + ")\n"
                + "Status: " + prop.getStatus() + "\n\n"
                + "Proposal Abstract:\n" + prop.getProposalAbstract();

        new AlertDialog.Builder(requireContext())
                .setTitle("Research Proposal Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProposals();
    }
}
