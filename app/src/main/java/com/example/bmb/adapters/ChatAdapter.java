package com.example.bmb.adapters;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bmb.R;
import com.example.bmb.data.models.ChatMessageModel;
import com.google.android.material.textview.MaterialTextView;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessageModel> chatMessages;
    private String currentUserId;

    public ChatAdapter(List<ChatMessageModel> chatMessages, String currentUserId) {
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessageModel message = chatMessages.get(position);

        holder.tvMessage.setText(message.getMessage());
        holder.tvTimestamp.setText(DateFormat.format("hh:mm a", message.getTimestamp()));

        if (message.getSenderId().equals(currentUserId)) {
            holder.llBodyChat.setBackgroundResource(R.drawable.right_chat);
            holder.llBodyChat.setGravity(Gravity.END);
            holder.llChat.setGravity(Gravity.END);
        } else {
            holder.llBodyChat.setBackgroundResource(R.drawable.left_chat);
            holder.llChat.setGravity(Gravity.START);
            holder.llBodyChat.setGravity(Gravity.START);
        }
    }


    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvMessage, tvTimestamp;
        LinearLayout llChat, llBodyChat;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            llChat = itemView.findViewById(R.id.llChat);
            llBodyChat = itemView.findViewById(R.id.llBodyChat);
        }
    }

    public void updateMessages(List<ChatMessageModel> newMessages) {
        chatMessages.clear();
        chatMessages.addAll(newMessages);
        notifyDataSetChanged();
    }

}