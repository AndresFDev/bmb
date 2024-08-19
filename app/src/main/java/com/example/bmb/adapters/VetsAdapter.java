package com.example.bmb.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.transition.Transition;
import com.example.bmb.R;
import com.example.bmb.utils.ShimmerViewHelper;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.Map;

public class VetsAdapter extends RecyclerView.Adapter<VetsAdapter.VetViewHolder> {
    private List<Map<String, String>> vetsList;

    public VetsAdapter(List<Map<String, String>> vetsList) {
        this.vetsList = vetsList;
    }

    @NonNull
    @Override
    public VetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vets, parent, false);
        return new VetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VetViewHolder holder, int position) {
        Map<String, String> vet = vetsList.get(position);

        holder.shimmerViewHelper.startShimmer();

        holder.tvVetName.setText(vet.get("name"));
        holder.tvVetPhone.setText(vet.get("phone"));
        holder.tvVetAddress.setText(vet.get("address"));

        Glide.with(holder.itemView.getContext())
                .load(vet.get("image"))
                .error(R.drawable.ic_image_placeholder)
                .into(new com.bumptech.glide.request.target.CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        holder.ivVetImage.setImageDrawable(resource);
                        holder.shimmerVetImage.stopShimmer();
                        holder.shimmerVetImage.setVisibility(View.GONE);
                        holder.ivVetImage.setVisibility(View.VISIBLE);
                    }

                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        holder.ivVetImage.setImageDrawable(errorDrawable);
                        holder.shimmerVetImage.stopShimmer();
                        holder.shimmerVetImage.setVisibility(View.GONE);
                        holder.ivVetImage.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

    }

    @Override
    public int getItemCount() {
        return vetsList.size();
    }

    public static class VetViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVetImage;
        MaterialTextView tvVetName, tvVetPhone, tvVetAddress;
        private ShimmerViewHelper shimmerViewHelper;
        private ShimmerFrameLayout shimmerVetImage;

        public VetViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVetImage = itemView.findViewById(R.id.ivVetImage);
            tvVetName = itemView.findViewById(R.id.tvVetName);
            tvVetPhone = itemView.findViewById(R.id.tvVetPhone);
            tvVetAddress = itemView.findViewById(R.id.tvVetAddress);
            shimmerVetImage = itemView.findViewById(R.id.shimmerVetImage);

            shimmerViewHelper = new ShimmerViewHelper(shimmerVetImage);
        }
    }
}