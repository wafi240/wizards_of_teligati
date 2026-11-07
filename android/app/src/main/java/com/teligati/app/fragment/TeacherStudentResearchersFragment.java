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
import com.teligati.app.adapter.StudentApplicantAdapter;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.StudentApplicant;
import java.util.ArrayList;
import java.util.List;

public class TeacherStudentResearchersFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvStudents;
    private EditText etSearch;
    private TextView tvEmpty;
    private StudentApplicantAdapter adapter;
    private List<StudentApplicant> allStudents = new ArrayList<>();
    private List<StudentApplicant> displayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_student_researchers, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvStudents = view.findViewById(R.id.rvTeacherStudents);
        etSearch = view.findViewById(R.id.etSearchResearchers);
        tvEmpty = view.findViewById(R.id.tvEmptyResearchers);

        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentApplicantAdapter(displayedList, new StudentApplicantAdapter.OnStudentResearcherListener() {
            @Override
            public void onAccept(StudentApplicant item, int position) {
                confirmAccept(item, position);
            }

            @Override
            public void onReject(StudentApplicant item, int position) {
                confirmReject(item, position);
            }

            @Override
            public void onMessage(StudentApplicant item, int position) {
                showSendMessageDialog(item);
            }

            @Override
            public void onViewPortfolio(StudentApplicant item, int position) {
                showPortfolioDialog(item);
            }
        });
        rvStudents.setAdapter(adapter);

        setupSearch();
        loadApplicants();

        return view;
    }

    private void loadApplicants() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        repository.getTeacherStudentApplicants(session.getUserId(), session.getUserName(), entities -> {
            if (!isAdded()) return;
            allStudents = DataMapper.toStudentApplicantList(entities);
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
        String q = query != null ? query.toLowerCase().trim() : "";
        for (StudentApplicant s : allStudents) {
            if (s == null) continue;
            if (q.isEmpty()
                    || (s.getName() != null && s.getName().toLowerCase().contains(q))
                    || (s.getRoll() != null && s.getRoll().toLowerCase().contains(q))
                    || (s.getLabInterest() != null && s.getLabInterest().toLowerCase().contains(q))
                    || (s.getSkills() != null && s.getSkills().toLowerCase().contains(q))) {
                displayedList.add(s);
            }
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) {
            tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void confirmAccept(StudentApplicant item, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Recruit Student for Lab")
                .setMessage("Accept " + item.getName() + " (Roll " + item.getRoll() + ", CGPA " + item.getCgpa() + ") for research collaboration in " + item.getLabInterest() + "?")
                .setPositiveButton("Accept into Lab", (dialog, which) -> {
                    repository.reviewResearchApplication(item.getId(), "ACCEPTED", "Accepted into lab for " + item.getLabInterest(), res -> {
                        if (!isAdded()) return;
                        item.setStatus("Accepted");
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), item.getName() + " accepted into lab and notified!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReject(StudentApplicant item, int position) {
        final EditText etFeedback = new EditText(requireContext());
        etFeedback.setHint("Feedback / Revision notes (required for revision)");
        etFeedback.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(requireContext())
                .setTitle("Review Decision: " + item.getName())
                .setMessage("Choose whether to decline this application or request revisions from the student.")
                .setView(etFeedback)
                .setPositiveButton("Request Revision", (dialog, which) -> {
                    String feedback = etFeedback.getText().toString().trim();
                    if (feedback.isEmpty()) {
                        feedback = "Please provide additional details regarding your research background.";
                    }
                    final String finalFeedback = feedback;
                    repository.reviewResearchApplication(item.getId(), "REVISION_REQUESTED", finalFeedback, res -> {
                        if (!isAdded()) return;
                        item.setStatus("Revision Requested");
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Revision requested with feedback.", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNeutralButton("Decline", (dialog, which) -> {
                    String feedback = etFeedback.getText().toString().trim();
                    if (feedback.isEmpty()) feedback = "Declined by faculty.";
                    repository.reviewResearchApplication(item.getId(), "REJECTED", feedback, res -> {
                        if (!isAdded()) return;
                        item.setStatus("Declined");
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Application declined.", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSendMessageDialog(StudentApplicant item) {
        if (getContext() == null) return;
        SessionManager session = SessionManager.getInstance(requireContext());

        final EditText etMsg = new EditText(requireContext());
        etMsg.setHint("Write a message to " + item.getName());
        etMsg.setText("Regarding your research interest in " + item.getLabInterest() + ": Please come to the Intelligent Systems Lab on Sunday.");
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
                                Toast.makeText(requireContext(), "Message sent to student and recorded in Room!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPortfolioDialog(StudentApplicant item) {
        if (getContext() == null) return;
        repository.getStudentProfile(item.getRoll(), profile -> {
            if (!isAdded()) return;
            StringBuilder sb = new StringBuilder();
            sb.append("Student: ").append(item.getName()).append("\n")
                    .append("Roll: ").append(item.getRoll()).append(" • ").append(item.getDepartment()).append("\n")
                    .append("CGPA: ").append(item.getCgpa()).append("\n\n")
                    .append("Research Interest: ").append(item.getLabInterest()).append("\n")
                    .append("Skills: ").append(item.getSkills()).append("\n\n");

            if (profile != null) {
                if (profile.getSpecialization() != null) sb.append("Specialization: ").append(profile.getSpecialization()).append("\n");
                if (profile.getProjects() != null) sb.append("Projects: ").append(profile.getProjects()).append("\n");
                if (profile.getGithubUrl() != null) sb.append("GitHub: ").append(profile.getGithubUrl()).append("\n");
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Verified Student Researcher Profile")
                    .setMessage(sb.toString().trim())
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadApplicants();
    }
}
