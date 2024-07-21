package com.example.bmb.utils;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class TextInputHelper {

    public static void setupTextChangeListener(TextInputEditText editText, TextInputLayout inputLayout, VisibilityHandler visibilityHandler) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    inputLayout.setErrorEnabled(false);
                    inputLayout.setError(null);
                }

                // Notify visibilityHandler if it's provided
                if (visibilityHandler != null) {
                    visibilityHandler.onTextChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public static void setupTextChangeListener(TextInputEditText editText, TextInputLayout inputLayout) {
        setupTextChangeListener(editText, inputLayout, null);
    }

    public interface VisibilityHandler {
        void onTextChanged();
    }
}