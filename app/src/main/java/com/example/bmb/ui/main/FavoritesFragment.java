package com.example.bmb.ui.main;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.bmb.R;
import com.example.bmb.adapters.PostAdapter;
import com.example.bmb.data.models.PostModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class FavoritesFragment extends Fragment {
    private RecyclerView rvFavorites;
    private ConstraintLayout clEmpty;
    private PostAdapter postAdapter;
    private List<PostModel> favoritePosts;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        clEmpty = view.findViewById(R.id.clEmpty);
        rvFavorites = view.findViewById(R.id.rvFavorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        favoritePosts = new ArrayList<>();
        postAdapter = new PostAdapter(favoritePosts);
        rvFavorites.setAdapter(postAdapter);

        db = FirebaseFirestore.getInstance();
        loadFavoritePosts();

        return view;
    }

    private void loadFavoritePosts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            db.collection("users").document(userId)
                    .collection("favorites")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Toast.makeText(getContext(), "Error al cargar favoritos", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (queryDocumentSnapshots != null) {
                                List<String> favoritePostIds = new ArrayList<>();
                                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                    favoritePostIds.add(document.getId());
                                }

                                if (!favoritePostIds.isEmpty()) {
                                    loadPostsDetails(favoritePostIds);
                                } else {
                                    favoritePosts.clear();
                                    postAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
        }
    }

    private void loadPostsDetails(List<String> postIds) {
        db.collection("posts")
                .whereIn("id", postIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        favoritePosts.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            PostModel post = document.toObject(PostModel.class);
                            if (post != null) {
                                favoritePosts.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                        updateUI();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al cargar los detalles de los posts", Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        if (favoritePosts.isEmpty()) {
            clEmpty.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
        } else {
            clEmpty.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);
        }
    }
}