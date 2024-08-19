package com.example.bmb.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bmb.data.models.ChatMessageModel;
import com.example.bmb.utils.ChatDatabaseHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatManager {
    private FirebaseFirestore db;
    private ChatDatabaseHelper dbHelper;
    private final String chatId;

    public ChatManager(Context context, String chatId) {
        this.db = FirebaseFirestore.getInstance();
        this.dbHelper = new ChatDatabaseHelper(context);
        this.chatId = chatId;
        syncMessages();
    }

    public void createChat(String chatId, String senderId, String receiverId) {
        List<String> userIds = Arrays.asList(senderId, receiverId);

        String receiverName = "";
        String receiverPhoto = "";

        ChatListManager chatData = new ChatListManager(chatId, userIds);

        db.collection("chats").document(chatId)
                .set(chatData)
                .addOnSuccessListener(aVoid -> {
                    fetchAndUpdateReceiverInfo(chatId, receiverId);
                })
                .addOnFailureListener(e -> {

                });
    }

    private void fetchAndUpdateReceiverInfo(String chatId, String receiverId) {
        db.collection("users").document(receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot userDoc = task.getResult();
                        String receiverName = userDoc.getString("name");
                        String receiverPhoto = userDoc.getString("photo");

                        updateReceiverInfo(chatId, receiverName, receiverPhoto);
                    }
                });
    }

    public void updateReceiverInfo(String chatId, String receiverName, String receiverPhoto) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("receiverName", receiverName);
        updates.put("receiverPhoto", receiverPhoto);

        db.collection("chats").document(chatId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                });
    }



    public void sendMessage(ChatMessageModel message) {
        db.collection("chats").document(chatId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    createChat(chatId, currentUserId, message.getReceiverId());
                }

                dbHelper.addMessage(chatId, message);

                db.collection("chats").document(chatId)
                        .collection("messages")
                        .document(message.getId())
                        .set(message)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
            } else {
                Log.e("ChatManager", "Error al verificar existencia del chat", task.getException());
            }
        });
    }


    public List<ChatMessageModel> getAllMessages(String senderId, String receiverId) {
        return dbHelper.getMessagesByUsers(senderId, receiverId);
    }

    public void syncMessages() {
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w("ChatManager", "Error al sincronizar mensajes: ", e);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            ChatMessageModel message = dc.getDocument().toObject(ChatMessageModel.class);
                            switch (dc.getType()) {
                                case ADDED:
                                    Log.d("ChatManager", "Mensaje agregado: " + message.getId());
                                    dbHelper.addMessage(chatId, message);
                                    break;
                                case MODIFIED:
                                    Log.d("ChatManager", "Mensaje modificado: " + message.getId());
                                    break;
                                case REMOVED:
                                    Log.d("ChatManager", "Mensaje eliminado: " + message.getId());
                                    break;
                            }
                        }
                    } else {
                        Log.d("ChatManager", "No se encontraron mensajes en Firestore.");
                    }
                });
    }
}
