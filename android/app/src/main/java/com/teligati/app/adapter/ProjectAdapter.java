package com.teligati.app.adapter;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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
import com.teligati.app.model.ProjectItem;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {
    public interface OnProjectActionListener {
        void onCollaborate(ProjectItem item, int position);
        void onViewDetails(ProjectItem item, int position);
    }

    private List<ProjectItem> projects;
    private OnProjectActionListener listener;

    public ProjectAdapter(List<ProjectItem> projects) {
        this.projects = projects;
    }

    public ProjectAdapter(List<ProjectItem> projects, OnProjectActionListener listener) {
        this.projects = projects;
        this.listener = listener;
    }

    public void setActionListener(OnProjectActionListener listener) {
        this.listener = listener;
    }

    public void updateList(List<ProjectItem> newList) {
        this.projects = newList;
        notifyDataSetChanged();
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
        holder.tvTech.setText("Area/Tech: " + String.join(", ", p.getTechnologies()));
        holder.tvOwner.setText("Faculty: " + p.getOwnerName() + " (" + p.getOwnerDept() + ")");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(p, position);
            }
        });

        holder.btnContribute.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCollaborate(p, position);
            } else {
                SessionManager session = SessionManager.getInstance(v.getContext());
                AppRepository repo = AppRepository.getInstance(v.getContext());
                String roll = session.getUserId();

                repo.checkResearchApplicationExists(p.getId(), roll, alreadyInterested -> {
                    if (alreadyInterested) {
                        Toast.makeText(v.getContext(), "You have already expressed interest for this paper.", Toast.LENGTH_SHORT).show();
                        holder.btnContribute.setText("Interest Sent ✓");
                        holder.btnContribute.setEnabled(false);
                        return;
                    }

                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Collaborate on " + p.getName())
                            .setMessage("Send research co-author & collaboration request to " + p.getOwnerName() + " (" + p.getOwnerDept() + ")?")
                            .setPositiveButton("Send Request", (dialog, which) -> {
                                holder.btnContribute.setEnabled(false);
                                repo.expressInterestInResearch(
                                        p.getId(), p.getName(), roll, session.getUserName(), session.getUserDept(),
                                        session.getUserCgpa(), "Research & development", "Interest expressed via Wizards of Teligati",
                                        success -> {
                                            if (success) {
                                                holder.btnContribute.setText("Interest Sent ✓");
                                                Toast.makeText(v.getContext(), "Interest recorded in lab records!", Toast.LENGTH_SHORT).show();
                                            } else {
                                                holder.btnContribute.setEnabled(true);
                                            }
                                        }
                                );
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
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
