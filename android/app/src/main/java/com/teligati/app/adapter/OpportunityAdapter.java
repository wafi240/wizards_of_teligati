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
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.Opportunity;
import java.util.List;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {
    public interface OnOpportunityActionListener {
        void onApply(Opportunity opp, int position);
        void onViewDetails(Opportunity opp, int position);
    }

    private List<Opportunity> opportunities;
    private OnOpportunityActionListener listener;

    public OpportunityAdapter(List<Opportunity> opportunities) {
        this.opportunities = opportunities;
    }

    public OpportunityAdapter(List<Opportunity> opportunities, OnOpportunityActionListener listener) {
        this.opportunities = opportunities;
        this.listener = listener;
    }

    public void setActionListener(OnOpportunityActionListener listener) {
        this.listener = listener;
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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(opp, position);
            }
        });

        if (opp.isApplied()) {
            holder.btnApply.setEnabled(false);
            holder.btnApply.setText("Applied ✓");
        } else {
            holder.btnApply.setEnabled(true);
            holder.btnApply.setText("Apply Now");
            holder.btnApply.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApply(opp, position);
                } else {
                    // Default fallback apply dialog
                    SessionManager session = SessionManager.getInstance(v.getContext());
                    String roll = session.getUserId();
                    AppRepository repo = AppRepository.getInstance(v.getContext());

                    repo.checkApplicationExists(opp.getId(), roll, alreadyApplied -> {
                        if (alreadyApplied) {
                            Toast.makeText(v.getContext(), "You have already applied for this position.", Toast.LENGTH_SHORT).show();
                            opp.setApplied(true);
                            notifyItemChanged(position);
                            return;
                        }

                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Apply for " + opp.getTitle())
                                .setMessage("Submit your verified profile (" + session.getUserName() + ", " + roll + ") for " + opp.getCompensation() + "?")
                                .setPositiveButton("Submit", (dialog, which) -> {
                                    holder.btnApply.setEnabled(false);
                                    opp.setApplied(true);
                                    notifyItemChanged(position);
                                    repo.applyForJob(
                                            opp.getId(), roll, session.getUserName(), session.getUserDept(),
                                            session.getUserCgpa(), "Proficient in requested stack", "Application submitted via Wizards of Teligati",
                                            res -> Toast.makeText(v.getContext(), "Application recorded & persisted!", Toast.LENGTH_SHORT).show()
                                    );
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }
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
