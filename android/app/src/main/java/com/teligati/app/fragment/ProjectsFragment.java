package com.teligati.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.teligati.app.R;
import com.teligati.app.adapter.ProjectAdapter;
import com.teligati.app.model.SampleData;

public class ProjectsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);
        RecyclerView rv = view.findViewById(R.id.rvProjects);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new ProjectAdapter(SampleData.getProjects()));
        return view;
    }
}
