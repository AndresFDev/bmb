package com.example.bmb.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.bmb.R;
import com.example.bmb.adapters.ChatListAdapter;
import com.example.bmb.adapters.FollowListAdapter;
import com.example.bmb.data.ChatListManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private RecyclerView rvChats;
    private ConstraintLayout clNoChats;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FloatingActionButton fabNewChat;
    private ChatListAdapter chatListAdapter;
    private List<ChatListManager> chatList;
    private BottomSheetDialog bottomSheetDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        rvChats = view.findViewById(R.id.rvChats);
        clNoChats = view.findViewById(R.id.clNoChats);
        fabNewChat = view.findViewById(R.id.fabNewChat);

        chatList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(chatList, currentUser.getUid(), this::onChatClickListener);
        rvChats.setAdapter(chatListAdapter);
        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));

        if (chatList != null && !chatList.isEmpty()) {
            clNoChats.setVisibility(View.GONE);
            rvChats.setVisibility(View.VISIBLE);
        } else {
            clNoChats.setVisibility(View.VISIBLE);
            rvChats.setVisibility(View.GONE);
        }

        fabNewChat.setOnClickListener(v -> createNewChat());

        fabNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewChat();
            }
        });

        loadChatList();
        setupChatListener();

        return view;
    }

    private void loadChatList() {
        if (currentUser == null) return;

        db.collection("chats")
                .whereArrayContains("userIds", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ChatListManager> chats = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            ChatListManager chat = document.toObject(ChatListManager.class);
                            if (chat != null) {
                                chats.add(new ChatListManager(chat.getChatId(), chat.getUserIds()));
                            }
                        }
                        chatListAdapter.updateChatList(chats);
                    } else {
                        Log.e("ChatListFragment", "Error al obtener la lista de chats", task.getException());
                    }
                });
    }

    private void updateChatListUI(List<ChatListManager> chatList) {
        if (chatList != null && !chatList.isEmpty()) {
            clNoChats.setVisibility(View.GONE);
            rvChats.setVisibility(View.VISIBLE);

            chatListAdapter.updateChatList(chatList);
        } else {
            clNoChats.setVisibility(View.VISIBLE);
            rvChats.setVisibility(View.GONE);
        }
    }
    private void getFollowedUsers(OnFollowedUsersLoadedCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            db.collection("users").document(currentUserId)
                    .collection("following")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<String> followedUserIds = new ArrayList<>();
                            List<String> followedUserNames = new ArrayList<>();
                            List<String> followedUserPhotos = new ArrayList<>();

                            for (DocumentSnapshot document : task.getResult()) {
                                followedUserIds.add(document.getId());
                            }

                            if (!followedUserIds.isEmpty()) {
                                db.collection("users")
                                        .whereIn("userId", followedUserIds)
                                        .get()
                                        .addOnCompleteListener(userTask -> {
                                            if (userTask.isSuccessful()) {
                                                for (DocumentSnapshot userDoc : userTask.getResult()) {
                                                    String userName = userDoc.getString("name");
                                                    String userPhoto = userDoc.getString("userPhoto");

                                                    followedUserNames.add(userName != null ? userName : "Desconocido");
                                                    followedUserPhotos.add(userPhoto != null ? userPhoto : "");
                                                }
                                                callback.onSuccess(followedUserIds, followedUserNames, followedUserPhotos);
                                            } else {
                                                callback.onFailure(userTask.getException());
                                            }
                                        });
                            } else {
                                callback.onSuccess(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                            }
                        } else {
                            callback.onFailure(task.getException());
                        }
                    });
        }
    }

    public interface OnFollowedUsersLoadedCallback {
        void onSuccess(List<String> userIds, List<String> userNames, List<String> userPhotos);

        void onFailure(Exception e);
    }

    private void openChatWithUser(String userId) {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String chatId = currentUserId.compareTo(userId) < 0 ? currentUserId + "_" + userId : userId + "_" + currentUserId;

        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("receiverId", userId);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
    }


    private void createNewChat() {
        getFollowedUsers(new OnFollowedUsersLoadedCallback() {

            @Override
            public void onSuccess(List<String> followedUserIds, List<String> followedUserNames, List<String> userPhotos) {
                showFollowedUsersBottomSheet(followedUserIds, followedUserNames, userPhotos);
            }

            @Override
            public void onFailure(Exception e) {
                // Manejar el error
            }
        });
    }

    private void showFollowedUsersBottomSheet(List<String> userIds, List<String> userNames, List<String> userPhotos) {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_followed_users, null);
        RecyclerView rvFollowedUsers = bottomSheetView.findViewById(R.id.rvFollowedUsers);
        MaterialTextView tvNoFollowedUsers = bottomSheetView.findViewById(R.id.tvNoFollowedUsers);

        FollowListAdapter adapter = new FollowListAdapter(userIds, userNames, userPhotos, this::onUserSelected);
        rvFollowedUsers.setAdapter(adapter);
        rvFollowedUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        if (adapter.getItemCount() > 0) {
            rvFollowedUsers.setVisibility(View.VISIBLE);
            tvNoFollowedUsers.setVisibility(View.GONE);
        } else {
            rvFollowedUsers.setVisibility(View.GONE);
            tvNoFollowedUsers.setVisibility(View.VISIBLE);
        }

        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void onUserSelected(String userId) {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String chatId = currentUserId.compareTo(userId) < 0 ? currentUserId + "_" + userId : userId + "_" + currentUserId;

        openChatActivity(chatId, userId);
    }

    private void openChatActivity(String chatId, String receiverId) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("receiverId", receiverId);
        startActivity(intent);
    }

    private void setupChatListener() {
        if (currentUser == null) return;

        db.collection("chats")
                .whereArrayContains("userIds", currentUser.getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("FirebaseChat", "Listen failed.", e);
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            List<ChatListManager> chatList = new ArrayList<>();
                            for (QueryDocumentSnapshot document : snapshots) {
                                ChatListManager chat = document.toObject(ChatListManager.class);
                                chatList.add(chat);
                            }
                            updateChatListUI(chatList);
                        }
                    }
                });
    }

    private void onChatClickListener(ChatListManager chat) {
        String chatId = chat.getChatId();
        String receiverId = getOtherUserId(chat);
        openChatActivity(chatId, receiverId);
    }

    private String getOtherUserId(ChatListManager chat) {
        List<String> userIds = chat.getUserIds();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        for (String userId : userIds) {
            if (!userId.equals(currentUserId)) {
                return userId;
            }
        }
        return null;
    }
}