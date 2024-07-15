package com.example.bmb.ui;

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
import com.example.bmb.data.AuthManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ImageManager imageManager;
    private Bitmap selectedBitmap;
    private MaterialButton btnPhoto, btnEnter, btnRegister, btnSignUp;
    private LinearLayoutCompat llSesion;
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

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                AuthManager authManager = new AuthManager(this);
                authManager.fetchUserData(user, new AuthManager.OnUserDataFetchListener() {
                    @Override
                    public void onSuccess(Map<String, Object> userData) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("userData", new HashMap<>(userData));
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(LoginActivity.this, "Error al obtener datos del usuario: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                imageManager = new ImageManager(this);
                setupUI();
                setupButtonClickListeners();
            }
        };
    }

    private void setupUI() {
        cvPhoto = findViewById(R.id.cvPhoto);
        ivPhoto = findViewById(R.id.ivPhoto);
        btnPhoto = findViewById(R.id.btnPhoto);
        btnEnter = findViewById(R.id.btnEnter);
        btnRegister = findViewById(R.id.btnRegister);
        btnSignUp = findViewById(R.id.btnSignUp);
        llSesion = findViewById(R.id.llSesion);
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

        // Configurar colores
        tvNameLogo.setTextColor(ContextCompat.getColor(this, R.color.bgColor));

        // Configurar TextChangedListeners para validar campos de entrada
        setupTextChangeListeners();
    }

    private void setupTextChangeListeners() {
        etUserName.addTextChangedListener(createTextWatcher(tilUserName));
        etEmail.addTextChangedListener(createTextWatcher(tilEmail));
        etPassword.addTextChangedListener(createTextWatcher(tilPassword));
        etConfirm.addTextChangedListener(createTextWatcher(tilConfirm));
    }

    private TextWatcher createTextWatcher(TextInputLayout inputLayout) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No necesario
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    inputLayout.setErrorEnabled(false);
                    inputLayout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No necesario
            }
        };
    }


    private void setupButtonClickListeners() {
        btnPhoto.setOnClickListener(v -> selectPhoto());
        btnEnter.setOnClickListener(v -> signInWithEmail());
        btnRegister.setOnClickListener(v -> signUpWithEmail());
        btnSignUp.setOnClickListener(v -> toggleSignUpView());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
    }

    private void selectPhoto() {
        imageManager.selectImage();
    }

    private void signInWithEmail() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        boolean isValid = true;

        if (email.isEmpty() && password.isEmpty()) {
            tilEmail.setErrorEnabled(true);
            tilPassword.setErrorEnabled(true);
            tilEmail.setError("Campo vacío");
            tilPassword.setError("Campo vacío");
            Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if (email.isEmpty()) {
            tilEmail.setErrorEnabled(true);
            tilEmail.setError("Por favor, ingrese su correo electrónico");
            isValid = false;
        } else if (password.isEmpty()) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Por favor, ingrese su contraseña");
            isValid = false;
        } else if (!email.contains("@") || !email.contains(".com")) {
            tilEmail.setError("Correo electrónico no válido");
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setError("La contraseña debe tener al menos 8 caracteres");
            isValid = false;
        }

        if (isValid) {
            AuthManager authManager = new AuthManager(this);
            authManager.signInWithEmail(email, password, mAuthListener);
        }
    }

    private void signUpWithEmail() {
        Bitmap userPhoto = selectedBitmap;
        String userName = etUserName.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirm.getText().toString();

        boolean isValid = true;

        if (userName.isEmpty() && email.isEmpty() && password.isEmpty() && confirmPassword.isEmpty()) {
            tilUserName.setErrorEnabled(true);
            tilEmail.setErrorEnabled(true);
            tilPassword.setErrorEnabled(true);
            tilConfirm.setErrorEnabled(true);
            tilUserName.setError("Campo vacío");
            tilEmail.setError("Campo vacío");
            tilPassword.setError("Campo vacío");
            tilConfirm.setError("Campo vacío");
            Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if (userName.isEmpty()) {
            tilUserName.setErrorEnabled(true);
            tilUserName.setError("Falta el nombre de usuario");
            isValid = false;
        } else if (email.isEmpty()) {
            tilEmail.setErrorEnabled(true);
            tilEmail.setError("Por favor, ingrese su correo electrónico");
            isValid = false;
        } else if (!email.contains("@") || !email.contains(".com")) {
            tilEmail.setErrorEnabled(true);
            tilEmail.setError("Por favor, ingrese un correo electrónico válido");
            isValid = false;
        } else if (password.isEmpty()) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Por favor, ingrese su contraseña");
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("La contraseña debe tener al menos 8 caracteres");
            isValid = false;
        } else if (confirmPassword.isEmpty()) {
            tilConfirm.setErrorEnabled(true);
            tilConfirm.setError("Confirme su contraseña");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirm.setErrorEnabled(true);
            tilConfirm.setError("Las contraseñas no coinciden");
            isValid = false;
        }

        if (isValid) {
            AuthManager authManager = new AuthManager(this);
            authManager.signUpWithEmail(userPhoto, userName, email, password, mAuthListener);
        }

    }

    private void toggleSignUpView() {
        String statusText = btnSignUp.getText().toString();
        if (statusText.equals(getString(R.string.signUp))) {
            cvPhoto.setVisibility(View.VISIBLE);
            tilUserName.setVisibility(View.VISIBLE);
            tilConfirm.setVisibility(View.VISIBLE);
            msgReg1.setText(R.string.msgReg1);
            msgReg2.setText(R.string.login);
            btnRegister.setVisibility(View.VISIBLE);
            llSesion.setVisibility(View.GONE);
            btnEnter.setVisibility(View.GONE);
            btnSignUp.setText(R.string.login);
        } else if (statusText.equals(getString(R.string.login))) {
            cvPhoto.setVisibility(View.GONE);
            tilUserName.setVisibility(View.GONE);
            tilConfirm.setVisibility(View.GONE);
            msgReg1.setText(R.string.msgReg1_v);
            msgReg2.setText(R.string.optionLogin);
            btnRegister.setVisibility(View.GONE);
            btnEnter.setVisibility(View.VISIBLE);
            llSesion.setVisibility(View.VISIBLE);
            btnSignUp.setText(R.string.signUp);
        }
    }

    private void signInWithGoogle() {
        AuthManager authManager = new AuthManager(this);
        authManager.signInWithGoogle();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imageManager.onRequestPermissionsResult(requestCode, grantResults, null);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthManager authManager = new AuthManager(this);
                authManager.firebaseAuthWithGoogle(account.getIdToken(), mAuthListener);
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "Fallo en el inicio de sesión con Google", e);
                Toast.makeText(this, "Error al iniciar sesión con Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        imageManager.onImageSelected(requestCode, resultCode, data, bitmap -> {
            selectedBitmap = bitmap;
            ivPhoto.setImageBitmap(bitmap);
        }, e -> Toast.makeText(LoginActivity.this, "Error al seleccionar la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Agregar el AuthStateListener al inicio de la actividad
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remover el AuthStateListener al detener la actividad para evitar fugas de memoria
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }

    }
}