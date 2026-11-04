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
import com.teligati.app.model.StudentApplicant;
import java.util.List;

public class StudentApplicantAdapter extends RecyclerView.Adapter<StudentApplicantAdapter.ViewHolder> {
    private final List<StudentApplicant> list;

    public StudentApplicantAdapter(List<StudentApplicant> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_applicant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentApplicant item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDeptRoll.setText(item.getDepartment() + " • Roll " + item.getRoll());
        holder.tvCgpa.setText("CGPA: " + item.getCgpa());
        holder.tvInterest.setText("🎯 Focus: " + item.getLabInterest());
        holder.tvSkills.setText("Skills: " + item.getSkills());

        if ("Accepted".equalsIgnoreCase(item.getStatus())) {
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setText("Accepted into Lab ✓");
        } else {
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setText("Accept for Research");

            holder.btnAccept.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Recruit " + item.getName())
                        .setMessage("Recruit " + item.getName() + " (CGPA " + item.getCgpa() + ") for research collaboration in " + item.getLabInterest() + "?")
                        .setPositiveButton("Accept into Lab", (d, w) -> {
                            item.setStatus("Accepted");
                            notifyItemChanged(position);
                            Toast.makeText(v.getContext(), item.getName() + " accepted for research collaboration!", Toast.LENGTH_SHORT).show();
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
        TextView tvName, tvDeptRoll, tvCgpa, tvInterest, tvSkills;
        Button btnAccept;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvApplicantName);
            tvDeptRoll = itemView.findViewById(R.id.tvApplicantDeptRoll);
            tvCgpa = itemView.findViewById(R.id.tvApplicantCgpa);
            tvInterest = itemView.findViewById(R.id.tvApplicantInterest);
            tvSkills = itemView.findViewById(R.id.tvApplicantSkills);
            btnAccept = itemView.findViewById(R.id.btnAcceptApplicant);
        }
    }
}
