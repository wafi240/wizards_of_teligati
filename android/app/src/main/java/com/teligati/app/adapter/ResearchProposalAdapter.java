package com.teligati.app.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.model.ResearchProposal;
import java.util.List;

public class ResearchProposalAdapter extends RecyclerView.Adapter<ResearchProposalAdapter.ViewHolder> {
    private final List<ResearchProposal> list;

    public ResearchProposalAdapter(List<ResearchProposal> list) {
        this.list = list;
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
        holder.tvDegree.setText(item.getDegreeType());
        holder.tvStatus.setText(item.getStatus());
        holder.tvTopic.setText(item.getTopic());
        holder.tvAuthor.setText("Proposed by: " + item.getStudentName() + " (Roll " + item.getStudentRoll() + ")");
        holder.tvAbstract.setText(item.getProposalAbstract());

        if ("Accepted".equalsIgnoreCase(item.getStatus())) {
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accepted ✓");
        } else {
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept Proposal");

            holder.btnAccept.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Accept Proposal")
                        .setMessage("Accept \"" + item.getTopic() + "\" for faculty mentorship and research supervision?")
                        .setPositiveButton("Accept & Supervise", (d, w) -> {
                            item.setStatus("Accepted");
                            notifyItemChanged(position);
                            Toast.makeText(v.getContext(), "Proposal accepted! Student notified.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        holder.btnReview.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle(item.getTopic())
                    .setMessage("Student: " + item.getStudentName() + " (Roll " + item.getStudentRoll() + ")\nType: " + item.getDegreeType() + "\n\nAbstract:\n" + item.getProposalAbstract())
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDegree, tvStatus, tvTopic, tvAuthor, tvAbstract;
        Button btnAccept, btnReview;

        ViewHolder(View itemView) {
            super(itemView);
            tvDegree = itemView.findViewById(R.id.tvProposalDegree);
            tvStatus = itemView.findViewById(R.id.tvProposalStatus);
            tvTopic = itemView.findViewById(R.id.tvProposalTopic);
            tvAuthor = itemView.findViewById(R.id.tvProposalAuthor);
            tvAbstract = itemView.findViewById(R.id.tvProposalAbstract);
            btnAccept = itemView.findViewById(R.id.btnAcceptProposal);
            btnReview = itemView.findViewById(R.id.btnReviewProposal);
        }
    }
}
