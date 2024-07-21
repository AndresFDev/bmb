package com.example.bmb.ui.main;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bmb.R;
import com.example.bmb.data.ImageManager;
import com.example.bmb.data.PostManager;
import com.example.bmb.data.models.PostModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddPostFragment extends Fragment {

    private ImageView ivPhoto;
    private TextInputEditText etTitle, etDescription, etPhone, etCity;
    private MaterialButton btnImage, btnAddPost;
    private PostManager postManager;
    private FirebaseAuth mAuth;
    private ImageManager imageManager;
    private Bitmap selectedBitmap;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private String postId, currentImageUrl;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postManager = new PostManager();
        mAuth = FirebaseAuth.getInstance();
        imageManager = new ImageManager(getActivity());

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                            selectedBitmap = bitmap;
                            ivPhoto.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Error al seleccionar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_post, container, false);

        btnImage = view.findViewById(R.id.btnImage);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etPhone = view.findViewById(R.id.etPhone);
        etCity = view.findViewById(R.id.etCity);
        ivPhoto = view.findViewById(R.id.ivPhoto);
        btnAddPost = view.findViewById(R.id.btnAddPost);

        btnImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        btnAddPost.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String description = etDescription.getText().toString();
            String phone = etPhone.getText().toString();
            String city = etCity.getText().toString();

            if (title.isEmpty() || description.isEmpty() || phone.isEmpty() || city.isEmpty()) {
                Toast.makeText(getActivity(), "Por favor, llena todos los campos.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedBitmap == null && postId == null) {
                Toast.makeText(getActivity(), "Por favor, selecciona una imagen.", Toast.LENGTH_SHORT).show();
                return;
            }

            long timestamp = System.currentTimeMillis();
            FirebaseUser user = mAuth.getCurrentUser();

            if (user != null) {
                String idUser = user.getUid();
                if (postId != null) {
                    updatePost(title, description, phone, city, timestamp, idUser);
                } else {
                    addNewPost(title, description, phone, city, timestamp, idUser);
                }
            } else {
                Toast.makeText(getActivity(), "Usuario no está autenticado", Toast.LENGTH_SHORT).show();
            }

        });

        if (postId != null) {
            loadPostData(postId);
        }

        return view;
    }

    private void addNewPost(String title, String description, String phone, String city, long timestamp, String idUser) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference newPostRef = db.collection("posts").document();
        String id = newPostRef.getId();

        uploadImageToFirebase(selectedBitmap, imageUrl -> {
            postManager.addPost(id, imageUrl, title, description, idUser, phone, city, timestamp, new PostManager.OnPostAddedListener() {
                @Override
                public void onSuccess(PostModel post) {
                    Toast.makeText(getActivity(), "Post creado con exito", Toast.LENGTH_SHORT).show();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new HomeFragment())
                            .commit();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getActivity(), "error creando post: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }, e -> {
            Toast.makeText(getActivity(), "Error subiendo una imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updatePost(String title, String description, String phone, String city, long timestamp, String idUser) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title", title);
        updatedData.put("description", description);
        updatedData.put("phone", phone);
        updatedData.put("city", city);
        updatedData.put("timestamp", timestamp);

        if (selectedBitmap != null) {
            uploadImageToFirebase(selectedBitmap, imageUrl -> {
                updatedData.put("imageUrl", imageUrl);

                postManager.updatePost(postId, updatedData, new PostManager.OnPostUpdatedListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getActivity(), "Post actualizado", Toast.LENGTH_SHORT).show();
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragmentContainer, new HomeFragment())
                                .commit();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getActivity(), "Error actualizando el post: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }, e -> {
                Toast.makeText(getActivity(), "Error actualizando la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            updatedData.put("imageUrl", currentImageUrl);

            postManager.updatePost(postId, updatedData, new PostManager.OnPostUpdatedListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getActivity(), "Post actualizado", Toast.LENGTH_SHORT).show();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new HomeFragment())
                            .commit();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getActivity(), "Error actualizando el post: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadPostData(String postId) {
        FirebaseFirestore.getInstance().collection("posts").whereEqualTo("id", postId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        btnAddPost.setText("Guardar cambios");
                        etTitle.setText(documentSnapshot.getString("title"));
                        etDescription.setText(documentSnapshot.getString("description"));
                        etPhone.setText(documentSnapshot.getString("phone"));
                        etCity.setText(documentSnapshot.getString("city"));
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        currentImageUrl = documentSnapshot.getString("imageUrl");
                        Glide.with(this).load(imageUrl).into(ivPhoto);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error cargando los datos del post", Toast.LENGTH_SHORT).show());
    }

    private void uploadImageToFirebase(Bitmap bitmap, OnSuccessListener<String> onSuccessListener, OnSuccessListener<Exception> onFailureListener) {
        imageManager.uploadImageToFirebase(bitmap, onSuccessListener, e -> onFailureListener.onSuccess(e));
    }

}