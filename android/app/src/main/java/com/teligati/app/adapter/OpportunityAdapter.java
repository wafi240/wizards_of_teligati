package com.teligati.app.adapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.model.Opportunity;
import java.util.List;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {
    private List<Opportunity> opportunities;

    public OpportunityAdapter(List<Opportunity> opportunities) {
        this.opportunities = opportunities;
    }

    public void updateList(List<Opportunity> newList) {
        this.opportunities = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Opportunity opp = opportunities.get(position);
        holder.tvTitle.setText(opp.getTitle());
        holder.tvOrg.setText(opp.getOrganization() + " • 📍 " + opp.getLocation());
        holder.tvPostedBy.setText("👤 Posted by: " + opp.getPostedByName() + " (" + opp.getPosterRole() + ")");
        holder.tvDescription.setText(opp.getDescription());
        holder.tvCategory.setText(opp.getCategory().getDisplayName());
        holder.tvCompensation.setText(opp.getCompensation());
        holder.tvDuration.setText(opp.getDuration());

        if (opp.isApplied()) {
            holder.btnApply.setEnabled(false);
            holder.btnApply.setText("Applied ✓");
        } else {
            holder.btnApply.setEnabled(true);
            holder.btnApply.setText("Apply Now");
            holder.btnApply.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Apply for " + opp.getTitle())
                        .setMessage("Submit your verified KUET profile and portfolio for " + opp.getCompensation() + "?")
                        .setPositiveButton("Submit", (dialog, which) -> {
                            opp.setApplied(true);
                            notifyItemChanged(position);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return opportunities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDuration, tvTitle, tvOrg, tvPostedBy, tvDescription, tvCompensation;
        Button btnApply;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvOrg = itemView.findViewById(R.id.tvOrg);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCompensation = itemView.findViewById(R.id.tvCompensation);
            btnApply = itemView.findViewById(R.id.btnApply);
        }
    }
}
