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
import com.teligati.app.model.ProjectItem;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {
    private List<ProjectItem> projects;

    public ProjectAdapter(List<ProjectItem> projects) {
        this.projects = projects;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectItem p = projects.get(position);
        holder.tvName.setText(p.getName());
        holder.tvType.setText(p.getType());
        holder.tvDescription.setText(p.getDescription());
        holder.tvTech.setText("Tech: " + String.join(", ", p.getTechnologies()));
        holder.tvOwner.setText("Owner: " + p.getOwnerName() + " (" + p.getOwnerDept() + ") • " + p.getContributorsCount() + " Collaborators");

        holder.btnContribute.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Collaborate on " + p.getName())
                    .setMessage("Send proposal to " + p.getOwnerName() + " to contribute your engineering skills?")
                    .setPositiveButton("Send Proposal", (dialog, which) -> {
                        holder.btnContribute.setText("Request Sent ✓");
                        holder.btnContribute.setEnabled(false);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvDescription, tvTech, tvOwner;
        Button btnContribute;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvType = itemView.findViewById(R.id.tvProjectType);
            tvDescription = itemView.findViewById(R.id.tvProjectDesc);
            tvTech = itemView.findViewById(R.id.tvProjectTech);
            tvOwner = itemView.findViewById(R.id.tvProjectOwner);
            btnContribute = itemView.findViewById(R.id.btnContribute);
        }
    }
}
