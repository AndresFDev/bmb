package com.example.bmb.data;

import com.example.bmb.models.PostModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.sql.Date;
import java.util.Map;

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
        db.collection("posts")
                .whereEqualTo("id", postId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String idUser = documentSnapshot.getString("idUser");
                        if (idUser != null && idUser.equals(userId)) {
                            db.collection("posts").document(documentSnapshot.getId())
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

    public void updatePost(String postId, Map<String, Object> updatedData, OnPostUpdatedListener listener) {
        db.collection("posts").whereEqualTo("id", postId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().update(updatedData)
                                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                                    .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
                            return;
                        }
                    } else {
                        listener.onFailure("Post no encontrado");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure("Error al buscar post: " + e.getMessage()));
    }

    public interface OnPostUpdatedListener {
        void onSuccess();
        void onFailure(String error);
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