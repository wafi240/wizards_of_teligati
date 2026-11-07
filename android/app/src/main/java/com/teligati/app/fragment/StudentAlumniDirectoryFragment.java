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
import com.teligati.app.adapter.AlumniAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.AlumniProfile;
import java.util.ArrayList;
import java.util.List;

public class StudentAlumniDirectoryFragment extends Fragment {
    private RecyclerView rvDirectory;
    private EditText etSearch;
    private TextView tvEmpty;
    private AlumniAdapter adapter;
    private List<AlumniProfile> allAlumni = new ArrayList<>();
    private List<AlumniProfile> displayedList = new ArrayList<>();
    private int currentLoadVersion = 0;
    private boolean isRemoteLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_alumni_directory, container, false);

        rvDirectory = view.findViewById(R.id.rvAlumniDirectory);
        etSearch = view.findViewById(R.id.etSearchAlumni);
        tvEmpty = view.findViewById(R.id.tvEmptyAlumni);

        rvDirectory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlumniAdapter(displayedList, new AlumniAdapter.OnAlumniActionListener() {
            @Override
            public void onGuidance(AlumniProfile a, int position) {
                showGuidanceMessageDialog(a);
            }

            @Override
            public void onViewDetails(AlumniProfile a, int position) {
                showAlumniDetailsDialog(a);
            }
        });
        rvDirectory.setAdapter(adapter);

        setupSearch();
        loadAlumni();

        return view;
    }

    private void loadAlumni() {
        if (!isAdded()) return;

        final int version = ++currentLoadVersion;

        // 1. Immediate local Room database rendering (if remote has not yet loaded)
        if (!isRemoteLoaded) {
            AppRepository.getInstance(requireContext()).getAlumniDirectory(entities -> {
                if (!isAdded() || isRemoteLoaded || version != currentLoadVersion) return;
                allAlumni = DataMapper.toAlumniList(entities);
                filter(etSearch != null ? etSearch.getText().toString() : "");
            });
        }

        // 2. Authoritative remote directory synchronization via ProfileRepository (Firestore source of truth)
        SessionManager session = SessionManager.getInstance(requireContext());
        if (session.isLoggedIn()) {
            com.teligati.app.data.repository.ProfileRepository.getInstance(requireContext())
                    .getAlumniDirectory(null, null, null, null, null, null, 0, 50,
                            new com.teligati.app.data.repository.ProfileRepository.Callback<com.teligati.app.network.model.PagedResponse<com.teligati.app.network.model.AlumniDirectoryResponse>>() {
                                @Override
                                public void onSuccess(com.teligati.app.network.model.PagedResponse<com.teligati.app.network.model.AlumniDirectoryResponse> result) {
                                    if (!isAdded() || version != currentLoadVersion || result == null || result.getContent() == null) return;
                                    isRemoteLoaded = true;
                                    allAlumni = DataMapper.fromDirectoryResponseList(result.getContent());
                                    filter(etSearch != null ? etSearch.getText().toString() : "");
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // Fail silently to local Room cache
                                }
                            });
        }
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
        String q = query != null ? query.toLowerCase().trim() : "";
        for (AlumniProfile a : allAlumni) {
            if (a == null) continue;
            if (q.isEmpty()
                    || (a.getName() != null && a.getName().toLowerCase().contains(q))
                    || (a.getCurrentCompany() != null && a.getCurrentCompany().toLowerCase().contains(q))
                    || (a.getCurrentPosition() != null && a.getCurrentPosition().toLowerCase().contains(q))
                    || (a.getCountry() != null && a.getCountry().toLowerCase().contains(q))
                    || (a.getCity() != null && a.getCity().toLowerCase().contains(q))
                    || (a.getDepartment() != null && a.getDepartment().toLowerCase().contains(q))
                    || (a.getBio() != null && a.getBio().toLowerCase().contains(q))) {
                displayedList.add(a);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showAlumniDetailsDialog(AlumniProfile a) {
        if (getContext() == null || a == null) return;
        String position = a.getCurrentPosition() != null ? a.getCurrentPosition() : "Alumni";
        String company = a.getCurrentCompany() != null ? a.getCurrentCompany() : "N/A";
        String location = (a.getCity() != null ? a.getCity() : "") + (a.getCountry() != null ? (", " + a.getCountry()) : "");
        String dept = a.getDepartment() != null ? a.getDepartment() : "KUET";
        String batch = a.getBatch() != null ? (" • " + a.getBatch()) : "";
        String bio = a.getBio() != null ? a.getBio() : "No bio available.";

        String details = "Position: " + position + "\n"
                + "Company: " + company + "\n"
                + "Location: " + location + "\n"
                + "KUET: " + dept + batch + "\n\n"
                + "Professional Bio & Mentorship:\n" + bio;

        new AlertDialog.Builder(requireContext())
                .setTitle(a.getName() + " (KUET Alumni)")
                .setMessage(details)
                .setPositiveButton("Message / Guidance", (dialog, which) -> showGuidanceMessageDialog(a))
                .setNeutralButton("Request Referral", (dialog, which) -> showReferenceRequestDialog(a))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showReferenceRequestDialog(AlumniProfile a) {
        if (getContext() == null || a == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        if (!session.isVerified()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⏳ Verification Required")
                    .setMessage("Your account is pending institutional email verification. Requesting alumni referrals is restricted until verified.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        final EditText etPitch = new EditText(requireContext());
        etPitch.setHint("State why you are qualified for " + (a.getCurrentCompany() != null ? a.getCurrentCompany() : "their company"));
        etPitch.setText("Hello " + a.getName() + ", I am graduating from KUET CSE. I would be grateful for your internal referral and professional endorsement for opportunities at " + a.getCurrentCompany() + ".");
        etPitch.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Request Referral from " + a.getName())
                .setMessage("Target Company: " + (a.getCurrentCompany() != null ? a.getCurrentCompany() : "KUET Alumni Network") + "\nYour Pitch:")
                .setView(etPitch)
                .setPositiveButton("Submit Request", (dialog, which) -> {
                    String note = etPitch.getText().toString().trim();
                    if (note.isEmpty()) note = "Reference request submitted.";

                    AppRepository.getInstance(requireContext()).submitReferenceRequest(
                            session.getUserId(), session.getUserName(), session.getUserDept(),
                            a.getCurrentCompany() != null ? a.getCurrentCompany() : "KUET Alumni Network",
                            note, String.valueOf(a.getId()),
                            res -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Referral request submitted to " + a.getName() + "!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGuidanceMessageDialog(AlumniProfile a) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        AppRepository repo = AppRepository.getInstance(requireContext());

        final EditText etMsg = new EditText(requireContext());
        etMsg.setHint("Write guidance inquiry or message...");
        etMsg.setText("Hello " + a.getName() + ", I am a KUET CSE student interested in your work at " + a.getCurrentCompany() + ". Could I seek some career guidance?");
        etMsg.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Message " + a.getName())
                .setMessage("Start a mentorship conversation with " + a.getName() + " (" + a.getCurrentCompany() + "):")
                .setView(etMsg)
                .setPositiveButton("Send Message", (dialog, which) -> {
                    String content = etMsg.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            String.valueOf(a.getId()), a.getName(),
                            content,
                            msgId -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Mentorship message sent and saved in Room!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAlumni();
    }
}
