package com.example.bmb.ui.main;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bmb.R;
import com.example.bmb.adapters.PostUserAdapter;
import com.example.bmb.data.AuthManager;
import com.example.bmb.data.ImageManager;
import com.example.bmb.data.PostManager;
import com.example.bmb.models.PostModel;
import com.example.bmb.ui.LoginActivity;
import com.example.bmb.ui.MainActivity;
import com.example.bmb.utils.ProgressUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.carousel.CarouselLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView ivPhoto, ivNewPhoto;
    private ImageButton btnUserSettings;
    private MaterialTextView tvUserName;
    private ConstraintLayout clTopDataUser, clEditUser;
    private TextInputLayout tilUserName, tilEmail, tilPassword, tilNewPassword, tilConfirm;
    private TextInputEditText etUserName, etEmail, etPassword, etNewPassword, etConfirm;
    private MaterialButton btnPhoto, btnShow, btnSave;
    private PostUserAdapter postsAdapter;
    private PostManager postManager;
    private List<PostModel> postList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView rvPostUser;
    private AuthManager authManager;
    private ImageManager imageManager;
    private Bitmap selectedBitmap;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private boolean arePasswordFieldsVisible = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        authManager = new AuthManager(requireContext());

        postList = new ArrayList<>();
        postManager = new PostManager();
        postsAdapter = new PostUserAdapter(getContext(), postList);

        fetchUserPosts();

        imageManager = new ImageManager(getActivity());

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                            selectedBitmap = bitmap;
                            ivNewPhoto.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Error selecting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        btnUserSettings = view.findViewById(R.id.btnUserSettings);
        ivPhoto = view.findViewById(R.id.ivPhoto);
        ivNewPhoto = view.findViewById(R.id.ivNewPhoto);
        tvUserName = view.findViewById(R.id.tvUserName);
        etUserName = view.findViewById(R.id.etUserName);
        tilUserName = view.findViewById(R.id.tilUserName);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilNewPassword = view.findViewById(R.id.tilNewPassword);
        tilConfirm = view.findViewById(R.id.tilConfirm);
        btnPhoto = view.findViewById(R.id.btnPhoto);
        btnShow = view.findViewById(R.id.btnShow);
        btnSave = view.findViewById(R.id.btnSave);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirm = view.findViewById(R.id.etConfirm);
        clTopDataUser = view.findViewById(R.id.clTopDataUser);
        clEditUser = view.findViewById(R.id.clEditUser);
        rvPostUser = view.findViewById(R.id.rvPostUser);


        rvPostUser.setLayoutManager(new CarouselLayoutManager());
        rvPostUser.setAdapter(postsAdapter);

        buttonChangeListeners();

        return view;
    }

    private void buttonChangeListeners() {
        btnUserSettings.setOnClickListener(v -> showBottomSheet());
        btnPhoto.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
        btnShow.setOnClickListener(v -> {
            arePasswordFieldsVisible = !arePasswordFieldsVisible;
            showOptions();
        });
        btnSave.setOnClickListener(v -> updateUser());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userPhotoUrl = documentSnapshot.getString("userPhoto");
                            String userName = documentSnapshot.getString("name");

                            requireActivity().runOnUiThread(() -> {
                                tvUserName.setText(userName);
                                etUserName.setText(userName);
                                etEmail.setText(currentUser.getEmail());

                                Glide.with(requireContext())
                                        .load(userPhotoUrl)
                                        .placeholder(R.drawable.ic_user_photo)
                                        .error(R.drawable.ic_user_photo)
                                        .into(ivPhoto);

                                Glide.with(requireContext())
                                        .load(userPhotoUrl)
                                        .placeholder(R.drawable.ic_user_photo)
                                        .error(R.drawable.ic_user_photo)
                                        .into(ivNewPhoto);
                            });
                        } else {
                            Log.d("ProfileFragment", "No se encontraron datos para el usuario actual");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileFragment", "Error al obtener datos del usuario: " + e.getMessage());
                    });
        }

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                            selectedBitmap = bitmap;
                            ivNewPhoto.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Error selecting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchUserPosts() {
        ProgressUtils.showProgress();

        String currentIdUser = auth.getCurrentUser().getUid();
        PostUserAdapter postsUserAdapter = new PostUserAdapter(getContext(), postList);

        db.collection("posts")
                .whereEqualTo("idUser", currentIdUser)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        PostModel post = document.toObject(PostModel.class);
                        postList.add(post);
                    }
                    postsUserAdapter.notifyDataSetChanged();
                    ProgressUtils.hideProgress();
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error al obtener los posts: " + e.getMessage());
                    ProgressUtils.hideProgress();
                });
    }

    private void showBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        MaterialButton editUser = new MaterialButton(getContext());
        editUser.setPadding(32, 32, 0, 32);
        ;
        editUser.setBackgroundColor(getResources().getColor(R.color.none));
        editUser.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_edit));
        editUser.setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        editUser.setText("Editar usuario");
        editUser.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        editUser.setTextColor(getResources().getColor(R.color.white));
        editUser.setOnClickListener(v -> {
            clTopDataUser.setVisibility(View.GONE);
            clEditUser.setVisibility(View.VISIBLE);
            bottomSheetDialog.dismiss();
        });

        MaterialButton deleteUser = new MaterialButton(getContext());
        deleteUser.setPadding(32, 32, 8, 32);
        deleteUser.setBackgroundColor(getResources().getColor(R.color.none));
        deleteUser.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_delete));
        deleteUser.setIconTint(ColorStateList.valueOf(getResources().getColor(com.google.android.material.R.color.design_default_color_error)));
        deleteUser.setText("Eliminar cuenta");
        deleteUser.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        deleteUser.setTextColor(getResources().getColor(com.google.android.material.R.color.design_default_color_error));
        deleteUser.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Eliminar cuenta")
                    .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Se eliminará toda tu información. \n \nPara eliminar la cuenta vamos a requerir tu contraseña")
                    .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clTopDataUser.setVisibility(View.GONE);
                            rvPostUser.setVisibility(View.GONE);
                            ProgressUtils.showProgress();

                            authManager.deleteUser(new AuthManager.DeleteUserCallback() {
                                @Override
                                public void onSuccess() {
                                    ProgressUtils.hideProgress();
                                }

                                @Override
                                public void onFailure(String error) {
                                    ProgressUtils.hideProgress();
                                    Toast.makeText(getContext(), "Error al eliminar cuenta: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
            bottomSheetDialog.dismiss();
        });

        layout.addView(editUser);
        layout.addView(deleteUser);

        bottomSheetDialog.setContentView(layout);
        bottomSheetDialog.show();
    }

    private void showOptions() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.getProviderData().contains(GoogleAuthProvider.PROVIDER_ID)) {
            tilPassword.setVisibility(View.GONE);
            tilNewPassword.setVisibility(View.GONE);
            tilConfirm.setVisibility(View.GONE);
        } else {
            if (arePasswordFieldsVisible) {
                tilPassword.setVisibility(View.VISIBLE);
                tilNewPassword.setVisibility(View.VISIBLE);
                tilConfirm.setVisibility(View.VISIBLE);
            } else {
                tilPassword.setVisibility(View.GONE);
                tilNewPassword.setVisibility(View.GONE);
                tilConfirm.setVisibility(View.GONE);
            }
        }
    }

    private void updateUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        String newEmail = etEmail.getText().toString();
        String newUserName = etUserName.getText().toString();
        Bitmap newUserPhoto = selectedBitmap;
        String newPassword = etNewPassword.getText().toString();

        ProgressUtils.showProgress();

        if (newPassword.isEmpty()) {
            // Actualizar solo nombre y foto
            authManager.updateUserData(newUserName, newUserPhoto, newEmail, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(requireContext(), "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
                    ProgressUtils.hideProgress();
                    goToMain();
                } else {
                    ProgressUtils.hideProgress();
                    Toast.makeText(requireContext(), "Error al actualizar datos: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (currentUser != null) {
                // Actualizar la contraseña primero
                authManager.updatePassword(newPassword, passwordUpdateTask -> {
                    if (passwordUpdateTask.isSuccessful()) {
                        // Si la actualización de la contraseña fue exitosa, actualizar nombre y foto
                        authManager.updateUserData(newUserName, newUserPhoto, newEmail, userDataUpdateTask -> {
                            if (userDataUpdateTask.isSuccessful()) {
                                ProgressUtils.hideProgress();
                                goToMain();
                                Toast.makeText(requireContext(), "Datos y contraseña actualizados correctamente", Toast.LENGTH_SHORT).show();
                                // Actualización exitosa, puedes actualizar la UI o hacer otras acciones necesarias
                            } else {
                                ProgressUtils.hideProgress();
                                Toast.makeText(requireContext(), "Error al actualizar datos: " + userDataUpdateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        ProgressUtils.hideProgress();
                        Toast.makeText(requireContext(), "Error al actualizar contraseña: " + passwordUpdateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void goToMain() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

}