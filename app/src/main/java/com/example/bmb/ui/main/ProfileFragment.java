package com.example.bmb.ui.main;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
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
import com.example.bmb.auth.AuthManager;
import com.example.bmb.utils.ProgressUtils;
import com.example.bmb.adapters.PostUserAdapter;
import com.example.bmb.data.ImageManager;
import com.example.bmb.data.models.ProfileViewModel;
import com.example.bmb.ui.LoginActivity;
import com.example.bmb.utils.StrokeTextView;
import com.example.bmb.utils.TextInputValidator;
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
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private AuthManager authManager;
    private FirebaseFirestore db;
    private ConstraintLayout clEmpty;
    private ImageView ivPhoto, ivNewPhoto;
    private ImageButton btnUserSettings, btnClose;
    private StrokeTextView tvUserName;
    private ConstraintLayout clTopDataUser, clEditUser;
    private TextInputLayout tilEmail, tilPassword, tilNewPassword, tilConfirm;
    private TextInputEditText etUserName, etEmail, etPassword, etNewPassword, etConfirm;
    private MaterialButton btnPhoto, btnShow, btnSave;
    private RecyclerView rvPostUser;
    private ProfileViewModel profileViewModel;
    private ImageManager imageManager;
    private Bitmap selectedBitmap;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private boolean arePasswordFieldsVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(getContext());
        db = FirebaseFirestore.getInstance();
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        imageManager = new ImageManager(getActivity());
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                            selectedBitmap = bitmap;
                            ivNewPhoto.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Error seleccionando imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupRecyclerView();
        setupButtonListeners();
        ProgressUtils.initProgress(getContext(), (ViewGroup) view);
        togglePasswordFieldsVisibility();
        return view;
    }

    private void initViews(View view) {
        clEmpty = view.findViewById(R.id.clEmpty);
        ivPhoto = view.findViewById(R.id.ivPhoto);
        ivNewPhoto = view.findViewById(R.id.ivNewPhoto);
        tvUserName = view.findViewById(R.id.tvUserName);
        etUserName = view.findViewById(R.id.etUserName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirm = view.findViewById(R.id.etConfirm);
        clTopDataUser = view.findViewById(R.id.clTopDataUser);
        clEditUser = view.findViewById(R.id.clEditUser);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilNewPassword = view.findViewById(R.id.tilNewPassword);
        tilConfirm = view.findViewById(R.id.tilConfirm);
        btnUserSettings = view.findViewById(R.id.btnUserSettings);
        btnClose = view.findViewById(R.id.btnClose);
        btnPhoto = view.findViewById(R.id.btnPhoto);
        btnShow = view.findViewById(R.id.btnShow);
        btnSave = view.findViewById(R.id.btnSave);
        rvPostUser = view.findViewById(R.id.rvPostUser);
    }

    private void setupRecyclerView() {
        rvPostUser.setLayoutManager(new CarouselLayoutManager());
        PostUserAdapter postsUserAdapter = new PostUserAdapter(getContext(), new ArrayList<>());
        rvPostUser.setAdapter(postsUserAdapter);

        profileViewModel.getPostList().observe(getViewLifecycleOwner(), posts -> {
            postsUserAdapter.updatePosts(posts);

            if (posts.isEmpty()) {
                clEmpty.setVisibility(View.VISIBLE);
                rvPostUser.setVisibility(View.GONE);
            } else {
                clEmpty.setVisibility(View.GONE);
                rvPostUser.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupButtonListeners() {
        btnUserSettings.setOnClickListener(v -> {
            if (!isCurrentUserGoogleUser()) {
                togglePasswordFieldsVisibility();
                btnUserSettings.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_settings, getContext().getTheme()));
            } else {
                tilEmail.setVisibility(View.GONE);
                tilPassword.setVisibility(View.GONE);
                tilNewPassword.setVisibility(View.GONE);
                tilConfirm.setVisibility(View.GONE);
                btnShow.setVisibility(View.GONE);
                btnUserSettings.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_settings, getContext().getTheme()));
            }
            showBottomSheet();
        });

        btnPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnShow.setOnClickListener(v -> togglePasswordFieldsVisibility());
        btnClose.setOnClickListener(v -> toggleEditUserVisibility(false));
        btnSave.setOnClickListener(v -> updateUser());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel.getCurrentUser().observe(getViewLifecycleOwner(), currentUser -> {
            if (currentUser != null) {
                profileViewModel.getUserData().observe(getViewLifecycleOwner(), userData -> {
                    updateUI(currentUser, userData);
                });
            } else {
                ProgressUtils.hideProgress();
            }
        });
    }

    private void updateUI(FirebaseUser currentUser, Map<String, Object> userData) {
        ProgressUtils.showProgress();

        if (userData != null) {
            String userPhotoUrl = (String) userData.get("userPhoto");
            String userName = (String) userData.get("name");
            String userEmail = (String) userData.get("email");

            if (userName == null) {
                userName = "";
            }

            tvUserName.setText(userName);
            etUserName.setText(userName);
            etEmail.setText(userEmail);

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

            ProgressUtils.hideProgress();
            return;
        }

        if (currentUser == null) {
            Log.e("ProfileFragment", "currentUser es null.");
            ProgressUtils.hideProgress();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userPhotoUrl = documentSnapshot.getString("userPhoto");
                        String userName = documentSnapshot.getString("name");

                        if (userName == null) {
                            userName = "";
                        }

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
                    } else {
                        Log.d("ProfileFragment", "No se encontraron datos para el usuario actual");
                    }
                    ProgressUtils.hideProgress();
                })
                .addOnFailureListener(e -> {
                    ProgressUtils.hideProgress();
                    Log.e("ProfileFragment", "Error al obtener datos del usuario: " + e.getMessage());
                });
    }


    private void togglePasswordFieldsVisibility() {
        arePasswordFieldsVisible = !arePasswordFieldsVisible;
        tilPassword.setVisibility(arePasswordFieldsVisible ? View.VISIBLE : View.GONE);
        tilNewPassword.setVisibility(arePasswordFieldsVisible ? View.VISIBLE : View.GONE);
        tilConfirm.setVisibility(arePasswordFieldsVisible ? View.VISIBLE : View.GONE);
        btnShow.setText(arePasswordFieldsVisible ? "Ocultar" : "Mostrar");
    }

    private void toggleEditUserVisibility(boolean show) {
        clTopDataUser.setVisibility(show ? View.GONE : View.VISIBLE);
        clEditUser.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showBottomSheet() {
        btnUserSettings.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_settings_selected, getContext().getTheme()));

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_layout, null);
        LinearLayout bottomSheetContent = sheetView.findViewById(R.id.llButtonSheet);

        MaterialButton editUser = bottomSheetContent.findViewById(R.id.btnOptionOne);
        editUser.setText("Editar usuario");
        editUser.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_edit, getContext().getTheme()));
        editUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleEditUserVisibility(true);
                bottomSheetDialog.dismiss();
            }
        });

        MaterialButton deleteUser = bottomSheetContent.findViewById(R.id.btnOptionTwo);
        deleteUser.setText("Eliminar usuario");
        deleteUser.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete, getContext().getTheme()));
        deleteUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Eliminar cuenta")
                        .setMessage("¿Estás seguro de que quieres eliminar tu cuenta? Se eliminará toda tu información.")
                        .setPositiveButton("Eliminar", (dialog, which) -> deleteUserAccount())
                        .setNegativeButton("Cancelar", null)
                        .show();
                bottomSheetDialog.dismiss();
            }
        });

        deleteUser.setIconTint(ColorStateList.valueOf(getResources().getColor(com.google.android.material.R.color.design_default_color_error)));
        deleteUser.setTextColor(getResources().getColor(com.google.android.material.R.color.design_default_color_error));

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private boolean isCurrentUserGoogleUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            for (UserInfo userInfo : currentUser.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(userInfo.getProviderId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateUser() {
        ProgressUtils.showProgress();
        String name = etUserName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        boolean isGoogleUser = isCurrentUserGoogleUser();
        boolean emailChanged = !email.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail());


        if (!TextInputValidator.isValidEmail(etEmail, tilEmail)) {
            ProgressUtils.hideProgress();
            return;
        }

        if (!newPassword.isEmpty() && !TextInputValidator.isValidPassword(etPassword, tilPassword)) {
            ProgressUtils.hideProgress();
            return;
        }

        if (!newPassword.isEmpty()) {
            if (!TextInputValidator.isValidPassword(etNewPassword, tilNewPassword)) {
                ProgressUtils.hideProgress();
                return;
            }

            if (!TextInputValidator.doPasswordsMatch(etNewPassword, etConfirm, tilNewPassword, tilConfirm)) {
                ProgressUtils.hideProgress();
                return;
            }
        }

        if (isGoogleUser && (emailChanged || !password.isEmpty())) {
            ProgressUtils.hideProgress();
            Toast.makeText(getContext(), "No se puede cambiar el correo electrónico o la contraseña de un usuario autenticado con Google.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGoogleUser) {
            authManager.updateUserData(name, selectedBitmap, null, new AuthManager.UpdateUserCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                    toggleEditUserVisibility(false);
                    ProgressUtils.hideProgress();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Error al actualizar el perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    ProgressUtils.hideProgress();
                }
            });
        } else if (!newPassword.isEmpty()) {
            if (newPassword.equals(confirm)) {
                reauthenticateUserAndSaveData(password, name, newPassword);
            } else {
                Toast.makeText(getContext(), "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
                ProgressUtils.hideProgress();
            }
        } else {
            authManager.updateUserData(name, selectedBitmap, null, new AuthManager.UpdateUserCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                    toggleEditUserVisibility(false);
                    ProgressUtils.hideProgress();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Error al actualizar el perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    ProgressUtils.hideProgress();
                }
            });
        }
    }

    private void reauthenticateUserAndSaveData(String password, String name, String newPassword) {
        ProgressUtils.showProgress();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                authManager.updateUserData(name, selectedBitmap, newPassword, new AuthManager.UpdateUserCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(getContext(), "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show();
                                        toggleEditUserVisibility(false);
                                        ProgressUtils.hideProgress();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(getContext(), "Error al actualizar el perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        ProgressUtils.hideProgress();
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                ProgressUtils.hideProgress();
                                Toast.makeText(getContext(), "Error al actualizar la contraseña: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    ProgressUtils.hideProgress();
                    Toast.makeText(getContext(), "Error al reautenticar al usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteUserAccount() {
        ProgressUtils.showProgress();
        authManager.deleteUser(new AuthManager.DeleteUserCallback() {
            @Override
            public void onSuccess() {
                ProgressUtils.hideProgress();
                Toast.makeText(getContext(), "Cuenta eliminada correctamente.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getContext(), LoginActivity.class));
                getActivity().finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                ProgressUtils.hideProgress();
                Toast.makeText(getContext(), "Error al eliminar la cuenta: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}