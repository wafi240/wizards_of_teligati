package com.teligati.app.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.teligati.app.R;
import com.teligati.app.mediastore.MediaImageItem;
import com.teligati.app.mediastore.MediaStoreHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaStoreImageAdapter extends RecyclerView.Adapter<MediaStoreImageAdapter.ViewHolder> {

    public interface OnImageClickListener {
        void onImageClick(MediaImageItem item);
    }

    private final Context context;
    private final List<MediaImageItem> items = new ArrayList<>();
    private final OnImageClickListener listener;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public MediaStoreImageAdapter(Context context, OnImageClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<MediaImageItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mediastore_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaImageItem item = items.get(position);
        holder.tvName.setText(item.getDisplayName());
        holder.ivThumbnail.setImageResource(R.drawable.ic_nav_profile);

        // Load thumbnail off main thread
        executor.execute(() -> {
            Bitmap thumb = MediaStoreHelper.loadThumbnail(context, item.getUri(), 160, 160);
            if (thumb != null) {
                mainHandler.post(() -> {
                    if (holder.getBindingAdapterPosition() == position) {
                        holder.ivThumbnail.setImageBitmap(thumb);
                    }
                });
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivMediaThumbnail);
            tvName = itemView.findViewById(R.id.tvMediaName);
        }
    }
}
