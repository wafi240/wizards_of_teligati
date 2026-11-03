package com.teligati.app.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.OpportunityAdapter;
import com.teligati.app.model.Opportunity;
import com.teligati.app.model.SampleData;
import java.util.ArrayList;
import java.util.List;

public class OpportunitiesFragment extends Fragment {
    private OpportunityAdapter adapter;
    private List<Opportunity> fullList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_opportunities, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.rvOpportunities);
        EditText etSearch = view.findViewById(R.id.etSearch);

        fullList = SampleData.getOpportunities();
        adapter = new OpportunityAdapter(new ArrayList<>(fullList));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void filter(String query) {
        List<Opportunity> filtered = new ArrayList<>();
        for (Opportunity o : fullList) {
            if (o.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                o.getOrganization().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(o);
            }
        }
        adapter.updateList(filtered);
    }
}
