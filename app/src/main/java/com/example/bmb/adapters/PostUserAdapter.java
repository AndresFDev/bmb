package com.example.bmb.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.carousel.MaskableFrameLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class PostUserAdapter extends RecyclerView.Adapter<PostUserAdapter.PostViewHolder> {

    private final Context context;
    private List<PostModel> postList;

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private ImageButton btnOptions;
        private ImageView ivCarousel;
        private TextView tvTitle;

        public PostViewHolder(View itemView) {
            super(itemView);
            ivCarousel = itemView.findViewById(R.id.ivCarousel);
            btnOptions = itemView.findViewById(R.id.btnOptions);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }

    public PostUserAdapter(Context context, List<PostModel> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_user_item, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        PostModel post = postList.get(position);
        String imageUrl = post.getImageUrl();

        holder.tvTitle.setText(post.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(holder.ivCarousel);

        ((MaskableFrameLayout) holder.itemView).setOnMaskChangedListener(maskRect -> {
            holder.tvTitle.setTranslationX(maskRect.left);
            holder.tvTitle.setAlpha(lerp(1F, 0F, 0F, 80F, maskRect.left));
        });

        String postId = post.getId();
        holder.btnOptions.setOnClickListener(v -> showOptionsSheet(holder.itemView.getContext(), postId, position));

    }

    private void showOptionsSheet(Context context,  String postId, int position) {

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
        deletePost.setTextSize(24);
        deletePost.setIconTint(ColorStateList.valueOf(context.getResources().getColor(com.google.android.material.R.color.design_default_color_error)));
        deletePost.setText("Eliminar post");
        deletePost.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        deletePost.setTextColor(context.getResources().getColor(com.google.android.material.R.color.design_default_color_error));
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
                                    postList.remove(position); // Eliminar el post de la lista local
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

    private float lerp(float startValue, float endValue, float startBound, float endBound, float value) {
        return startValue + (endValue - startValue) * (value - startBound) / (endBound - startBound);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }
}