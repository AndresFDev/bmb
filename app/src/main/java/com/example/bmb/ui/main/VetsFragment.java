package com.example.bmb.ui.main;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.bmb.R;
import com.example.bmb.adapters.VetsAdapter;
import com.example.bmb.utils.VetsData;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.Map;

public class VetsFragment extends Fragment {
    private ImageView ivImage;
    private MaterialTextView tvNoEntries;
    private RecyclerView rvVets;
    private VetsAdapter vetsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vets, container, false);

        rvVets = view.findViewById(R.id.rvVets);
        ivImage = view.findViewById(R.id.ivImage);
        tvNoEntries = view.findViewById(R.id.tvNoEntries);

        List<Map<String, String>> vetsList = VetsData.getVetsList();
        vetsAdapter = new VetsAdapter(vetsList);

        rvVets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvVets.setAdapter(vetsAdapter);

        updateVisibility();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void updateVisibility() {
        if (vetsAdapter.getItemCount() > 0) {
            ivImage.setVisibility(View.GONE);
            tvNoEntries.setVisibility(View.GONE);
            rvVets.setVisibility(View.VISIBLE);
        } else {
            ivImage.setVisibility(View.VISIBLE);
            tvNoEntries.setVisibility(View.VISIBLE);
            rvVets.setVisibility(View.GONE);
        }
    }
}