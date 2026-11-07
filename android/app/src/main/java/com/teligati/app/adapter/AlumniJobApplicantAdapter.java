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
import com.teligati.app.data.entity.JobApplicationEntity;
import com.teligati.app.data.repository.AppRepository;
import java.util.List;

public class AlumniJobApplicantAdapter extends RecyclerView.Adapter<AlumniJobApplicantAdapter.ViewHolder> {
    public interface OnApplicantActionListener {
        void onAccept(JobApplicationEntity app, int position);
        void onShortlist(JobApplicationEntity app, int position);
        void onReject(JobApplicationEntity app, int position);
        void onMessage(JobApplicationEntity app, int position);
        void onViewPortfolio(JobApplicationEntity app, int position);
    }

    private List<JobApplicationEntity> list;
    private OnApplicantActionListener listener;

    public AlumniJobApplicantAdapter(List<JobApplicationEntity> list) {
        this.list = list;
    }

    public AlumniJobApplicantAdapter(List<JobApplicationEntity> list, OnApplicantActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setListener(OnApplicantActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<JobApplicationEntity> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_applicant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JobApplicationEntity app = list.get(position);
        holder.tvName.setText(app.getApplicantName());
        holder.tvDetails.setText("Roll " + app.getApplicantRoll() + " • " + app.getApplicantDept() + " • CGPA " + app.getApplicantCgpa());
        holder.tvJobTitle.setText("Applied for: " + app.getJobTitle());
        holder.tvStatement.setText("Statement: \"" + app.getStatement() + "\"\nSkills: " + app.getApplicantSkills());
        holder.tvStatus.setText(app.getStatus());

        String s = app.getStatus() != null ? app.getStatus().toLowerCase() : "";
        if (s.contains("accept")) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E676"));
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accepted ✓");
            holder.btnShortlist.setEnabled(false);
            holder.btnShortlist.setText("Shortlist");
            holder.btnReject.setEnabled(false);
            holder.btnReject.setText("Reject");
        } else if (s.contains("shortlist")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFD700"));
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept");
            holder.btnShortlist.setEnabled(false);
            holder.btnShortlist.setText("Shortlisted ✓");
            holder.btnReject.setEnabled(true);
            holder.btnReject.setText("Reject");
        } else if (s.contains("reject") || s.contains("decline")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF5252"));
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accept");
            holder.btnShortlist.setEnabled(false);
            holder.btnShortlist.setText("Shortlist");
            holder.btnReject.setEnabled(false);
            holder.btnReject.setText("Rejected ✗");
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#00E5FF"));
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept");
            holder.btnShortlist.setEnabled(true);
            holder.btnShortlist.setText("Shortlist");
            holder.btnReject.setEnabled(true);
            holder.btnReject.setText("Reject");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewPortfolio(app, position);
            } else {
                showDefaultPortfolioDialog(v.getContext(), app);
            }
        });

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(app, position);
            } else {
                showDefaultDecisionDialog(v.getContext(), app, "Accepted", position, holder);
            }
        });

        holder.btnShortlist.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShortlist(app, position);
            } else {
                showDefaultDecisionDialog(v.getContext(), app, "Shortlisted", position, holder);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(app, position);
            } else {
                showDefaultDecisionDialog(v.getContext(), app, "Rejected", position, holder);
            }
        });

        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessage(app, position);
            } else {
                showDefaultMessageDialog(v.getContext(), app);
            }
        });
    }

    private void showDefaultDecisionDialog(Context context, JobApplicationEntity app, String action, int position, ViewHolder holder) {
        new AlertDialog.Builder(context)
                .setTitle("Mark as " + action)
                .setMessage("Update application status for " + app.getApplicantName() + " (Roll " + app.getApplicantRoll() + ") to " + action + "?")
                .setPositiveButton("Confirm " + action, (dialog, which) -> {
                    // Prevent multi-clicks
                    holder.btnAccept.setEnabled(false);
                    holder.btnShortlist.setEnabled(false);
                    holder.btnReject.setEnabled(false);

                    AppRepository.getInstance(context).updateJobApplicationStatus(app.getId(), action, res -> {
                        app.setStatus(action);
                        notifyItemChanged(position);
                        Toast.makeText(context, "Application marked as " + action + "!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultMessageDialog(Context context, JobApplicationEntity app) {
        SessionManager session = SessionManager.getInstance(context);
        final EditText etMsg = new EditText(context);
        etMsg.setHint("Write a message to " + app.getApplicantName());
        etMsg.setText("Regarding your application for " + app.getJobTitle() + ": ");
        etMsg.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(context)
                .setTitle("Message " + app.getApplicantName())
                .setMessage("Student: Roll " + app.getApplicantRoll() + " (" + app.getApplicantDept() + ")")
                .setView(etMsg)
                .setPositiveButton("Send", (dialog, which) -> {
                    String content = etMsg.getText().toString().trim();
                    if (content.isEmpty()) return;

                    AppRepository.getInstance(context).sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            app.getApplicantRoll(), app.getApplicantName(),
                            content,
                            msgId -> Toast.makeText(context, "Message sent to " + app.getApplicantName() + "!", Toast.LENGTH_SHORT).show()
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultPortfolioDialog(Context context, JobApplicationEntity app) {
        String info = "Applicant: " + app.getApplicantName() + "\n"
                + "Department: " + app.getApplicantDept() + " • Roll: " + app.getApplicantRoll() + "\n"
                + "CGPA: " + app.getApplicantCgpa() + " / 4.00 (Verified)\n"
                + "Position: " + app.getJobTitle() + "\n"
                + "Key Skills: " + app.getApplicantSkills() + "\n\n"
                + "Statement of Interest:\n\"" + app.getStatement() + "\"";

        new AlertDialog.Builder(context)
                .setTitle("Applicant Portfolio")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvJobTitle, tvStatement, tvStatus;
        Button btnAccept, btnShortlist, btnReject, btnMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvApplicantName);
            tvDetails = itemView.findViewById(R.id.tvApplicantDetails);
            tvJobTitle = itemView.findViewById(R.id.tvAppliedJobTitle);
            tvStatement = itemView.findViewById(R.id.tvApplicantStatement);
            tvStatus = itemView.findViewById(R.id.tvApplicantStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnShortlist = itemView.findViewById(R.id.btnShortlist);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnMessage = itemView.findViewById(R.id.btnMessage);
        }
    }
}
