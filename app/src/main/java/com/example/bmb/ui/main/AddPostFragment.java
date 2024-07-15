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

import com.example.bmb.R;
import com.example.bmb.data.ImageManager;
import com.example.bmb.data.PostManager;
import com.example.bmb.models.PostModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddPostFragment extends Fragment {

    private ImageView ivPhoto;
    private TextInputEditText etTitle, etDescription, etPhone, etCity;
    private MaterialButton btnImage, btnAddPost;
    private PostManager postManager;
    private FirebaseAuth mAuth;
    private ImageManager imageManager;
    private Bitmap selectedBitmap;
    private ActivityResultLauncher<String> imagePickerLauncher;

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
                            ivPhoto.setImageBitmap(bitmap); // Mostrar la imagen seleccionada en ImageView
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Error selecting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference newPostRef = db.collection("posts").document(); // Genera un ID único
            String id = newPostRef.getId();

            String title = etTitle.getText().toString();
            String description = etDescription.getText().toString();
            String phone = etPhone.getText().toString();
            String city = etCity.getText().toString();

            long timestamp = System.currentTimeMillis();

            FirebaseUser user = mAuth.getCurrentUser();

            if (user != null) {
                String idUser = user.getUid();

                uploadImageToFirebase(selectedBitmap, imageUrl -> {
                    postManager.addPost(id, imageUrl, title, description, idUser, phone, city, timestamp, new PostManager.OnPostAddedListener() {
                        @Override
                        public void onSuccess(PostModel post) {
                            Toast.makeText(getActivity(), "Post added successfully", Toast.LENGTH_SHORT).show();
                            // Puedes navegar de regreso al HomeFragment o a otro fragmento después de añadir el post
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragmentContainer, new HomeFragment())
                                    .commit();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(getActivity(), "Error adding post: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }, e -> {
                    Toast.makeText(getActivity(), "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });


            } else {
                Toast.makeText(getActivity(), "User is not authenticated", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void uploadImageToFirebase(Bitmap bitmap, OnSuccessListener<String> onSuccessListener, OnSuccessListener<Exception> onFailureListener) {
        imageManager.uploadImageToFirebase(bitmap, onSuccessListener, e -> {
            onFailureListener.onSuccess(e);
        });
    }

}