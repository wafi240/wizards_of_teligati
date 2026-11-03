package com.teligati.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.model.TransactionRecord;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<TransactionRecord> transactions;

    public TransactionAdapter(List<TransactionRecord> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionRecord tx = transactions.get(position);
        holder.tvTitle.setText(tx.getTitle());
        holder.tvCategoryDate.setText(tx.getCategory() + " • " + tx.getDate());
        holder.tvAmount.setText("+ ৳" + (int) tx.getAmount());
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategoryDate, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTxTitle);
            tvCategoryDate = itemView.findViewById(R.id.tvTxMeta);
            tvAmount = itemView.findViewById(R.id.tvTxAmount);
        }
    }
}
