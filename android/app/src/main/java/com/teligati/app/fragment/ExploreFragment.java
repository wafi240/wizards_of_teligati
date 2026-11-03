package com.teligati.app.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.AlumniAdapter;
import com.teligati.app.model.AlumniProfile;
import com.teligati.app.model.SampleData;
import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {
    private AlumniAdapter adapter;
    private List<AlumniProfile> fullList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.rvExplore);
        EditText etSearch = view.findViewById(R.id.etExploreSearch);
        Button btnGermanyHub = view.findViewById(R.id.btnGermanyHub);
        Button btnUsaHub = view.findViewById(R.id.btnUsaHub);
        Button btnVouchJunior = view.findViewById(R.id.btnVouchJunior);

        fullList = SampleData.getAlumniList();
        adapter = new AlumniAdapter(new ArrayList<>(fullList));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Country Relocation Hub Feature
        btnGermanyHub.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("🇩🇪 KUET Germany Chapter (Munich & Berlin)")
                    .setMessage("214 Verified Alumni in Germany\nTop Companies: BMW, Siemens, SAP, Google Munich\nTop Universities: TU Munich, RWTH Aachen\n\nNeed student accommodation or visa interview guidance?")
                    .setPositiveButton("Join Germany Chapter Chat", (d, w) -> {
                        Toast.makeText(getContext(), "Connected to Germany Alumni Chapter!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });

        btnUsaHub.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("🇺🇸 KUET North America Chapter")
                    .setMessage("1,245 Verified Alumni across USA\nTop Hubs: Silicon Valley, Austin Texas, Seattle\n\nIncludes PhD scholarship guidance and mock tech interview slots.")
                    .setPositiveButton("Explore USA Mentors", (d, w) -> {
                        etSearch.setText("USA");
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });

        // Alumni Vouch / Endorsement System
        btnVouchJunior.setOnClickListener(v -> {
            EditText etRoll = new EditText(getContext());
            etRoll.setHint("Enter Junior's Roll (e.g. 2307003)");
            new AlertDialog.Builder(getContext())
                    .setTitle("⭐ Give Official KUET Alumni Vouch")
                    .setMessage("Vouching adds a trusted endorsement badge to the student's profile for future employers.")
                    .setView(etRoll)
                    .setPositiveButton("Vouch for Junior", (d, w) -> {
                        Toast.makeText(getContext(), "Official KUET Vouch recorded for Roll " + etRoll.getText().toString() + "!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return view;
    }

    private void filter(String query) {
        List<AlumniProfile> filtered = new ArrayList<>();
        for (AlumniProfile a : fullList) {
            if (a.getName().toLowerCase().contains(query.toLowerCase()) ||
                a.getCurrentCompany().toLowerCase().contains(query.toLowerCase()) ||
                a.getCountry().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(a);
            }
        }
        adapter.updateList(filtered);
    }
}
