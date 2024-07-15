package com.example.bmb.data;

import com.example.bmb.models.PostModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.sql.Date;

public class PostManager {
    private FirebaseFirestore db;

    public PostManager() {
        db = FirebaseFirestore.getInstance();
    }

    public void addPost(String id, String imageUrl, String title, String description, String idUser, String phone, String city, Long timestamp, OnPostAddedListener listener) {
        String postId = db.collection("posts").document().getId();
        PostModel postModel = new PostModel(id, imageUrl, title, description, idUser, phone, city, timestamp);
        db.collection("posts").document(postId)
                .set(postModel)
                .addOnSuccessListener(aVoid -> listener.onSuccess(postModel))
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void deletePost(String postId, String userId, OnPostDeletedListener listener) {
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String idUser = documentSnapshot.getString("idUser");
                        if (idUser != null && idUser.equals(userId)) {
                            db.collection("posts").document(postId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                        } else {
                            listener.onFailure("No tienes permiso para eliminar este post.");
                        }
                    } else {
                        listener.onFailure("El post ya no existe.");
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onFailure("Error al obtener el post: " + e.getMessage());
                });
    }

    public interface OnPostAddedListener {
        void onSuccess(PostModel postModel);
        void onFailure(String errorMessage);
    }

    public interface OnPostDeletedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }
}