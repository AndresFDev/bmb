package com.example.bmb.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bmb.R;
import com.example.bmb.data.PostManager;
import com.example.bmb.models.PostModel;
import com.example.bmb.ui.MainActivity;
import com.example.bmb.ui.main.AddPostFragment;
import com.example.bmb.utils.ProgressUtils;
import com.example.bmb.utils.TimeAgoUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
    private FirebaseAuth auth;

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private ImageButton btnOptions;
        private ImageView ivPostImage, ivUserImage;
        private MaterialTextView tvPostContent, tvUserName, tvTime;
        private AppCompatImageButton btnFavorite;

        public PostViewHolder(View itemView) {
            super(itemView);
            btnOptions = itemView.findViewById(R.id.btnOptions);
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
        this.auth = FirebaseAuth.getInstance();
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

        String idUser = auth.getCurrentUser().getUid();
        String postId = post.getId();

        if(idUser.equals(post.getIdUser())) {
            holder.btnOptions.setVisibility(View.VISIBLE);
            holder.btnOptions.setOnClickListener(v -> showOptionsSheet(holder.itemView.getContext(), postId, position));
        }
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

    private void showOptionsSheet(Context context, String postId, int position) {

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        MaterialButton editPost = new MaterialButton(context);
        editPost.setPadding(32, 32, 0, 32);
        editPost.setBackgroundColor(context.getColor(R.color.none));
        editPost.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_edit));
        editPost.setIconTint(ColorStateList.valueOf(context.getColor(R.color.white)));
        editPost.setText("Editar post");
        editPost.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        editPost.setTextColor(context.getResources().getColor(R.color.white));
        editPost.setTextSize(16);
        editPost.setOnClickListener(v -> {
            Fragment fragment = new AddPostFragment();
            Bundle args = new Bundle();
            args.putString("postId", postId);
            fragment.setArguments(args);

            ((MainActivity) context).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();

            bottomSheetDialog.dismiss();
        });

        MaterialButton deletePost = new MaterialButton(context);
        deletePost.setPadding(32, 32, 8, 32);
        deletePost.setBackgroundColor(context.getResources().getColor(R.color.none));
        deletePost.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_delete));
        deletePost.setIconTint(ColorStateList.valueOf(context.getResources().getColor(com.google.android.material.R.color.design_default_color_error)));
        deletePost.setText("Eliminar post");
        deletePost.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        deletePost.setTextColor(context.getResources().getColor(com.google.android.material.R.color.design_default_color_error));
        deletePost.setTextSize(16);
        deletePost.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Eliminar post")
                    .setMessage("¿Estás seguro de que quieres eliminarel post?")
                    .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ProgressUtils.showProgress();

                            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            PostManager postManager = new PostManager();
                            postManager.deletePost(postId, currentUserId, new PostManager.OnPostDeletedListener() {
                                @Override
                                public void onSuccess() {
                                    ProgressUtils.hideProgress();
                                    Toast.makeText(context, "Post eliminado correctamente", Toast.LENGTH_SHORT).show();
                                    notifyItemRemoved(position);
                                }

                                @Override
                                public void onFailure(String error) {
                                    ProgressUtils.hideProgress();
                                    Toast.makeText(context, "Error al eliminar post: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
            bottomSheetDialog.dismiss();
        });

        layout.addView(editPost);
        layout.addView(deletePost);

        bottomSheetDialog.setContentView(layout);
        bottomSheetDialog.show();
    }

    private void showToast(View v, String message) {
        Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}