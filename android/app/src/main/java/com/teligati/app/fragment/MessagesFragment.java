package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.MessageAdapter;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.entity.ConversationEntity;
import com.teligati.app.data.entity.MessageEntity;
import com.teligati.app.data.repository.AppRepository;
import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {
    private AppRepository repository;
    private RecyclerView rvMessages;
    private EditText etNewMessage;
    private TextView tvEmpty;
    private TextView tvActiveConversation;
    private MessageAdapter adapter;
    private final List<MessageEntity> messageList = new ArrayList<>();
    private final List<ConversationEntity> conversationList = new ArrayList<>();

    private Long activeConversationId = null;
    private String targetReceiverId = "2";
    private String targetReceiverName = "Engr. Tanvir Ahmed";
    private Button btnSend;
    private com.google.firebase.firestore.ListenerRegistration messageListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        repository = AppRepository.getInstance(requireContext());

        rvMessages = view.findViewById(R.id.rvMessagesList);
        etNewMessage = view.findViewById(R.id.etNewMessage);
        tvEmpty = view.findViewById(R.id.tvEmptyMessages);
        tvActiveConversation = view.findViewById(R.id.tvActiveConversation);
        Button btnSwitch = view.findViewById(R.id.btnSwitchConversation);
        btnSend = view.findViewById(R.id.btnSendMessage);

        if (getArguments() != null) {
            if (getArguments().containsKey("CONVERSATION_ID")) {
                activeConversationId = getArguments().getLong("CONVERSATION_ID");
            }
            if (getArguments().getString("RECEIVER_ID") != null) {
                targetReceiverId = getArguments().getString("RECEIVER_ID");
            }
            if (getArguments().getString("RECEIVER_NAME") != null) {
                targetReceiverName = getArguments().getString("RECEIVER_NAME");
            }
        }

        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MessageAdapter(messageList);
        rvMessages.setAdapter(adapter);

        updateActiveHeader();
        loadConversationsAndMessages();

        if (btnSwitch != null) {
            btnSwitch.setOnClickListener(v -> showConversationSwitchDialog());
        }

        if (btnSend != null && etNewMessage != null) {
            btnSend.setOnClickListener(v -> sendMessage());
        }

        return view;
    }

    private void updateActiveHeader() {
        if (tvActiveConversation != null) {
            tvActiveConversation.setText("Thread: " + targetReceiverName);
        }
    }

    private void loadConversationsAndMessages() {
        if (!isAdded()) return;
        repository.getConversations(convs -> {
            if (!isAdded()) return;
            conversationList.clear();
            if (convs != null) {
                conversationList.addAll(convs);
            }

            if (activeConversationId == null && !conversationList.isEmpty()) {
                selectConversation(conversationList.get(0));
            } else if (activeConversationId != null) {
                loadMessagesForActiveConversation();
            } else {
                loadAllMessagesFallback();
            }
        });
    }

    private void selectConversation(ConversationEntity conv) {
        activeConversationId = conv.getId();
        targetReceiverId = String.valueOf(conv.getOtherParticipantId());
        targetReceiverName = conv.getOtherParticipantName();
        updateActiveHeader();
        loadMessagesForActiveConversation();
        repository.markConversationAsRead(activeConversationId, null);
    }

    private void loadMessagesForActiveConversation() {
        if (activeConversationId == null || !isAdded()) return;
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        repository.getConversationMessages(activeConversationId, list -> {
            if (!isAdded()) return;
            messageList.clear();
            if (list != null) {
                messageList.addAll(list);
            }
            adapter.notifyDataSetChanged();
            if (rvMessages != null && !messageList.isEmpty()) {
                rvMessages.scrollToPosition(messageList.size() - 1);
            }
            if (tvEmpty != null) {
                tvEmpty.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        if (repository.getMessagingRepository() != null) {
            messageListener = repository.getMessagingRepository().listenToConversationMessages(activeConversationId, new com.teligati.app.data.repository.MessagingRepository.Callback<List<MessageEntity>>() {
                @Override
                public void onSuccess(List<MessageEntity> list) {
                    if (!isAdded()) return;
                    messageList.clear();
                    if (list != null) {
                        messageList.addAll(list);
                    }
                    adapter.notifyDataSetChanged();
                    if (rvMessages != null && !messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                }
            });
        }
    }

    private void loadAllMessagesFallback() {
        if (!isAdded()) return;
        SessionManager session = SessionManager.getInstance(requireContext());
        repository.getMessages(session.getUserId(), list -> {
            if (!isAdded()) return;
            messageList.clear();
            if (list != null) {
                messageList.addAll(list);
            }
            adapter.notifyDataSetChanged();
            if (rvMessages != null && !messageList.isEmpty()) {
                rvMessages.scrollToPosition(messageList.size() - 1);
            }
            if (tvEmpty != null) {
                tvEmpty.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showConversationSwitchDialog() {
        if (getContext() == null) return;
        if (conversationList.isEmpty()) {
            Toast.makeText(requireContext(), "No other conversations active yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[conversationList.size()];
        for (int i = 0; i < conversationList.size(); i++) {
            ConversationEntity c = conversationList.get(i);
            String unread = c.getUnreadCount() > 0 ? " (" + c.getUnreadCount() + " unread)" : "";
            items[i] = c.getOtherParticipantName() + unread + "\n\"" + (c.getLastMessageText() != null ? c.getLastMessageText() : "No messages") + "\"";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Conversation Thread")
                .setItems(items, (dialog, which) -> {
                    ConversationEntity selected = conversationList.get(which);
                    selectConversation(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendMessage() {
        if (etNewMessage == null || getContext() == null) return;
        String text = etNewMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        if (btnSend != null) btnSend.setEnabled(false);

        if (activeConversationId != null && activeConversationId > 0) {
            repository.sendConversationMessage(activeConversationId, text, msg -> {
                if (btnSend != null) btnSend.setEnabled(true);
                if (!isAdded()) return;
                etNewMessage.setText("");
                Toast.makeText(requireContext(), "Message sent & synced!", Toast.LENGTH_SHORT).show();
                loadMessagesForActiveConversation();
            });
        } else {
            SessionManager session = SessionManager.getInstance(requireContext());
            repository.sendDirectMessage(
                    session.getUserId(), session.getUserName(),
                    targetReceiverId, targetReceiverName,
                    text,
                    id -> {
                        if (btnSend != null) btnSend.setEnabled(true);
                        if (!isAdded()) return;
                        etNewMessage.setText("");
                        Toast.makeText(requireContext(), "Message sent!", Toast.LENGTH_SHORT).show();
                        loadConversationsAndMessages();
                    }
            );
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversationsAndMessages();
    }
}
