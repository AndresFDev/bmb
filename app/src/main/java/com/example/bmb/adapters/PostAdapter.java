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
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bmb.R;
import com.example.bmb.data.PostManager;
import com.example.bmb.data.models.PostModel;
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

import java.util.HashMap;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostModel> postList;
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

    public PostAdapter(List<PostModel> postList) {
        this.postList = postList;
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
        PostModel post = postList.get(position);

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
                        favoriteRef.set(new HashMap<>())
                                .addOnSuccessListener(aVoid -> {
                                    showToast(v, "Post agregado a favoritos");
                                    holder.btnFavorite.setImageResource(R.drawable.ic_favorite_selected);
                                })
                                .addOnFailureListener(e -> showToast(v, "Error al agregar a favoritos"));
                    }
                } else {
                    showToast(v, "Error al obtener el estado de favoritos");
                }
            });
        } else {
            showToast(v, "Usuario no está autenticado");
        }
    }

    private void showOptionsSheet(Context context, String postId, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView =  LayoutInflater.from(context).inflate(R.layout.bottom_sheet_layout, null);
        LinearLayout bottomSheetContent = sheetView.findViewById(R.id.llButtonSheet);

        MaterialButton editPost = bottomSheetContent.findViewById(R.id.btnOptionOne);
        editPost.setText("Editar post");
        editPost.setIcon(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_edit, context.getTheme()));
        editPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new AddPostFragment();
                Bundle args = new Bundle();
                args.putString("postId", postId);
                fragment.setArguments(args);

                ((MainActivity) context).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit();

                bottomSheetDialog.dismiss();
            }
        });

        MaterialButton deletePost = bottomSheetContent.findViewById(R.id.btnOptionTwo);
        deletePost.setText("Eliminar post");
        deletePost.setIcon(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_delete, context.getTheme()));
        deletePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Eliminar post")
                        .setMessage("¿Estás seguro de que quieres eliminar tu post? No lo podrás recuperar.")
                        .setPositiveButton("Eliminar", (dialog, which) -> deletePost())
                        .setNegativeButton("Cancelar", null)
                        .show();
                bottomSheetDialog.dismiss();
            }

            private void deletePost() {
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
                                        postList.remove(position);
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
            }
        });

        deletePost.setIconTint(ColorStateList.valueOf(context.getResources().getColor(com.google.android.material.R.color.design_default_color_error)));
        deletePost.setTextColor(context.getResources().getColor(com.google.android.material.R.color.design_default_color_error));

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void showToast(View v, String message) {
        Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }
}