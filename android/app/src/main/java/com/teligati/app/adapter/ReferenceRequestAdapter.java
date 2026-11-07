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
import com.teligati.app.data.repository.AppRepository;
import com.teligati.app.model.ReferenceRequest;
import java.util.List;

public class ReferenceRequestAdapter extends RecyclerView.Adapter<ReferenceRequestAdapter.ViewHolder> {
    public interface OnReferenceActionListener {
        void onEndorse(ReferenceRequest req, int position);
        void onDecline(ReferenceRequest req, int position);
        void onGuidance(ReferenceRequest req, int position);
        void onViewPortfolio(ReferenceRequest req, int position);
    }

    private final List<ReferenceRequest> list;
    private OnReferenceActionListener listener;

    public ReferenceRequestAdapter(List<ReferenceRequest> list) {
        this.list = list;
    }

    public ReferenceRequestAdapter(List<ReferenceRequest> list, OnReferenceActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setListener(OnReferenceActionListener listener) {
        this.listener = listener;
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

        String s = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
        if (s.contains("complete")) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E676"));
            holder.btnEndorse.setEnabled(true);
            holder.btnEndorse.setText("Letter ✓");
            holder.btnDecline.setEnabled(false);
        } else if (s.contains("accept") || s.contains("endorse")) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E5FF"));
            holder.btnEndorse.setEnabled(true);
            holder.btnEndorse.setText("Write Letter ✎");
            holder.btnDecline.setEnabled(true);
        } else if (s.contains("decline") || s.contains("reject") || s.contains("cancel")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF5252"));
            holder.btnEndorse.setEnabled(false);
            holder.btnEndorse.setText("Closed");
            holder.btnDecline.setEnabled(false);
        } else if (s.contains("guid")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFD700"));
            holder.btnEndorse.setEnabled(true);
            holder.btnEndorse.setText("Accept / Endorse");
            holder.btnDecline.setEnabled(true);
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#FFD700"));
            holder.btnEndorse.setEnabled(true);
            holder.btnEndorse.setText("Accept / Endorse");
            holder.btnDecline.setEnabled(true);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewPortfolio(item, position);
            } else {
                showDefaultPortfolioDialog(v.getContext(), item);
            }
        });

        holder.btnEndorse.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEndorse(item, position);
            } else {
                showDefaultEndorseDialog(v.getContext(), item, position, holder);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecline(item, position);
            } else {
                showDefaultDeclineDialog(v.getContext(), item, position, holder);
            }
        });

        holder.btnReplyGuidance.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGuidance(item, position);
            } else {
                showDefaultGuidanceDialog(v.getContext(), item, position);
            }
        });
    }

    private void showDefaultEndorseDialog(Context context, ReferenceRequest item, int position, ViewHolder holder) {
        new AlertDialog.Builder(context)
                .setTitle("Endorse " + item.getStudentName())
                .setMessage("Submit your verified Alumni referral for " + item.getStudentName() + " to " + item.getTargetCompany() + "?")
                .setPositiveButton("Confirm Endorsement", (d, w) -> {
                    holder.btnEndorse.setEnabled(false);
                    holder.btnDecline.setEnabled(false);
                    item.setStatus("ACCEPTED");
                    AppRepository.getInstance(context).updateReferenceRequestStatus(
                            item.getId(), "ACCEPTED",
                            res -> {
                                notifyItemChanged(position);
                                Toast.makeText(context, "Referral confirmed and saved to database!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultDeclineDialog(Context context, ReferenceRequest item, int position, ViewHolder holder) {
        new AlertDialog.Builder(context)
                .setTitle("Decline Request")
                .setMessage("Politely decline this referral request from " + item.getStudentName() + "?")
                .setPositiveButton("Decline", (d, w) -> {
                    holder.btnEndorse.setEnabled(false);
                    holder.btnDecline.setEnabled(false);
                    item.setStatus("DECLINED");
                    AppRepository.getInstance(context).updateReferenceRequestStatus(
                            item.getId(), "DECLINED",
                            res -> {
                                notifyItemChanged(position);
                                Toast.makeText(context, "Request declined & updated!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultGuidanceDialog(Context context, ReferenceRequest item, int position) {
        SessionManager session = SessionManager.getInstance(context);
        final EditText etGuidance = new EditText(context);
        etGuidance.setHint("Write career advice or preparation guidance...");
        etGuidance.setText("Hi " + item.getStudentName() + ", please focus on DSA and system design for " + item.getTargetCompany() + ".");
        etGuidance.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(context)
                .setTitle("Mentorship for " + item.getStudentName())
                .setMessage("Send mentorship advice & suggestions to the student:")
                .setView(etGuidance)
                .setPositiveButton("Send Guidance", (dialog, which) -> {
                    String advice = etGuidance.getText().toString().trim();
                    if (advice.isEmpty()) return;

                    AppRepository.getInstance(context).sendDirectMessage(
                            session.getUserId(), session.getUserName(),
                            item.getStudentRoll(), item.getStudentName(),
                            "Career Guidance for " + item.getTargetCompany() + ": " + advice,
                            msgId -> {
                                item.setStatus("Guided");
                                notifyItemChanged(position);
                                Toast.makeText(context, "Mentorship advice sent to student!", Toast.LENGTH_SHORT).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDefaultPortfolioDialog(Context context, ReferenceRequest item) {
        String info = "Student: " + item.getStudentName() + "\n"
                + "Department: " + item.getStudentDept() + " • Roll: " + item.getStudentRoll() + "\n"
                + "Target: " + item.getTargetCompany() + "\n"
                + "Current Status: " + item.getStatus() + "\n\n"
                + "Personal Pitch:\n\"" + item.getMessage() + "\"";

        new AlertDialog.Builder(context)
                .setTitle("Reference Applicant")
                .setMessage(info)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTargetCompany, tvStatus, tvStudentName, tvStudentMeta, tvMessage;
        Button btnEndorse, btnDecline, btnReplyGuidance;

        ViewHolder(View itemView) {
            super(itemView);
            tvTargetCompany = itemView.findViewById(R.id.tvRefTargetCompany);
            tvStatus = itemView.findViewById(R.id.tvRefStatus);
            tvStudentName = itemView.findViewById(R.id.tvRefStudentName);
            tvStudentMeta = itemView.findViewById(R.id.tvRefStudentMeta);
            tvMessage = itemView.findViewById(R.id.tvRefMessage);
            btnEndorse = itemView.findViewById(R.id.btnEndorseReference);
            btnDecline = itemView.findViewById(R.id.btnDeclineReference);
            btnReplyGuidance = itemView.findViewById(R.id.btnReplyGuidance);
        }
    }
}
