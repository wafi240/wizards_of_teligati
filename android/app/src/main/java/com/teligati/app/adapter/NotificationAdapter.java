package com.teligati.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.data.entity.NotificationEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationEntity notif, int position);
    }

    private List<NotificationEntity> notifications;
    private OnNotificationClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault());

    public NotificationAdapter(List<NotificationEntity> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public void updateList(List<NotificationEntity> newList) {
        this.notifications = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationEntity n = notifications.get(position);
        holder.tvTitle.setText(n.getTitle());
        holder.tvBody.setText(n.getMessage());
        holder.tvTime.setText(n.getTimestamp() > 0 ? sdf.format(new Date(n.getTimestamp())) : n.getFormattedTime());

        if (n.isRead()) {
            holder.tvStatus.setText("Read ✓");
            holder.tvStatus.setTextColor(Color.parseColor("#78909C"));
        } else {
            holder.tvStatus.setText("● New Alert");
            holder.tvStatus.setTextColor(Color.parseColor("#00E676"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(n, position);
        });
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvStatus = itemView.findViewById(R.id.tvNotificationReadStatus);
        }
    }
}
