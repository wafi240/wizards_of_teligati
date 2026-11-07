package com.teligati.app.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.teligati.app.R;
import com.teligati.app.model.ResearchProposal;

import java.util.List;

public class StudentResearchProposalAdapter extends RecyclerView.Adapter<StudentResearchProposalAdapter.ViewHolder> {

    public interface OnProposalClickListener {
        void onProposalClick(ResearchProposal proposal);
    }

    private final List<ResearchProposal> proposals;
    private final OnProposalClickListener listener;

    public StudentResearchProposalAdapter(List<ResearchProposal> proposals, OnProposalClickListener listener) {
        this.proposals = proposals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_research_proposal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResearchProposal item = proposals.get(position);

        String degree = item.getDegreeType() != null && !item.getDegreeType().trim().isEmpty()
                ? item.getDegreeType()
                : "Research Proposal";
        holder.tvDegree.setText(degree);

        String status = item.getStatus() != null && !item.getStatus().trim().isEmpty()
                ? item.getStatus()
                : "Pending";
        holder.tvStatus.setText(status);

        // Colorize status badge
        String sLower = status.toLowerCase();
        if (sLower.contains("accept")) {
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Emerald Green
        } else if (sLower.contains("revision")) {
            holder.tvStatus.setTextColor(Color.parseColor("#F59E0B")); // Gold
        } else if (sLower.contains("reject") || sLower.contains("decline")) {
            holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // Soft Red
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#00ADB5")); // Mystic Cyan
        }

        holder.tvTopic.setText(item.getTopic());
        holder.tvDate.setText("📅 Submitted: " + item.getFormattedDate());
        holder.tvAbstract.setText(item.getProposalAbstract());

        // Display faculty feedback if present
        if (item.getFeedback() != null && !item.getFeedback().trim().isEmpty() && !"Under review".equalsIgnoreCase(item.getFeedback().trim())) {
            holder.layoutFeedback.setVisibility(View.VISIBLE);
            holder.tvFeedback.setText(item.getFeedback().trim());
        } else {
            holder.layoutFeedback.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProposalClick(item);
            } else {
                showDetailsDialog(v.getContext(), item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return proposals != null ? proposals.size() : 0;
    }

    private void showDetailsDialog(Context context, ResearchProposal item) {
        String facultyInfo = (item.getFacultyName() != null && !item.getFacultyName().isEmpty())
                ? "\nFaculty Supervisor: " + item.getFacultyName()
                : "";
        String feedbackInfo = (item.getFeedback() != null && !item.getFeedback().isEmpty())
                ? "\n\nFaculty Feedback:\n" + item.getFeedback()
                : "";

        new AlertDialog.Builder(context)
                .setTitle(item.getTopic())
                .setMessage("Research Area: " + item.getDegreeType() + "\n"
                        + "Submission Date: " + item.getFormattedDate() + "\n"
                        + "Status: " + item.getStatus()
                        + facultyInfo + "\n\n"
                        + "Abstract & Scope:\n" + item.getProposalAbstract()
                        + feedbackInfo)
                .setPositiveButton("Close", null)
                .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDegree;
        final TextView tvStatus;
        final TextView tvTopic;
        final TextView tvDate;
        final TextView tvAbstract;
        final View layoutFeedback;
        final TextView tvFeedback;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDegree = itemView.findViewById(R.id.tvStudentPropDegree);
            tvStatus = itemView.findViewById(R.id.tvStudentPropStatus);
            tvTopic = itemView.findViewById(R.id.tvStudentPropTopic);
            tvDate = itemView.findViewById(R.id.tvStudentPropDate);
            tvAbstract = itemView.findViewById(R.id.tvStudentPropAbstract);
            layoutFeedback = itemView.findViewById(R.id.layoutStudentPropFeedback);
            tvFeedback = itemView.findViewById(R.id.tvStudentPropFeedback);
        }
    }
}
