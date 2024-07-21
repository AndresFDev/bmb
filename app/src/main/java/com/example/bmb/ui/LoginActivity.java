package com.example.bmb.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;

import com.example.bmb.data.ImageManager;
import com.example.bmb.R;
import com.example.bmb.auth.AuthManager;
import com.example.bmb.utils.ProgressUtils;
import com.example.bmb.utils.TextInputHelper;
import com.example.bmb.utils.TextInputValidator;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private AuthManager authManager;
    private ImageManager imageManager;
    private Bitmap selectedBitmap;
    private MaterialButton btnForgotPassword, btnPhoto, btnEnter, btnRegister, btnSignUp;
    private TextInputLayout tilUserName, tilEmail, tilPassword, tilConfirm;
    private TextInputEditText etUserName, etEmail, etPassword, etConfirm;
    private ExtendedFloatingActionButton btnGoogle;
    private MaterialTextView tvNameLogo, msgReg1, msgReg2;
    private MaterialCardView cvPhoto;
    private ImageView imgLogo, ivPhoto;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        authManager = new AuthManager(this);
        imageManager = new ImageManager(this);
        ProgressUtils.initProgress(this, findViewById(android.R.id.content));

        mAuthListener = this::handleAuthStateChange;

        setupUI();
        setupButtonClickListeners();
    }

    private void handleAuthStateChange(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            fetchUserData(user);
        } else {
            setupUIForSignIn();
        }
    }

    private void fetchUserData(FirebaseUser user) {
        AuthManager authManager = new AuthManager(this);
        ProgressUtils.showProgress();
        authManager.fetchUserData(user, new AuthManager.OnUserDataFetchListener() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                ProgressUtils.hideProgress();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("userData", new HashMap<>(userData));
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                ProgressUtils.hideProgress();
                Toast.makeText(LoginActivity.this, "Error al obtener datos del usuario: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUIForSignIn() {
        cvPhoto.setVisibility(View.GONE);
        tilUserName.setVisibility(View.GONE);
        tilConfirm.setVisibility(View.GONE);
        msgReg1.setText(R.string.msgReg1);
        msgReg2.setText(R.string.optionLogin);
        btnRegister.setVisibility(View.GONE);
        btnEnter.setVisibility(View.VISIBLE);
        btnSignUp.setText(R.string.signUp);
    }


    private void setupUI() {
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        cvPhoto = findViewById(R.id.cvPhoto);
        ivPhoto = findViewById(R.id.ivPhoto);
        btnPhoto = findViewById(R.id.btnPhoto);
        btnEnter = findViewById(R.id.btnEnter);
        btnRegister = findViewById(R.id.btnRegister);
        btnSignUp = findViewById(R.id.btnSignUp);
        tilUserName = findViewById(R.id.tilUserName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirm);
        etUserName = findViewById(R.id.etUserName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnGoogle = findViewById(R.id.btnGoogle);
        imgLogo = findViewById(R.id.imgLogo);
        tvNameLogo = findViewById(R.id.tvNameLogo);
        msgReg1 = findViewById(R.id.msgReg1);
        msgReg2 = findViewById(R.id.msgReg2);

        tvNameLogo.setTextColor(ContextCompat.getColor(this, R.color.bgColor));

        setupTextChangeListeners();
    }

    private void setupTextChangeListeners() {
        TextInputHelper.setupTextChangeListener(etUserName, tilUserName);
        TextInputHelper.setupTextChangeListener(etEmail, tilEmail);
        TextInputHelper.setupTextChangeListener(etPassword, tilPassword);
        TextInputHelper.setupTextChangeListener(etConfirm, tilConfirm);
    }

    private void setupButtonClickListeners() {
        btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        btnPhoto.setOnClickListener(v -> selectPhoto());
        btnEnter.setOnClickListener(v -> signInWithEmail());
        btnRegister.setOnClickListener(v -> signUpWithEmail());
        btnSignUp.setOnClickListener(v -> toggleSignUpView());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
    }

    private void showForgotPasswordDialog() {
        TextInputLayout tilEmailReset = new TextInputLayout(this);
        tilEmailReset.setHint("Correo Electrónico");
        TextInputEditText etEmailReset = new TextInputEditText(this);
        tilEmailReset.setPadding(32, 8, 32, 8);
        tilEmailReset.addView(etEmailReset);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Recuperar Contraseña")
                .setView(tilEmailReset)
                .setPositiveButton("Enviar", (dialog, which) -> sendPasswordResetEmail(etEmailReset.getText().toString()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su correo electrónico", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error al enviar correo de recuperación", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void selectPhoto() {
        imageManager.selectImage();
    }

    private void signInWithEmail() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (validateSignInFields(email, password)) {
            ProgressUtils.showProgress();
            AuthManager authManager = new AuthManager(this);
            authManager.signInWithEmail(email, password, new AuthManager.OnSignInListener() {
                @Override
                public void onSignInSuccess(FirebaseUser user) {
                    ProgressUtils.hideProgress();
                    handleAuthStateChange(mAuth);
                }

                @Override
                public void onSignInFailure(String errorMessage) {
                    ProgressUtils.hideProgress();
                    Toast.makeText(LoginActivity.this, "Error al iniciar sesión: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void signUpWithEmail() {
        Bitmap userPhoto = selectedBitmap;
        String userName = etUserName.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();

        if (validateSignUpFields(userName, email, password, confirm)) {
            ProgressUtils.showProgress();
            AuthManager authManager = new AuthManager(this);
            authManager.signUpWithEmail(userPhoto, userName, email, password, new AuthManager.OnSignUpListener() {
                @Override
                public void onSignUpSuccess(FirebaseUser user) {
                    ProgressUtils.hideProgress();
                    Toast.makeText(LoginActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                    handleAuthStateChange(mAuth);
                }

                @Override
                public void onSignUpFailure(String errorMessage) {
                    ProgressUtils.hideProgress();
                    Toast.makeText(LoginActivity.this, "Error al registrar: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean validateSignInFields(String email, String password) {
        boolean valid = true;
        if (!TextInputValidator.isValidEmail(etEmail, tilEmail)) {
            valid = false;
        }
        if (!TextInputValidator.isValidPassword(etPassword, tilPassword)) {
            valid = false;
        }
        return valid;
    }

    private boolean validateSignUpFields(String userName, String email, String password, String confirm) {
        boolean valid = true;
        if (!TextInputValidator.isValidUsername(etUserName, tilUserName)) {
            valid = false;
        }
        if (!TextInputValidator.isValidEmail(etEmail, tilEmail)) {
            valid = false;
        }
        if (!TextInputValidator.isValidPassword(etPassword, tilPassword)) {
            valid = false;
        }
        if (!TextInputValidator.doPasswordsMatch(etPassword, etConfirm, tilPassword, tilConfirm)) {
            valid = false;
        }
        return valid;
    }

    private void toggleSignUpView() {
        String statusText = btnSignUp.getText().toString();
        if (statusText.equals(getString(R.string.signUp))) {
            cvPhoto.setVisibility(View.VISIBLE);
            tilUserName.setVisibility(View.VISIBLE);
            tilConfirm.setVisibility(View.VISIBLE);
            btnForgotPassword.setVisibility(View.GONE);
            msgReg1.setText(R.string.msgReg1_v);
            msgReg2.setText(R.string.msgReg2_r);
            btnRegister.setVisibility(View.VISIBLE);
            btnEnter.setVisibility(View.GONE);
            btnSignUp.setText(R.string.login);
        } else if (statusText.equals(getString(R.string.login))) {
            cvPhoto.setVisibility(View.GONE);
            tilUserName.setVisibility(View.GONE);
            tilConfirm.setVisibility(View.GONE);
            btnForgotPassword.setVisibility(View.VISIBLE);
            msgReg1.setText(R.string.msgReg1);
            msgReg2.setText(R.string.optionLogin);
            btnRegister.setVisibility(View.GONE);
            btnEnter.setVisibility(View.VISIBLE);
            btnSignUp.setText(R.string.signUp);
        }
    }

    private void signInWithGoogle() {
        GoogleSignInClient googleSignInClient = authManager.configureGoogleSignIn();
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }

        imageManager.onImageSelected(requestCode, resultCode, data, bitmap -> {
            selectedBitmap = bitmap;
            ivPhoto.setImageBitmap(bitmap);
        }, e -> Toast.makeText(LoginActivity.this, "Error al seleccionar la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            AuthManager authManager = new AuthManager(this);
            ProgressUtils.showProgress();
            authManager.signInWithGoogle(account, new AuthManager.OnSignInListener() {
                @Override
                public void onSignInSuccess(FirebaseUser user) {
                    ProgressUtils.hideProgress();
                    handleAuthStateChange(mAuth);
                }

                @Override
                public void onSignInFailure(String errorMessage) {
                    ProgressUtils.hideProgress();
                    Toast.makeText(LoginActivity.this, "Error al iniciar sesión con Google: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (ApiException e) {
            Log.w("LoginActivity", "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imageManager.onRequestPermissionsResult(requestCode, grantResults, null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }

    }
}