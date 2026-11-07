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
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.ResearchProposal;
import java.util.List;

public class ResearchProposalAdapter extends RecyclerView.Adapter<ResearchProposalAdapter.ViewHolder> {
    public interface OnProposalActionListener {
        void onAccept(ResearchProposal prop, int position);
        void onRequestRevision(ResearchProposal prop, int position);
        void onReject(ResearchProposal prop, int position);
        void onViewDetails(ResearchProposal prop, int position);
    }

    private final List<ResearchProposal> list;
    private OnProposalActionListener listener;

    public ResearchProposalAdapter(List<ResearchProposal> list) {
        this.list = list;
    }

    public ResearchProposalAdapter(List<ResearchProposal> list, OnProposalActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setListener(OnProposalActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_research_proposal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResearchProposal item = list.get(position);
        holder.tvDegree.setText(item.getDegreeType() != null ? item.getDegreeType() : "Undergraduate Thesis");
        holder.tvStatus.setText(item.getStatus() != null ? item.getStatus() : "Pending");
        holder.tvTopic.setText(item.getTopic());
        holder.tvAuthor.setText("Proposed by: " + item.getStudentName() + " (Roll " + item.getStudentRoll() + ")");
        holder.tvAbstract.setText(item.getProposalAbstract());

        String s = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
        if (s.contains("accept")) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E676"));
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accepted ✓");
            holder.btnRequestRevision.setEnabled(false);
            holder.btnRequestRevision.setText("Revise");
            holder.btnReject.setEnabled(false);
            holder.btnReject.setText("Reject");
        } else if (s.contains("revision")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFD700"));
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept");
            holder.btnRequestRevision.setEnabled(false);
            holder.btnRequestRevision.setText("Revision ⏳");
            holder.btnReject.setEnabled(true);
            holder.btnReject.setText("Reject");
        } else if (s.contains("reject") || s.contains("decline")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF5252"));
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accept");
            holder.btnRequestRevision.setEnabled(false);
            holder.btnRequestRevision.setText("Revise");
            holder.btnReject.setEnabled(false);
            holder.btnReject.setText("Rejected ✗");
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#00E5FF"));
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept");
            holder.btnRequestRevision.setEnabled(true);
            holder.btnRequestRevision.setText("Request Revision");
            holder.btnReject.setEnabled(true);
            holder.btnReject.setText("Reject");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(item, position);
            } else {
                showDefaultDetailsDialog(v.getContext(), item);
            }
        });

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(item, position);
            } else {
                showDefaultDecisionDialog(v.getContext(), item, "Accepted", position, holder);
            }
        });

        holder.btnRequestRevision.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestRevision(item, position);
            } else {
                showDefaultDecisionDialog(v.getContext(), item, "Revision Requested", position, holder);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(item, position);
            } else {
                showDefaultDecisionDialog(v.getContext(), item, "Rejected", position, holder);
            }
        });
    }

    private void showDefaultDetailsDialog(Context context, ResearchProposal item) {
        new AlertDialog.Builder(context)
                .setTitle("Proposal: " + item.getTopic())
                .setMessage("Student: " + item.getStudentName() + " (Roll " + item.getStudentRoll() + ")\n"
                        + "Degree: " + item.getDegreeType() + "\n"
                        + "Status: " + item.getStatus() + "\n\n"
                        + "Abstract:\n" + item.getProposalAbstract())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showDefaultDecisionDialog(Context context, ResearchProposal item, String action, int position, ViewHolder holder) {
        final EditText etFeedback = new EditText(context);
        etFeedback.setHint("Feedback / Instructions to student...");
        if ("Accepted".equalsIgnoreCase(action)) {
            etFeedback.setText("Accepted for lab supervision. Please visit the Intelligent Systems Lab on Sunday.");
        } else if (action.toLowerCase().contains("revision")) {
            etFeedback.setText("Refine section on methodology and baseline comparisons before re-submitting.");
        } else {
            etFeedback.setText("Currently unable to supervise this topic due to lab capacity.");
        }
        etFeedback.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(context)
                .setTitle("Mark Proposal as " + action)
                .setMessage("Proposal: \"" + item.getTopic() + "\"\nStudent: " + item.getStudentName() + " (Roll " + item.getStudentRoll() + ")\n\nFeedback:")
                .setView(etFeedback)
                .setPositiveButton("Confirm " + action, (dialog, which) -> {
                    // Prevent multi-clicks
                    holder.btnAccept.setEnabled(false);
                    holder.btnRequestRevision.setEnabled(false);
                    holder.btnReject.setEnabled(false);

                    String feedback = etFeedback.getText().toString().trim();
                    AppRepository.getInstance(context).updateResearchProposalStatus(item.getId(), action, feedback, success -> {
                        item.setStatus(action);
                        notifyItemChanged(position);
                        Toast.makeText(context, "Proposal marked as " + action + " & saved to database!", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDegree, tvStatus, tvTopic, tvAuthor, tvAbstract;
        Button btnAccept, btnRequestRevision, btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            tvDegree = itemView.findViewById(R.id.tvProposalDegree);
            tvStatus = itemView.findViewById(R.id.tvProposalStatus);
            tvTopic = itemView.findViewById(R.id.tvProposalTopic);
            tvAuthor = itemView.findViewById(R.id.tvProposalAuthor);
            tvAbstract = itemView.findViewById(R.id.tvProposalAbstract);
            btnAccept = itemView.findViewById(R.id.btnAcceptProposal);
            btnRequestRevision = itemView.findViewById(R.id.btnRequestRevision);
            btnReject = itemView.findViewById(R.id.btnRejectProposal);
        }
    }
}
