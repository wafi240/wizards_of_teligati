package com.teligati.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.data.entity.MessageEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<MessageEntity> messages;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault());

    public MessageAdapter(List<MessageEntity> messages) {
        this.messages = messages;
    }

    public void updateList(List<MessageEntity> newList) {
        this.messages = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_bubble, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageEntity msg = messages.get(position);
        holder.tvSender.setText(msg.getSenderName() != null ? msg.getSenderName() : "User");
        holder.tvContent.setText(msg.getContent() != null ? msg.getContent() : "");
        String readStatus = msg.isRead() ? " • Read ✓✓" : " • Sent ✓";
        holder.tvReceiver.setText("To: " + (msg.getReceiverName() != null ? msg.getReceiverName() : "User") + readStatus);
        holder.tvTime.setText(msg.getTimestamp() > 0 ? sdf.format(new Date(msg.getTimestamp())) : msg.getFormattedTime());
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvContent, tvReceiver, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvMessageSender);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvReceiver = itemView.findViewById(R.id.tvMessageReceiver);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}
