package com.example.bmb.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
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

    public void updatePosts(List<PostModel> newPosts) {
        this.postList.clear();
        this.postList.addAll(newPosts);
        notifyDataSetChanged();
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

    private float lerp(float startValue, float endValue, float startBound, float endBound, float value) {
        return startValue + (endValue - startValue) * (value - startBound) / (endBound - startBound);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }
}