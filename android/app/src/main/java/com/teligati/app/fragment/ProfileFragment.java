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
import com.teligati.app.adapter.TransactionAdapter;
import com.teligati.app.model.SampleData;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        RecyclerView rvTx = view.findViewById(R.id.rvTransactions);
        rvTx.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTx.setAdapter(new TransactionAdapter(SampleData.getTransactions()));
        return view;
    }
}
