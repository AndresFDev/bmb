package com.example.bmb.ui.main;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.bmb.R;
import com.example.bmb.adapters.PostAdapter;
import com.example.bmb.models.PostModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class FavoritesFragment extends Fragment {
    private RecyclerView rvFavorites;
    private PostAdapter postAdapter;
    private List<PostModel> favoritePosts;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        rvFavorites = view.findViewById(R.id.rvFavorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        favoritePosts = new ArrayList<>();
        postAdapter = new PostAdapter(favoritePosts);
        rvFavorites.setAdapter(postAdapter);

        loadFavoritePosts();

        return view;
    }

    private void loadFavoritePosts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .collection("favorites")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            PostModel post = document.toObject(PostModel.class);
                            favoritePosts.add(post);
                        }
                        postAdapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load favorites", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}