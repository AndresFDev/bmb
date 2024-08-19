package com.example.bmb.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bmb.R;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class FollowListAdapter extends RecyclerView.Adapter<FollowListAdapter.FollowViewHolder> {
    private List<String> userIds;
    private List<String> userNames;
    private List<String> userPhotos;
    private OnItemClickListener onItemClickListener;

    public FollowListAdapter(List<String> userIds, List<String> userNames, List<String> userPhotos, OnItemClickListener listener) {
        this.userIds = userIds;
        this.userNames = userNames;
        this.userPhotos = userPhotos;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public FollowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_user_item, parent, false);
        return new FollowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FollowViewHolder holder, int position) {
        String userName = userNames.get(position);
        holder.bind(userName, userIds.get(position), userPhotos.get(position));
    }

    @Override
    public int getItemCount() {
        return userNames.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String userId);
    }

    public class FollowViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPhoto;
        private MaterialTextView tvUserName;

        public FollowViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvUserName = itemView.findViewById(R.id.tvUserName);
        }

        public void bind(String userName, String userId, String userPhoto) {
            Glide.with(itemView.getContext())
                    .load(userPhoto)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_user_photo)
                    .error(R.drawable.ic_user_photo)
                    .into(ivPhoto);
            tvUserName.setText(userName);
            itemView.setOnClickListener(v -> onItemClickListener.onItemClick(userId));
        }
    }
}