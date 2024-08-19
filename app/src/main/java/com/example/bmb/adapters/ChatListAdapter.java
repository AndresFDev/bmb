package com.example.bmb.adapters;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.Transition;
import com.example.bmb.NotificationHelper;
import com.example.bmb.R;
import com.example.bmb.utils.ShimmerViewHelper;
import com.example.bmb.data.ChatListManager;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<ChatListManager> chatList;;
    private OnChatClickListener onChatClickListener;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId, userName;


    public ChatListAdapter(List<ChatListManager> chatList, String currentUserId, OnChatClickListener onChatClickListener) {
        this.chatList = chatList;
        this.currentUserId = currentUserId;
        this.onChatClickListener = onChatClickListener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatListManager chat = chatList.get(position);

        String otherUserId = findOtherUserId(chat.getUserIds());

        holder.shimmerViewHelper.startShimmer();
        holder.shimmerUserName.startShimmer();

        if (otherUserId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(otherUserId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot userDoc = task.getResult();
                            String userName = userDoc.getString("name");
                            String userPhoto = userDoc.getString("userPhoto");

                            // Asegúrate de actualizar la UI en el hilo principal
                            holder.itemView.post(() -> {

                                holder.shimmerUserName.setVisibility(View.VISIBLE);
                                holder.tvChatName.setVisibility(View.INVISIBLE);

                                holder.tvChatName.setText(userName != null ? userName : "Desconocido");

                                holder.shimmerUserName.stopShimmer();
                                holder.shimmerUserName.setVisibility(View.GONE);
                                holder.tvChatName.setVisibility(View.VISIBLE);

                                Glide.with(holder.itemView.getContext())
                                        .load(userPhoto != null ? userPhoto : "")
                                        .error(R.drawable.ic_user)
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(new com.bumptech.glide.request.target.CustomTarget<Drawable>() {
                                            @Override
                                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                                holder.ivUserPhoto.setImageDrawable(resource);
                                                holder.shimmerUserImage.stopShimmer();
                                                holder.shimmerUserImage.setVisibility(View.GONE);
                                                holder.ivUserPhoto.setVisibility(View.VISIBLE);
                                            }

                                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                                holder.ivUserPhoto.setImageDrawable(errorDrawable);
                                                holder.shimmerUserImage.stopShimmer();
                                                holder.shimmerUserImage.setVisibility(View.GONE);
                                                holder.ivUserPhoto.setVisibility(View.VISIBLE);
                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                            }
                                        });
                            });
                        }
                    });
                setupLastMessageListener(chat.getChatId(), holder);
        }
        holder.itemView.setOnClickListener(v -> {
            if (onChatClickListener != null) {
                onChatClickListener.onChatClick(chat);
            }
        });
    }

    private void setupLastMessageListener(String chatId, ChatViewHolder holder) {
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("ChatListAdapter", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        DocumentSnapshot lastMessageDoc = snapshots.getDocuments().get(0);
                        String lastMessage = lastMessageDoc.getString("message");
                        String senderId = lastMessageDoc.getString("senderId");
                        holder.tvLastMessage.setText(lastMessage != null ? lastMessage : "");

                        if (!senderId.equals(currentUserId)) {
                            Log.d("ChatListAdapter", "Mostrando notificación: " + lastMessage);
                            NotificationHelper.showNotification(holder.itemView.getContext(), userName, lastMessage);
                        }
                    } else {
                        holder.tvLastMessage.setText("");
                    }
                });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private String findOtherUserId(List<String> userIds) {
        for (String userId : userIds) {
            if (!userId.equals(currentUserId)) {
                return userId;
            }
        }
        return null;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivUserPhoto;
        private MaterialTextView tvChatName, tvLastMessage;
        private ShimmerViewHelper shimmerViewHelper;
        private ShimmerFrameLayout shimmerUserName, shimmerUserImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
            tvChatName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            shimmerUserName = itemView.findViewById(R.id.shimmerUserName);
            shimmerUserImage = itemView.findViewById(R.id.shimmerUserImage);

            shimmerViewHelper = new ShimmerViewHelper(shimmerUserImage);
        }
    }

    public void updateChatList(List<ChatListManager> newChatList) {
        this.chatList = newChatList;
        notifyDataSetChanged();
    }

    public interface OnChatClickListener {
        void onChatClick(ChatListManager chat);
    }

}