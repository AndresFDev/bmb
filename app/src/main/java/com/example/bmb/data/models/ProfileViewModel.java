package com.example.bmb.data.models;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewModel extends ViewModel {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<FirebaseUser> currentUserLiveData;
    private final MutableLiveData<List<PostModel>> postListLiveData;
    private final MutableLiveData<Map<String, Object>> userDataLiveData;

    public ProfileViewModel() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserLiveData = new MutableLiveData<>(auth.getCurrentUser());
        postListLiveData = new MutableLiveData<>(new ArrayList<>());
        userDataLiveData = new MutableLiveData<>(new HashMap<>());
        fetchUserPosts();
        fetchUserData();
    }

    public LiveData<FirebaseUser> getCurrentUser() {
        return currentUserLiveData;
    }

    public LiveData<List<PostModel>> getPostList() {
        return postListLiveData;
    }

    public LiveData<Map<String, Object>> getUserData() {
        return userDataLiveData;
    }

    private void fetchUserPosts() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("posts")
                .whereEqualTo("idUser", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PostModel> postList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        PostModel post = document.toObject(PostModel.class);
                        if (post != null) {
                            postList.add(post);
                        }
                    }
                    postListLiveData.setValue(postList);
                })
                .addOnFailureListener(e -> Log.e("ProfileViewModel", "Error al obtener los posts: " + e.getMessage()));
    }

    private void fetchUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("ProfileViewModel", "Error al obtener los datos del usuario: " + e.getMessage());
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        userDataLiveData.setValue(documentSnapshot.getData());
                    }
                });
    }
}