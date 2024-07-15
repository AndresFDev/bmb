package com.example.bmb.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bmb.R;
import com.example.bmb.models.PostModel;
import com.example.bmb.utils.TimeAgoUtils;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostModel> posts;
    private FirebaseFirestore db;

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivPostImage, ivUserImage;
        public MaterialTextView tvPostContent, tvUserName, tvTime;
        public AppCompatImageButton btnFavorite;

        public PostViewHolder(View itemView) {
            super(itemView);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            ivUserImage = itemView.findViewById(R.id.ivUserImage);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    public PostAdapter(List<PostModel> posts) {
        this.posts = posts;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_item, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        PostModel post = posts.get(position);

        checkFavoriteStatus(post, holder);

        holder.tvPostContent.setText(post.getDescription());
        String timeAgo = TimeAgoUtils.getTimeAgo(post.getTimestamp());
        holder.tvTime.setText(timeAgo);

        Glide.with(holder.itemView.getContext())
                .load(post.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.ivPostImage);

        loadUserDetails(post.getIdUser(), holder);

        holder.btnFavorite.setOnClickListener(v -> handleFavoriteButtonClick(post, holder, v));
    }

    private void loadUserDetails(String userId, PostViewHolder holder) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userPhotoUrl = documentSnapshot.getString("userPhoto");
                        String userName = documentSnapshot.getString("name");

                        Log.e("User", "Username: " + userName);

                        holder.tvUserName.setText(userName);
                        Glide.with(holder.itemView.getContext())
                                .load(userPhotoUrl)
                                .placeholder(R.drawable.ic_user_photo)
                                .into(holder.ivUserImage);

                    } else {
                        Log.d("PostsAdapter", "No se encontraron datos para el usuario actual");
                    }
                })
                .addOnFailureListener(e -> Log.e("PostsAdapter", "Error al obtener datos del usuario: " + e.getMessage()));
    }

    private void checkFavoriteStatus(PostModel post, PostViewHolder holder) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String postId = post.getId();

            DocumentReference favoriteRef = db.collection("users").document(userId)
                    .collection("favorites").document(postId);

            favoriteRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        holder.btnFavorite.setImageResource(R.drawable.ic_favorite_selected);
                    } else {
                        holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
                    }
                } else {
                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
                }
            });
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
        }
    }

    private void handleFavoriteButtonClick(PostModel post, PostViewHolder holder, View v) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            String postId = post.getId();

            DocumentReference favoriteRef = db.collection("users").document(userId)
                    .collection("favorites").document(postId);

            favoriteRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        favoriteRef.delete()
                                .addOnSuccessListener(aVoid -> {
                                    showToast(v, "Post removido de favoritos");
                                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite);
                                })
                                .addOnFailureListener(e -> showToast(v, "Error al remover de favoritos"));
                    } else {
                        favoriteRef.set(post)
                                .addOnSuccessListener(aVoid -> {
                                    showToast(v, "Post agregado a favoritos");
                                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite_selected);
                                })
                                .addOnFailureListener(e -> showToast(v, "Error al agregar a favoritos"));
                    }
                } else {
                    showToast(v, "Failed to check favorites status");
                }
            });
        } else {
            showToast(v, "Usuario no está autenticado");
        }
    }

    private void showToast(View v, String message) {
        Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}