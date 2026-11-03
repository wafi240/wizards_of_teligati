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

        fullList = SampleData.getAlumniList();
        adapter = new AlumniAdapter(new ArrayList<>(fullList));
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
