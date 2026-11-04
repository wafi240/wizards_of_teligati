package com.teligati.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
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
        ScrollView scrollChat = view.findViewById(R.id.scrollAiChat);
        Button chipTuition = view.findViewById(R.id.chipTuition);
        Button chipSecret = view.findViewById(R.id.chipSecret);
        Button chipResearch = view.findViewById(R.id.chipResearch);

        btnSend.setOnClickListener(v -> {
            String prompt = etPrompt.getText().toString().trim();
            if (!prompt.isEmpty()) {
                String reply;
                if (prompt.toLowerCase().contains("secret") || prompt.toLowerCase().contains("key") || prompt.toLowerCase().contains("password")) {
                    reply = "⚠️ Security Warning: Teligati AI detected potential credential/secret references in your prompt. Keep keys and database passwords confidential!";
                } else if (prompt.toLowerCase().contains("tuition") || prompt.toLowerCase().contains("assistant")) {
                    reply = "🤝 Match: 92% Match with Engr. Tanvir Ahmed (BMW Munich). Python Assistant role replaces private tuition with ৳15,000/month!";
                } else if (prompt.toLowerCase().contains("research") || prompt.toLowerCase().contains("paper")) {
                    reply = "🔬 Research Opportunity: Prof. Dr. K. M. Azharul Hasan has open positions in real-time YOLOv8 object detection. Eligible for IEEE co-authorship!";
                } else {
                    reply = "💡 Based on your verified KUET CSE profile, explore Alumni in Germany & Canada for graduate study guidance!";
                }
                tvChat.append("\n\nYou: " + prompt + "\n\nTeligati AI: " + reply);
                etPrompt.setText("");
                if (scrollChat != null) {
                    scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
                }
            }
        });

        chipTuition.setOnClickListener(v -> etPrompt.setText("Find Assistant jobs to replace my tuitions"));
        chipSecret.setOnClickListener(v -> etPrompt.setText("Scan my code for API_KEY leaks"));
        chipResearch.setOnClickListener(v -> etPrompt.setText("Which KUET professors have open research papers?"));

        return view;
    }
}
