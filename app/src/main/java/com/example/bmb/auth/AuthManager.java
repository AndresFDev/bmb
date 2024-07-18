package com.example.bmb.auth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bmb.R;
import com.example.bmb.data.ImageManager;
import com.example.bmb.ui.LoginActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
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
    private GoogleSignInClient mGoogleSignInClient;

    private static final int RC_SIGN_IN = 9001; // Código de solicitud para el inicio de sesión con Google

    public AuthManager(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        configureGoogleSignIn();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public void signInWithEmail(String email, String password, FirebaseAuth.AuthStateListener authStateListener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        authStateListener.onAuthStateChanged(mAuth);
                    } else {
                        Toast.makeText(context, "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void signUpWithEmail(Bitmap userPhoto, String userName, String email, String password, FirebaseAuth.AuthStateListener authStateListener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (userPhoto != null) {
                                // Sube la imagen primero y luego guarda los datos del usuario
                                ImageManager imgManager = new ImageManager(context);
                                imgManager.uploadImageToFirebase(userPhoto, imageUrl -> {
                                    saveUserData(user.getUid(), email, userName, imageUrl, password);
                                    authStateListener.onAuthStateChanged(mAuth); // Notifica éxito en el registro
                                    Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                }, e -> {
                                    Toast.makeText(context, "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                // No se proporcionó imagen, guarda los datos del usuario sin imagen
                                saveUserData(user.getUid(), email, userName, null, password);
                                authStateListener.onAuthStateChanged(mAuth); // Notifica el cambio de estado de autenticación
                                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Error al obtener el usuario después del registro", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        ((AppCompatActivity) context).startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void firebaseAuthWithGoogle(String idToken, FirebaseAuth.AuthStateListener authStateListener) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Uri photoUrl = user.getPhotoUrl();
                        String photoUrlString = (photoUrl != null) ? photoUrl.toString() : "";

                        if (user != null) {
                            // Registro en Firestore si es la primera vez que inicia sesión con Google
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                            if (isNewUser) {

                                saveUserData(user.getUid(), user.getEmail(), user.getDisplayName(), photoUrlString, "Google Authenticated");
                            }
                            authStateListener.onAuthStateChanged(mAuth);
                            Toast.makeText(context, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Error al obtener información del usuario", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Error al iniciar sesión con Google: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    if (task.isSuccessful()) {
                        authStateListener.onAuthStateChanged(mAuth);
                    } else {
                        Toast.makeText(context, "Error al iniciar sesión con Google: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public interface DeleteUserCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public void deleteUser(DeleteUserCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String userId = user.getUid(); // Obtén el ID del usuario actual

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Eliminar el documento del usuario en Firestore
            db.collection("users").document(userId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("AuthManager", "Documento de usuario eliminado correctamente");

                        // Después de eliminar el documento, eliminar el usuario de Firebase Authentication
                        revokeGoogleAccess(user);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AuthManager", "Error al eliminar documento de usuario", e);
                        callback.onFailure(e.getMessage());
                    });
        } else {
            Log.e("AuthManager", "No se pudo obtener el usuario actual para eliminar");
            callback.onFailure("No se pudo obtener el usuario actual para eliminar");
        }
    }

    private void revokeGoogleAccess(FirebaseUser user) {
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build());

        googleSignInClient.revokeAccess()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Después de revocar el acceso, eliminar el usuario de Firebase Authentication
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

    public interface OnUserDataFetchListener {
        void onSuccess(Map<String, Object> userData);

        void onFailure(String errorMessage);
    }


    public void updateUserData(String userName, Bitmap newPhoto, String newPassword, OnCompleteListener<Void> onCompleteListener) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            if (isGoogleSignInUser(user)) {
                // Usuario autenticado con Google, no permite la actualización de email y contraseña
                if (newPhoto != null) {
                    // Actualizar solo nombre y foto
                    updateUserNameAndPhoto(user.getUid(), userName, newPhoto, onCompleteListener);
                } else {
                    // Solo actualiza el nombre si no hay nueva foto
                    updateUserNameAndPhoto(user.getUid(), userName, null, onCompleteListener);
                }
            } else {
                // Usuario autenticado con email/password
                if (newPassword != null && !newPassword.isEmpty()) {
                    // Actualiza la contraseña
                    updatePassword(newPassword, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Error al actualizar la contraseña: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        // Luego de actualizar la contraseña, actualizar nombre y foto
                        updateUserNameAndPhoto(user.getUid(), userName, newPhoto, onCompleteListener);
                    });
                } else {
                    // Solo actualiza el nombre y la foto
                    updateUserNameAndPhoto(user.getUid(), userName, newPhoto, onCompleteListener);
                }
            }
        } else {
            Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserNameAndPhoto(String userId, String userName, Bitmap newPhoto, OnCompleteListener<Void> onCompleteListener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", userName);

        if (newPhoto != null) {
            // Si se proporciona una nueva imagen, subir y actualizar la URL de la imagen
            ImageManager imgManager = new ImageManager(context);
            imgManager.uploadImageToFirebase(newPhoto, imageUrl -> {
                updates.put("userPhoto", imageUrl); // Actualiza la nueva URL de la imagen
                db.collection("users").document(userId)
                        .update(updates)
                        .addOnCompleteListener(onCompleteListener);
            }, e -> {
                Toast.makeText(context, "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // Si no se proporciona una nueva imagen, mantener la URL de la imagen existente
            db.collection("users").document(userId)
                    .update(updates)
                    .addOnCompleteListener(onCompleteListener);
        }
    }


    public void updatePassword(String newPassword, OnCompleteListener<Void> onCompleteListener) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            if (isGoogleSignInUser(user)) {
                // No permite la actualización de contraseña para usuarios autenticados con Google
                Toast.makeText(context, "No se puede cambiar la contraseña de usuarios autenticados con Google", Toast.LENGTH_SHORT).show();
            } else {
                // Permite la actualización de contraseña para usuarios autenticados con email/password
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

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
}