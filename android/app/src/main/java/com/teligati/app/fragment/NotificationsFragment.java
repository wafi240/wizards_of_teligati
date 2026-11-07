package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.NotificationAdapter;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.NotificationEntity;
import com.teligati.app.data.repository.AppRepository;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private final List<NotificationEntity> list = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmpty = view.findViewById(R.id.tvEmptyNotifications);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(list, (notif, pos) -> {
            if (!notif.isRead()) {
                repository.markNotificationAsRead(notif.getId(), success -> {
                    if (!isAdded()) return;
                    notif.setRead(true);
                    adapter.notifyItemChanged(pos);
                });
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle(notif.getTitle())
                    .setMessage(notif.getMessage());

            if (notif.getTitle() != null && notif.getTitle().toLowerCase().contains("message")) {
                builder.setPositiveButton("Open Chat", (d, w) -> {
                    if (getActivity() != null) {
                        Bundle args = new Bundle();
                        try {
                            if (notif.getFormattedTime() != null) {
                                // optional extra
                            }
                        } catch (Exception ignored) {}
                        MessagesFragment mf = new MessagesFragment();
                        mf.setArguments(args);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, mf)
                                .addToBackStack(null)
                                .commit();
                    }
                });
                builder.setNegativeButton("Close", null);
            } else {
                builder.setPositiveButton("Close", null);
            }
            builder.show();
        });
        rvNotifications.setAdapter(adapter);

        loadNotifications();
        return view;
    }

    private void loadNotifications() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        repository.getNotifications(session.getUserId(), notifications -> {
            if (!isAdded()) return;
            list.clear();
            if (notifications != null) {
                list.addAll(notifications);
            }
            adapter.notifyDataSetChanged();
            if (tvEmpty != null) {
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }
}
