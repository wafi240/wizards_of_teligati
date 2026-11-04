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
import com.teligati.app.model.ReferenceRequest;
import java.util.List;

public class ReferenceRequestAdapter extends RecyclerView.Adapter<ReferenceRequestAdapter.ViewHolder> {
    private final List<ReferenceRequest> list;

    public ReferenceRequestAdapter(List<ReferenceRequest> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reference_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReferenceRequest item = list.get(position);
        holder.tvTargetCompany.setText("Target: " + item.getTargetCompany());
        holder.tvStatus.setText(item.getStatus());
        holder.tvStudentName.setText(item.getStudentName());
        holder.tvStudentMeta.setText(item.getStudentDept() + " • Roll " + item.getStudentRoll());
        holder.tvMessage.setText("\"" + item.getMessage() + "\"");

        if ("Endorsed".equalsIgnoreCase(item.getStatus())) {
            holder.btnEndorse.setEnabled(false);
            holder.btnEndorse.setText("Endorsed ✓");
            holder.btnDecline.setVisibility(View.GONE);
        } else {
            holder.btnEndorse.setEnabled(true);
            holder.btnEndorse.setText("Give Referral");
            holder.btnDecline.setVisibility(View.VISIBLE);

            holder.btnEndorse.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Endorse " + item.getStudentName())
                        .setMessage("Submit your verified Alumni referral for " + item.getStudentName() + " to " + item.getTargetCompany() + "?")
                        .setPositiveButton("Confirm Endorsement", (d, w) -> {
                            item.setStatus("Endorsed");
                            notifyItemChanged(position);
                            Toast.makeText(v.getContext(), "Referral confirmed and sent to employer ledger!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            holder.btnDecline.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Decline Request")
                        .setMessage("Politely decline this referral request?")
                        .setPositiveButton("Decline", (d, w) -> {
                            list.remove(position);
                            notifyItemRemoved(position);
                            Toast.makeText(v.getContext(), "Request declined", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTargetCompany, tvStatus, tvStudentName, tvStudentMeta, tvMessage;
        Button btnEndorse, btnDecline;

        ViewHolder(View itemView) {
            super(itemView);
            tvTargetCompany = itemView.findViewById(R.id.tvRefTargetCompany);
            tvStatus = itemView.findViewById(R.id.tvRefStatus);
            tvStudentName = itemView.findViewById(R.id.tvRefStudentName);
            tvStudentMeta = itemView.findViewById(R.id.tvRefStudentMeta);
            tvMessage = itemView.findViewById(R.id.tvRefMessage);
            btnEndorse = itemView.findViewById(R.id.btnEndorseReference);
            btnDecline = itemView.findViewById(R.id.btnDeclineReference);
        }
    }
}
