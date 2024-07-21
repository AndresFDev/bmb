package com.example.bmb.auth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.example.bmb.R;
import com.example.bmb.data.ImageManager;
import com.example.bmb.ui.LoginActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthManager {

    private Context context;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    public AuthManager(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        configureGoogleSignIn();
    }

    public GoogleSignInClient configureGoogleSignIn() {
        if (googleSignInClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);
        }
        return googleSignInClient;
    }

    public interface OnSignInListener {
        void onSignInSuccess(FirebaseUser user);
        void onSignInFailure(String error);
    }

    public interface OnSignUpListener {
        void onSignUpSuccess(FirebaseUser user);
        void onSignUpFailure(String error);
    }

    public interface DeleteUserCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface UpdateUserCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnUserDataFetchListener {
        void onSuccess(Map<String, Object> userData);
        void onFailure(String errorMessage);
    }

    public void signInWithEmail(String email, String password, OnSignInListener listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            listener.onSignInSuccess(user);
                        } else {
                            listener.onSignInFailure("No se pudo obtener el usuario");
                        }
                    } else {
                        listener.onSignInFailure("Error al iniciar sesión: " + task.getException().getMessage());
                    }
                });
    }

    public void signUpWithEmail(Bitmap userPhoto, String userName, String email, String password, OnSignUpListener listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (userPhoto != null) {
                                ImageManager imgManager = new ImageManager(context);
                                imgManager.uploadImageToFirebase(userPhoto, imageUrl -> {
                                    saveUserData(user.getUid(), email, userName, imageUrl, password);
                                    listener.onSignUpSuccess(user);
                                }, e -> {
                                    listener.onSignUpFailure("Error al subir la imagen: " + e.getMessage());
                                });
                            } else {
                                saveUserData(user.getUid(), email, userName, null, password);
                                listener.onSignUpSuccess(user);
                            }
                        } else {
                            listener.onSignUpFailure("Error al obtener el usuario después del registro");
                        }
                    } else {
                        listener.onSignUpFailure("Error al registrar: " + task.getException().getMessage());
                    }
                });
    }

    public void signInWithGoogle(GoogleSignInAccount account, OnSignInListener listener) {
        String idToken = account.getIdToken();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            listener.onSignInSuccess(user);
                        } else {
                            listener.onSignInFailure("No se pudo obtener el usuario");
                        }
                    } else {
                        listener.onSignInFailure("Error al iniciar sesión con Google: " + task.getException().getMessage());
                    }
                });
    }

    public void deleteUser(DeleteUserCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("Usuario no esta logeado");
            return;
        }

        if (isGoogleSignInUser(currentUser)) {
            revokeGoogleAccess(currentUser);
        }

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> currentUser.delete()
                        .addOnSuccessListener(aVoid1 -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void revokeGoogleAccess(FirebaseUser user) {
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());

        googleSignInClient.revokeAccess()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("AuthManager", "Usuario eliminado correctamente de Firebase Authentication");
                                    signOut();
                                })
                                .addOnFailureListener(e -> {
                                    if (e instanceof FirebaseAuthInvalidUserException) {
                                        Log.e("AuthManager", "El usuario ya fue eliminado de Firebase Authentication");
                                    } else {
                                        Log.e("AuthManager", "Error al eliminar usuario de Firebase Authentication", e);
                                    }
                                });
                    } else {
                        Log.e("AuthManager", "Error al revocar el acceso de Google", task.getException());
                    }
                });
    }

    public void saveUserData(String userId, String email, String userName, String userPhoto, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("email", email);
        userData.put("name", userName);
        userData.put("userPhoto", userPhoto);
        userData.put("password", password);

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("UserDTA", "Datos del usuario guardados correctamente en Firestore"))
                .addOnFailureListener(e -> Log.w("UserDTA", "Error al guardar datos del usuario en Firestore", e));
    }

    public void fetchUserData(FirebaseUser user, OnUserDataFetchListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> userData = documentSnapshot.getData();
                        listener.onSuccess(userData);
                    } else {
                        listener.onFailure("No se encontro datos de usuario");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    public void updateUserData(String userName, Bitmap newPhoto, String newPassword, UpdateUserCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            if (isGoogleSignInUser(user)) {
                if (newPhoto != null) {
                    updateUserNameAndPhoto(user.getUid(), userName, newPhoto, callback);
                } else {
                    updateUserNameAndPhoto(user.getUid(), userName, null, callback);
                }
            } else {
                if (newPassword != null && !newPassword.isEmpty()) {
                    updatePassword(newPassword, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show();
                            updateUserNameAndPhoto(user.getUid(), userName, newPhoto, callback);
                        } else {
                            Toast.makeText(context, "Error al actualizar la contraseña: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            callback.onFailure(task.getException());
                        }
                    });
                } else {
                    updateUserNameAndPhoto(user.getUid(), userName, newPhoto, callback);
                }
            }
        } else {
            Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
            callback.onFailure(new Exception("Usuario no encontrado"));
        }
    }

    private void updateUserNameAndPhoto(String userId, String userName, Bitmap newPhoto, UpdateUserCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", userName);

        if (newPhoto != null) {
            ImageManager imgManager = new ImageManager(context);
            imgManager.uploadImageToFirebase(newPhoto, imageUrl -> {
                updates.put("userPhoto", imageUrl);
                db.collection("users").document(userId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onFailure(e));
            }, e -> {
                Toast.makeText(context, "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                callback.onFailure(e);
            });
        } else {
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e));
        }
    }

    public void updatePassword(String newPassword, OnCompleteListener<Void> onCompleteListener) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            if (isGoogleSignInUser(user)) {
                Toast.makeText(context, "No se puede cambiar la contraseña de usuarios autenticados con Google", Toast.LENGTH_SHORT).show();
            } else {
                user.updatePassword(newPassword)
                        .addOnCompleteListener(onCompleteListener);
            }
        } else {
            Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isGoogleSignInUser(FirebaseUser user) {
        return user != null && user.getProviderData().stream()
                .anyMatch(provider -> GoogleAuthProvider.PROVIDER_ID.equals(provider.getProviderId()));
    }


    public void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}