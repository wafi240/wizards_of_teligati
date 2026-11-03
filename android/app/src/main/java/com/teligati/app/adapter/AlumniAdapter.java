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
import com.teligati.app.model.AlumniProfile;
import java.util.List;

public class AlumniAdapter extends RecyclerView.Adapter<AlumniAdapter.ViewHolder> {
    private List<AlumniProfile> alumniList;

    public AlumniAdapter(List<AlumniProfile> alumniList) {
        this.alumniList = alumniList;
    }

    public void updateList(List<AlumniProfile> newList) {
        this.alumniList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alumni, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlumniProfile a = alumniList.get(position);
        holder.tvName.setText(a.getName() + " ✓ KUET Verified");
        holder.tvDeptBatch.setText(a.getDepartment() + " • " + a.getBatch());
        holder.tvJob.setText(a.getCurrentPosition() + " @ " + a.getCurrentCompany());
        holder.tvLocation.setText("📍 " + a.getCity() + ", " + a.getCountry());
        holder.tvBio.setText(a.getBio());

        holder.btnGuidance.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Request Guidance from " + a.getName())
                    .setMessage("Send a direct career guidance and mentorship request?")
                    .setPositiveButton("Send Request", (dialog, which) -> {
                        holder.btnGuidance.setText("Requested ✓");
                        holder.btnGuidance.setEnabled(false);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return alumniList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDeptBatch, tvJob, tvLocation, tvBio;
        Button btnGuidance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDeptBatch = itemView.findViewById(R.id.tvDeptBatch);
            tvJob = itemView.findViewById(R.id.tvJob);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvBio = itemView.findViewById(R.id.tvBio);
            btnGuidance = itemView.findViewById(R.id.btnGuidance);
        }
    }
}
