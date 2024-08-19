package com.example.bmb.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.bmb.R;
import com.example.bmb.adapters.ChatAdapter;
import com.example.bmb.data.ChatListManager;
import com.example.bmb.data.ChatManager;
import com.example.bmb.data.models.ChatMessageModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration messageListener;
    private ChatManager chatManager;
    private RecyclerView rvMessages;
    private ChatAdapter chatAdapter;
    private List<ChatMessageModel> messages;
    private MaterialButton btnClose, btnSend;
    private ImageView ivUserPhoto;
    private MaterialTextView tvUserName;
    private TextInputEditText tietMessage;
    private String chatId, messageId, senderId, receiverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        chatId = getIntent().getStringExtra("chatId");
        receiverId = getIntent().getStringExtra("receiverId");
        senderId = FirebaseAuth.getInstance().getUid();

        chatManager = new ChatManager(this, chatId);

        btnClose = findViewById(R.id.btnClose);
        ivUserPhoto = findViewById(R.id.ivUserPhoto);
        tvUserName = findViewById(R.id.tvUserName);
        rvMessages = findViewById(R.id.rvMessages);
        tietMessage = findViewById(R.id.tietMessage);
        btnSend = findViewById(R.id.btnSend);

        tietMessage.requestFocus();

        // Desplegar el teclado
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(tietMessage, InputMethodManager.SHOW_IMPLICIT);

        if (chatId != null) {
            getChatData(chatId);
        } else {
            Log.e("ChatActivity", "chatId es nulo. No se puede continuar.");
            finish();
        }

        setupMessageListener();

        btnClose.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void getChatData(String chatId) {
        db.collection("chats").document(chatId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            ChatListManager chat = document.toObject(ChatListManager.class);
                            if (chat != null) {
                                initializeChat();
                            } else {
                                Log.e("ChatActivity", "No se pudo convertir el documento a ChatListManager");
                                finish();
                            }
                        } else {
                            fetchReceiverDetailsAndCreateChat(chatId, receiverId);
                        }
                    } else {
                        Log.e("ChatActivity", "Error al obtener los datos del chat", task.getException());
                        finish();
                    }
                });
    }

    private void fetchReceiverDetailsAndCreateChat(String chatId, String receiverId) {
        db.collection("chats").document(chatId).collection("messages").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().isEmpty()) {
                        createNewChatDocument(chatId);
                    } else {
                        Log.e("ChatActivity", "Error al recuperar mensajes o el chat ya tiene mensajes.");
                    }
                });
    }


    private void createNewChatDocument(String chatId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        List<String> userIds = new ArrayList<>(Arrays.asList(currentUserId, receiverId));
        userIds = new ArrayList<>(new HashSet<>(userIds));

        ChatListManager newChat = new ChatListManager(chatId, userIds);
        db.collection("chats").document(chatId).set(newChat)
                .addOnSuccessListener(aVoid -> {
                    initializeChat();
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Error al crear el nuevo documento de chat", e);
                    finish();
                });
    }

    private void initializeChat() {
        messages = chatManager.getAllMessages(receiverId, senderId);
        chatAdapter = new ChatAdapter(messages, senderId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        rvMessages.setAdapter(chatAdapter);
        rvMessages.setLayoutManager(layoutManager);

        getUserData(receiverId);
    }

    private void getUserData(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot userDoc = task.getResult();
                        String userName = userDoc.getString("name");
                        String userPhoto = userDoc.getString("userPhoto");

                        tvUserName.setText(userName != null ? userName : "Desconocido");
                        if (userPhoto != null) {
                            Glide.with(this)
                                    .load(userPhoto)
                                    .apply(RequestOptions.circleCropTransform())
                                    .into(ivUserPhoto);
                        } else {
                            ivUserPhoto.setImageResource(R.drawable.ic_user);
                        }
                    } else {
                        Log.e("ChatActivity", "Error al obtener datos del usuario", task.getException());
                    }
                });
    }

    private void sendMessage() {
        String messageText = tietMessage.getText().toString().trim();
        if (!messageText.isEmpty()) {
            long timestamp = System.currentTimeMillis();

            if (chatId == null) {
                Log.e("ChatActivity", "chatId es nulo. No se puede enviar el mensaje.");
                return;
            }

            CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");
            DocumentReference newMessageRef = messagesRef.document();

            ChatMessageModel message = new ChatMessageModel(newMessageRef.getId(), senderId, receiverId, messageText, timestamp);

            newMessageRef.set(message)
                    .addOnSuccessListener(aVoid -> {
                        tietMessage.setText("");
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }

    private void setupMessageListener() {
        messageListener = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w("FirestoreQuery", "Error al obtener mensajes: ", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<ChatMessageModel> updatedMessages = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ChatMessageModel message = doc.toObject(ChatMessageModel.class);
                            updatedMessages.add(message);
                        }

                        if (chatAdapter != null) {
                            chatAdapter.updateMessages(updatedMessages);
                        } else {
                            chatAdapter = new ChatAdapter(updatedMessages, currentUser.getUid());
                            rvMessages.setAdapter(chatAdapter);
                        }
                        rvMessages.scrollToPosition(updatedMessages.size() - 1);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}