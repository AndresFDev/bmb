package com.example.bmb.utils;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class TextInputValidator {

    public static boolean isValidEmail(TextInputEditText editText, TextInputLayout inputLayout) {
        String email = editText.getText().toString().trim();
        if (email.isEmpty()) {
            inputLayout.setError("Por favor, ingrese su correo electrónico");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputLayout.setError("Correo electrónico no válido");
            return false;
        } else {
            inputLayout.setErrorEnabled(false);
            return true;
        }
    }

    public static boolean isValidPassword(TextInputEditText editText, TextInputLayout inputLayout) {
        String password = editText.getText().toString().trim();
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";
        if (password.isEmpty()) {
            inputLayout.setError("Por favor, ingrese su contraseña");
            return false;
        } else if (!password.matches(passwordPattern)) {
            inputLayout.setError("La contraseña debe tener al menos 8 caracteres, incluyendo letras y números");
            return false;
        } else {
            inputLayout.setErrorEnabled(false);
            return true;
        }
    }

    public static boolean isValidUsername(TextInputEditText editText, TextInputLayout inputLayout) {
        String username = editText.getText().toString().trim();
        if (username.isEmpty()) {
            inputLayout.setError("Por favor, ingrese su nombre de usuario");
            return false;
        } else {
            inputLayout.setErrorEnabled(false);
            return true;
        }
    }

    public static boolean doPasswordsMatch(TextInputEditText passwordEditText, TextInputEditText confirmEditText, TextInputLayout passwordTextInput, TextInputLayout confirmTextInput) {
        String password = passwordEditText.getText().toString().trim();
        String confirm = confirmEditText.getText().toString().trim();
        if (!password.equals(confirm)) {
            passwordTextInput.setError("Las contraseñas no coinciden");
            confirmTextInput.setError("Las contraseñas no coinciden");
            return false;
        } else {
            passwordTextInput.setErrorEnabled(false);
            confirmTextInput.setErrorEnabled(false);
            return true;
        }
    }
}