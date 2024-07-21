package com.example.bmb.utils;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


public class KeyboardVisibilityHelper {

    private final View rootView;
    private final InputMethodManager imm;
    private final OnKeyboardVisibilityListener listener;

    public interface OnKeyboardVisibilityListener {
        void onKeyboardVisibilityChanged(boolean isVisible);
    }

    public KeyboardVisibilityHelper(Context context, View rootView, OnKeyboardVisibilityListener listener) {
        this.rootView = rootView;
        this.imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        this.listener = listener;

        setupKeyboardVisibilityListener();
    }

    private void setupKeyboardVisibilityListener() {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean isKeyboardOpen = keypadHeight > screenHeight * 0.15;
            listener.onKeyboardVisibilityChanged(isKeyboardOpen);
        });
    }
}