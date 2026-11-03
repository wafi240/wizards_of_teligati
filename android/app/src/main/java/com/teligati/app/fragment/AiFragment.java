package com.teligati.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.teligati.app.R;

public class AiFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);
        TextView tvChat = view.findViewById(R.id.tvAiChat);
        EditText etPrompt = view.findViewById(R.id.etAiPrompt);
        ImageButton btnSend = view.findViewById(R.id.btnSendAi);

        btnSend.setOnClickListener(v -> {
            String prompt = etPrompt.getText().toString().trim();
            if (!prompt.isEmpty()) {
                String reply;
                if (prompt.toLowerCase().contains("secret") || prompt.toLowerCase().contains("key")) {
                    reply = "⚠️ Security Warning: Teligati AI detected potential credential/secret references in your prompt. Keep keys confidential!";
                } else if (prompt.toLowerCase().contains("assistant")) {
                    reply = "🤝 Match: 92% Match with Engr. Tanvir Ahmed (BMW Munich). Python Assistant role replaces private tuition with ৳15,000/month!";
                } else {
                    reply = "💡 Based on your verified KUET CSE profile, explore Alumni in Germany & Canada for graduate study guidance!";
                }
                tvChat.append("\n\nYou: " + prompt + "\n\nTeligati AI: " + reply);
                etPrompt.setText("");
            }
        });

        return view;
    }
}
