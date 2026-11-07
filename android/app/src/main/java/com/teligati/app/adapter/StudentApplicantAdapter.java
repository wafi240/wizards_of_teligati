package com.teligati.app.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.StudentApplicant;
import java.util.List;

public class StudentApplicantAdapter extends RecyclerView.Adapter<StudentApplicantAdapter.ViewHolder> {
    public interface OnStudentResearcherListener {
        void onAccept(StudentApplicant item, int position);
        void onReject(StudentApplicant item, int position);
        void onMessage(StudentApplicant item, int position);
        void onViewPortfolio(StudentApplicant item, int position);
    }

    private final List<StudentApplicant> list;
    private OnStudentResearcherListener listener;

    public StudentApplicantAdapter(List<StudentApplicant> list) {
        this.list = list;
    }

    public StudentApplicantAdapter(List<StudentApplicant> list, OnStudentResearcherListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setListener(OnStudentResearcherListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_applicant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentApplicant item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDeptRoll.setText(item.getDepartment() + " • Roll " + item.getRoll());
        holder.tvCgpa.setText("CGPA: " + item.getCgpa());
        holder.tvInterest.setText("🎯 Focus: " + item.getLabInterest());
        holder.tvSkills.setText("Skills: " + item.getSkills());

        String s = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
        if (s.contains("accept")) {
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accepted ✓");
            holder.btnReject.setEnabled(false);
            holder.btnReject.setText("Decline");
        } else if (s.contains("decline") || s.contains("reject")) {
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accept Lab");
            holder.btnReject.setEnabled(false);
            holder.btnReject.setText("Declined ✗");
        } else if (s.contains("revision")) {
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept Lab");
            holder.btnReject.setEnabled(false);
            holder.btnReject.setText("Revision ⏳");
        } else {
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept Lab");
            holder.btnReject.setEnabled(true);
            holder.btnReject.setText("Decline");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewPortfolio(item, position);
            } else {
                showDefaultPortfolioDialog(v.getContext(), item);
            }
        });

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(item, position);
            } else {
                showDefaultAcceptDialog(v.getContext(), item, position, holder);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(item, position);
            } else {
                showDefaultRejectDialog(v.getContext(), item, position, holder);
            }
        });

        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessage(item, position);
            } else {
                showDefaultMessageDialog(v.getContext(), item);
            }
        });
    }

    private void showDefaultAcceptDialog(Context context, StudentApplicant item, int position, ViewHolder holder) {
        new AlertDialog.Builder(context)
                .setTitle("Recruit " + item.getName())
                .setMessage("Recruit " + item.getName() + " (CGPA " + item.getCgpa() + ") for research collaboration in " + item.getLabInterest() + "?")
                .setPositiveButton("Accept into Lab", (d, w) -> {
                    holder.btnAccept.setEnabled(false);
                    holder.btnReject.setEnabled(false);
                    item.setStatus("Accepted");
                    AppRepository.getInstance(context).reviewResearchApplication(
                            item.getId(), "ACCEPTED", "Accepted into lab for " + item.getLabInterest(),
                            res -> {
                                notifyItemChanged(position);
                                Toast.makeText(context, item.getName() + " accepted & saved in database!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultRejectDialog(Context context, StudentApplicant item, int position, ViewHolder holder) {
        final EditText etFeedback = new EditText(context);
        etFeedback.setHint("Feedback notes (optional)");
        etFeedback.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(context)
                .setTitle("Review Decision: " + item.getName())
                .setMessage("Decline this application or request revisions from " + item.getName() + "?")
                .setView(etFeedback)
                .setPositiveButton("Decline", (dialog, which) -> {
                    holder.btnAccept.setEnabled(false);
                    holder.btnReject.setEnabled(false);
                    String feedback = etFeedback.getText().toString().trim();
                    if (feedback.isEmpty()) feedback = "Declined by faculty.";
                    final String note = feedback;
                    item.setStatus("Declined");
                    AppRepository.getInstance(context).reviewResearchApplication(
                            item.getId(), "REJECTED", note,
                            res -> {
                                notifyItemChanged(position);
                                Toast.makeText(context, "Application declined & updated in database.", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNeutralButton("Request Revision", (dialog, which) -> {
                    holder.btnAccept.setEnabled(false);
                    holder.btnReject.setEnabled(false);
                    String feedback = etFeedback.getText().toString().trim();
                    if (feedback.isEmpty()) feedback = "Please provide additional details regarding your research background.";
                    final String note = feedback;
                    item.setStatus("Revision Requested");
                    AppRepository.getInstance(context).reviewResearchApplication(
                            item.getId(), "REVISION_REQUESTED", note,
                            res -> {
                                notifyItemChanged(position);
                                Toast.makeText(context, "Revision requested from student.", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultMessageDialog(Context context, StudentApplicant item) {
        SessionManager session = SessionManager.getInstance(context);
        final EditText etMsg = new EditText(context);
        etMsg.setHint("Write a message to " + item.getName());
        etMsg.setText("Regarding your research interest in " + item.getLabInterest() + ": Please come to the lab on Sunday.");
        etMsg.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(context)
                .setTitle("Message " + item.getName())
                .setMessage("Student: " + item.getName() + " (Roll " + item.getRoll() + ")")
                .setView(etMsg)
                .setPositiveButton("Send", (dialog, which) -> {
                    String content = etMsg.getText().toString().trim();
                    if (content.isEmpty()) return;

                    AppRepository.getInstance(context).sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            item.getRoll(), item.getName(),
                            content,
                            msgId -> Toast.makeText(context, "Message sent to " + item.getName() + "!", Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultPortfolioDialog(Context context, StudentApplicant item) {
        String info = "Student: " + item.getName() + "\n"
                + "Department: " + item.getDepartment() + " • Roll: " + item.getRoll() + "\n"
                + "CGPA: " + item.getCgpa() + " / 4.00 (Verified by KUET Registry)\n"
                + "Research Focus: " + item.getLabInterest() + "\n"
                + "Technical Skills: " + item.getSkills() + "\n"
                + "Academic Standing: Verified Undergraduate Researcher";

        new AlertDialog.Builder(context)
                .setTitle("Student Researcher Portfolio")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDeptRoll, tvCgpa, tvInterest, tvSkills;
        Button btnAccept, btnReject, btnMessage;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvApplicantName);
            tvDeptRoll = itemView.findViewById(R.id.tvApplicantDeptRoll);
            tvCgpa = itemView.findViewById(R.id.tvApplicantCgpa);
            tvInterest = itemView.findViewById(R.id.tvApplicantInterest);
            tvSkills = itemView.findViewById(R.id.tvApplicantSkills);
            btnAccept = itemView.findViewById(R.id.btnAcceptApplicant);
            btnReject = itemView.findViewById(R.id.btnRejectApplicant);
            btnMessage = itemView.findViewById(R.id.btnMessageApplicant);
        }
    }
}
