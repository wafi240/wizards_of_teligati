package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
import com.teligati.app.adapter.ProjectAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.ResearchPaperEntity;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.ProjectItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeacherResearchPapersFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvPapers;
    private EditText etSearch;
    private TextView tvEmpty;
    private ProjectAdapter adapter;
    private List<ProjectItem> allPapers = new ArrayList<>();
    private List<ProjectItem> displayedPapers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_research_papers, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvPapers = view.findViewById(R.id.rvTeacherPapers);
        etSearch = view.findViewById(R.id.etSearchTeacherPapers);
        tvEmpty = view.findViewById(R.id.tvEmptyTeacherPapers);
        Button btnPost = view.findViewById(R.id.btnTeacherPostPaperCall);

        rvPapers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProjectAdapter(displayedPapers, new ProjectAdapter.OnProjectActionListener() {
            @Override
            public void onCollaborate(ProjectItem item, int position) {
                showManagePaperDialog(item, position);
            }

            @Override
            public void onViewDetails(ProjectItem item, int position) {
                showManagePaperDialog(item, position);
            }
        });
        rvPapers.setAdapter(adapter);

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> showPublishPaperDialog());
        }

        setupSearch();
        loadPapers();

        return view;
    }

    private void loadPapers() {
        if (!isAdded()) return;
        repository.getResearchPapers(entities -> {
            if (!isAdded()) return;
            allPapers = DataMapper.toProjectList(entities);
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
        displayedPapers.clear();
        String q = query.toLowerCase().trim();
        for (ProjectItem p : allPapers) {
            if (q.isEmpty()
                    || p.getName().toLowerCase().contains(q)
                    || p.getDescription().toLowerCase().contains(q)
                    || p.getOwnerName().toLowerCase().contains(q)) {
                displayedPapers.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedPapers.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showPublishPaperDialog() {
        if (getContext() == null) return;
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        final EditText etTitle = createInput("Paper Title (e.g. Distributed Consensus in Edge AI)", "");
        final EditText etAbstract = createInput("Abstract & Proposed Scope", "");
        final EditText etTech = createInput("Research Area / Keywords (e.g. Edge AI, PyTorch, ROS)", "Edge AI, Vision Transformer, Python");
        final EditText etDoi = createInput("DOI / Link (e.g. doi.org/10.1109/...)", "doi.org/10.1145/kuet.edge.2026");

        layout.addView(etTitle);
        layout.addView(etAbstract);
        layout.addView(etTech);
        layout.addView(etDoi);

        SessionManager session = SessionManager.getInstance(requireContext());

        new AlertDialog.Builder(requireContext())
                .setTitle("Publish Call for Co-Authors")
                .setView(scrollView)
                .setPositiveButton("Publish to Lab", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Paper Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ResearchPaperEntity paper = new ResearchPaperEntity(
                            0,
                            title,
                            "Call for Co-Authors",
                            etAbstract.getText().toString().trim().isEmpty() ? "Faculty research project seeking undergraduate research contributors." : etAbstract.getText().toString().trim(),
                            etTech.getText().toString().trim(),
                            session.getUserName(),
                            "Dept of CSE",
                            session.getUserId(),
                            1,
                            true,
                            "https://github.com/kuet-wizards",
                            etDoi.getText().toString().trim(),
                            System.currentTimeMillis()
                    );

                    repository.publishResearchPaper(paper, id -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Research call published and saved to Room!", Toast.LENGTH_SHORT).show();
                        loadPapers();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showManagePaperDialog(ProjectItem item, int position) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setMessage("Faculty: " + item.getOwnerName() + " (" + item.getOwnerDept() + ")\n"
                        + "Area: " + String.join(", ", item.getTechnologies()) + "\n\n"
                        + "Abstract:\n" + item.getDescription())
                .setPositiveButton("Edit Paper", (dialog, which) -> showEditPaperDialog(item))
                .setNeutralButton("View Link", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://scholar.google.com")));
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Link: https://scholar.google.com", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Delete Publication", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Publication")
                            .setMessage("Remove '" + item.getName() + "' from lab records?")
                            .setPositiveButton("Delete", (d, w) -> {
                                String callerId = SessionManager.getInstance(requireContext()).getUserId();
                                repository.deleteResearchPaper(item.getId(), callerId, success -> {
                                    if (!isAdded()) return;
                                    if (Boolean.TRUE.equals(success)) {
                                        Toast.makeText(requireContext(), "Publication removed from Room database.", Toast.LENGTH_SHORT).show();
                                        loadPapers();
                                    } else {
                                        Toast.makeText(requireContext(), "Unauthorized: You can only remove your own publications.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }

    private void showEditPaperDialog(ProjectItem item) {
        if (getContext() == null) return;
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 16);
        scrollView.addView(layout);

        final EditText etTitle = createInput("Paper Title", item.getName());
        final EditText etDesc = createInput("Abstract & Scope", item.getDescription());
        final EditText etTech = createInput("Keywords / Area", String.join(", ", item.getTechnologies()));

        layout.addView(etTitle);
        layout.addView(etDesc);
        layout.addView(etTech);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Publication Details")
                .setView(scrollView)
                .setPositiveButton("Save Updates", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) return;

                    ResearchPaperEntity entity = new ResearchPaperEntity(
                            item.getId(),
                            title,
                            item.getType(),
                            etDesc.getText().toString().trim(),
                            etTech.getText().toString().trim(),
                            item.getOwnerName(),
                            item.getOwnerDept(),
                            SessionManager.getInstance(requireContext()).getUserId(),
                            item.getContributorsCount(),
                            true,
                            "https://github.com/kuet-wizards",
                            "doi.org/10.1145/kuet.edge.2026",
                            System.currentTimeMillis()
                    );

                    repository.updateResearchPaper(entity, res -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Paper updated in Room database!", Toast.LENGTH_SHORT).show();
                        loadPapers();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        loadPapers();
    }
}
