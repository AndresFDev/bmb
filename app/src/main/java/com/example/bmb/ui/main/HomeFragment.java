package com.example.bmb.ui.main;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.bmb.R;
import com.example.bmb.adapters.PostAdapter;
import com.example.bmb.data.models.PostModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvHome;
    private PostAdapter postAdapter;
    private List<PostModel> postModelList;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvHome = view.findViewById(R.id.rvHome);
        rvHome.setLayoutManager(new LinearLayoutManager(getContext()));

        postModelList = new ArrayList<>();
        postAdapter = new PostAdapter(postModelList);
        rvHome.setAdapter(postAdapter);


        loadPosts();

        return view;
    }

    private void loadPosts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String id = document.getString("id");
                        String imageUrl = document.getString("imageUrl");
                        String title = document.getString("description");
                        String description = document.getString("description");
                        String idUser = document.getString("idUser");
                        String phone = document.getString("phone");
                        String city = document.getString("city");
                        long timestamp = document.getLong("timestamp");

                        PostModel post = new PostModel(id, imageUrl, title, description, idUser, phone, city, timestamp);
                        postModelList.add(post);
                    }
                    postAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error al cargar publicaciones", e);
                });
    }

    public RecyclerView getRvHome() {
        return rvHome;
    }
}