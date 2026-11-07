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
import com.teligati.app.adapter.ReferenceRequestAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.ReferenceRequest;
import java.util.ArrayList;
import java.util.List;

public class AlumniReferenceRequestsFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvRequests;
    private EditText etSearch;
    private TextView tvEmpty;
    private ReferenceRequestAdapter adapter;
    private List<ReferenceRequest> allRequests = new ArrayList<>();
    private List<ReferenceRequest> displayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alumni_reference_requests, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvRequests = view.findViewById(R.id.rvAlumniRequests);
        etSearch = view.findViewById(R.id.etSearchRequests);
        tvEmpty = view.findViewById(R.id.tvEmptyRequests);

        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReferenceRequestAdapter(displayedList, new ReferenceRequestAdapter.OnReferenceActionListener() {
            @Override
            public void onEndorse(ReferenceRequest req, int position) {
                confirmEndorsement(req, position);
            }

            @Override
            public void onDecline(ReferenceRequest req, int position) {
                confirmDecline(req, position);
            }

            @Override
            public void onGuidance(ReferenceRequest req, int position) {
                showGuidanceReplyDialog(req, position);
            }

            @Override
            public void onViewPortfolio(ReferenceRequest req, int position) {
                showStudentPortfolio(req);
            }
        });
        rvRequests.setAdapter(adapter);

        setupSearch();
        loadRequests();

        return view;
    }

    private void loadRequests() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        repository.getAlumniReferenceRequests(session.getUserId(), session.getUserName(), entities -> {
            if (!isAdded()) return;
            allRequests = DataMapper.toReferenceRequestList(entities);
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
        for (ReferenceRequest r : allRequests) {
            if (q.isEmpty()
                    || r.getStudentName().toLowerCase().contains(q)
                    || r.getTargetCompany().toLowerCase().contains(q)
                    || r.getStudentDept().toLowerCase().contains(q)
                    || r.getMessage().toLowerCase().contains(q)) {
                displayedList.add(r);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void confirmEndorsement(ReferenceRequest req, int position) {
        String s = req.getStatus() != null ? req.getStatus().toUpperCase() : "";
        if ("ACCEPTED".equals(s) || "ENDORSED".equals(s) || "COMPLETED".equals(s)) {
            showWriteRecommendationDialog(req, position);
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Accept Reference Request")
                .setMessage("Accept reference request for " + req.getStudentName() + " to " + req.getTargetCompany() + "?")
                .setPositiveButton("Accept & Write Letter", (dialog, which) -> {
                    repository.updateReferenceRequestStatus(req.getId(), "ACCEPTED", res -> {
                        if (!isAdded()) return;
                        req.setStatus("ACCEPTED");
                        adapter.notifyItemChanged(position);
                        showWriteRecommendationDialog(req, position);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showWriteRecommendationDialog(ReferenceRequest req, int position) {
        if (getContext() == null) return;
        final EditText etLetter = new EditText(requireContext());
        etLetter.setHint("Write official recommendation / referral letter...");
        etLetter.setText(req.getRecommendationText() != null ? req.getRecommendationText() :
                "I strongly recommend " + req.getStudentName() + " for " + req.getTargetCompany() + ". During their studies at KUET CSE, they demonstrated stellar technical abilities.");
        etLetter.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Recommendation Letter")
                .setMessage("Official Recommendation for " + req.getStudentName() + ":")
                .setView(etLetter)
                .setPositiveButton("Submit Letter", (d, w) -> {
                    String text = etLetter.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(requireContext(), "Recommendation text cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.submitRecommendation(req.getId(), text, "Official Recommendation by Referee", success -> {
                        if (!isAdded()) return;
                        req.setStatus("COMPLETED");
                        req.setRecommendationText(text);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Recommendation letter completed & sent!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDecline(ReferenceRequest req, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Decline Request")
                .setMessage("Decline reference request from " + req.getStudentName() + "?")
                .setPositiveButton("Decline", (dialog, which) -> {
                    repository.updateReferenceRequestStatus(req.getId(), "Declined", res -> {
                        if (!isAdded()) return;
                        req.setStatus("Declined");
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Request status updated.", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGuidanceReplyDialog(ReferenceRequest req, int position) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());

        final EditText etGuidance = new EditText(requireContext());
        etGuidance.setHint("Write career suggestions or preparation guidance...");
        etGuidance.setText("Hi " + req.getStudentName() + ", I recommend polishing your systems programming portfolio and LeetCode graph algorithms for " + req.getTargetCompany() + ".");
        etGuidance.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Guidance for " + req.getStudentName())
                .setMessage("Send mentorship advice & suggestions directly to the student:")
                .setView(etGuidance)
                .setPositiveButton("Send Guidance & Message", (dialog, which) -> {
                    String advice = etGuidance.getText().toString().trim();
                    if (advice.isEmpty()) return;

                    repository.sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            req.getStudentRoll(), req.getStudentName(),
                            "Career Guidance for " + req.getTargetCompany() + ": " + advice,
                            msgId -> {
                                if (!isAdded()) return;
                                repository.updateReferenceRequestStatus(req.getId(), "Guided", res -> {
                                    if (!isAdded()) return;
                                    req.setStatus("Guided");
                                    adapter.notifyItemChanged(position);
                                    Toast.makeText(requireContext(), "Advice sent to student and recorded in Room!", Toast.LENGTH_SHORT).show();
                                });
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentPortfolio(ReferenceRequest req) {
        if (getContext() == null) return;
        repository.getStudentProfile(req.getStudentRoll(), profile -> {
            if (!isAdded()) return;
            StringBuilder sb = new StringBuilder();
            sb.append("Student: ").append(req.getStudentName()).append("\n")
                    .append("Roll: ").append(req.getStudentRoll()).append(" • ").append(req.getStudentDept()).append("\n")
                    .append("Target: ").append(req.getTargetCompany()).append("\n\n")
                    .append("Pitch / Note:\n\"").append(req.getMessage()).append("\"\n\n");

            if (profile != null) {
                sb.append("CGPA: ").append(profile.getCgpa()).append("\n")
                        .append("Skills: ").append(profile.getSkills()).append("\n")
                        .append("Projects: ").append(profile.getProjects()).append("\n");
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Applicant Profile")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRequests();
    }
}
