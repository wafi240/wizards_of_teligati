package com.teligati.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentHistoryAdapter extends RecyclerView.Adapter<StudentHistoryAdapter.ViewHolder> {
    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(HistoryEntry item, int position);
    }

    public static class HistoryEntry {
        public long id;
        public String category;
        public String title;
        public String subtitle;
        public String status;
        public long timestamp;

        public HistoryEntry(String category, String title, String subtitle, String status, long timestamp) {
            this(0, category, title, subtitle, status, timestamp);
        }

        public HistoryEntry(long id, String category, String title, String subtitle, String status, long timestamp) {
            this.id = id;
            this.category = category;
            this.title = title;
            this.subtitle = subtitle;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    private List<HistoryEntry> items;
    private OnHistoryItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

    public StudentHistoryAdapter(List<HistoryEntry> items) {
        this(items, null);
    }

    public StudentHistoryAdapter(List<HistoryEntry> items, OnHistoryItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setListener(OnHistoryItemClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<HistoryEntry> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntry item = items.get(position);
        holder.tvCategory.setText(item.category);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle);
        holder.tvStatus.setText(item.status);
        holder.tvTime.setText(dateFormat.format(new Date(item.timestamp > 0 ? item.timestamp : System.currentTimeMillis())));

        // Status color coding
        String s = item.status != null ? item.status.toLowerCase() : "";
        if (s.contains("accept") || s.contains("endorsed") || s.contains("shortlist")) {
            holder.tvStatus.setTextColor(Color.parseColor("#00E676")); // Green
        } else if (s.contains("reject") || s.contains("decline")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF5252")); // Red
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#00E5FF")); // Cyan
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onHistoryItemClick(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvTitle, tvSubtitle, tvStatus, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvHistoryCategory);
            tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvSubtitle = itemView.findViewById(R.id.tvHistorySubtitle);
            tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
        }
    }
}
