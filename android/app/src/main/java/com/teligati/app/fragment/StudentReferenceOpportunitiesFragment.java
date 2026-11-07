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
import com.teligati.app.adapter.OpportunityAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.Opportunity;
import java.util.ArrayList;
import java.util.List;

public class StudentReferenceOpportunitiesFragment extends Fragment {
    private RecyclerView rvJobs;
    private EditText etSearch;
    private TextView tvEmpty;
    private OpportunityAdapter adapter;
    private List<Opportunity> allOpportunities = new ArrayList<>();
    private List<Opportunity> displayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_reference_opportunities, container, false);

        rvJobs = view.findViewById(R.id.rvReferenceJobs);
        etSearch = view.findViewById(R.id.etSearchReferences);
        tvEmpty = view.findViewById(R.id.tvEmptyReferences);

        rvJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OpportunityAdapter(displayedList, new OpportunityAdapter.OnOpportunityActionListener() {
            @Override
            public void onApply(Opportunity opp, int position) {
                showReferenceRequestDialog(opp);
            }

            @Override
            public void onViewDetails(Opportunity opp, int position) {
                showDetailsDialog(opp);
            }
        });
        rvJobs.setAdapter(adapter);

        setupSearch();
        loadData();

        return view;
    }

    private void loadData() {
        if (!isAdded()) return;
        AppRepository.getInstance(requireContext()).getReferenceOpportunities(entities -> {
            if (!isAdded()) return;
            allOpportunities = DataMapper.toOpportunityList(entities);
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
        for (Opportunity opp : allOpportunities) {
            if (q.isEmpty()
                    || opp.getTitle().toLowerCase().contains(q)
                    || opp.getOrganization().toLowerCase().contains(q)
                    || opp.getPostedByName().toLowerCase().contains(q)
                    || opp.getDescription().toLowerCase().contains(q)) {
                displayedList.add(opp);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showDetailsDialog(Opportunity opp) {
        if (getContext() == null) return;
        String msg = "Company: " + opp.getOrganization() + "\n"
                + "Location: " + opp.getLocation() + "\n"
                + "Alumni Referee: " + opp.getPostedByName() + "\n"
                + "Compensation: " + opp.getCompensation() + "\n\n"
                + "Role & Referral Overview:\n" + opp.getDescription();

        new AlertDialog.Builder(requireContext())
                .setTitle(opp.getTitle())
                .setMessage(msg)
                .setPositiveButton("Request Referral", (dialog, which) -> showReferenceRequestDialog(opp))
                .setNeutralButton("Message Alumni", (dialog, which) -> showDirectMessageDialog(opp))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showReferenceRequestDialog(Opportunity opp) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        if (!session.isVerified()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⏳ Verification Required")
                    .setMessage("Your account is pending institutional email verification (pending external SMTP service deployment). Requesting alumni references is restricted until verified.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        AppRepository repo = AppRepository.getInstance(requireContext());

        final EditText etPitch = new EditText(requireContext());
        etPitch.setHint("State why you are qualified for " + opp.getOrganization());
        etPitch.setText("Hello " + opp.getPostedByName() + ", I am graduating from KUET CSE. I would be grateful for your internal referral and guidance for this opening.");
        etPitch.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Submit Reference Request")
                .setMessage("Target: " + opp.getOrganization() + " (" + opp.getTitle() + ")\nAlumni: " + opp.getPostedByName() + "\nYour Pitch:")
                .setView(etPitch)
                .setPositiveButton("Submit Request", (dialog, which) -> {
                    String note = etPitch.getText().toString().trim();
                    if (note.isEmpty()) note = "Reference request submitted.";

                    repo.submitReferenceRequest(
                            session.getUserId(), session.getUserName(), session.getUserDept(),
                            opp.getOrganization(), note, opp.getPostedByUserId(),
                            res -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Reference request submitted to " + opp.getPostedByName() + "!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDirectMessageDialog(Opportunity opp) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        AppRepository repo = AppRepository.getInstance(requireContext());

        final EditText etMessage = new EditText(requireContext());
        etMessage.setHint("Write your inquiry to " + opp.getPostedByName());
        etMessage.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Message " + opp.getPostedByName())
                .setMessage("Start a professional inquiry conversation:")
                .setView(etMessage)
                .setPositiveButton("Send Message", (dialog, which) -> {
                    String content = etMessage.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            opp.getPostedByUserId(), opp.getPostedByName(),
                            content,
                            msgId -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Message sent to " + opp.getPostedByName() + "!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
