package com.example.bmb.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class ProgressUtils {

    private static FrameLayout progressContainer;
    private static CircularProgressIndicator progressIndicator;

    // Método estático para inicializar el ProgressIndicator
    public static void initProgress(Context context, ViewGroup rootView) {
        progressContainer = new FrameLayout(context);
        progressContainer.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black));
        progressContainer.getBackground().setAlpha(128); // Ajusta la opacidad del fondo

        progressIndicator = new CircularProgressIndicator(context);
        progressIndicator.setIndeterminate(true);
        progressIndicator.setVisibility(View.GONE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        params.gravity = android.view.Gravity.CENTER;

        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        progressContainer.addView(progressIndicator, params);
        progressContainer.setVisibility(View.GONE);

        if (rootView instanceof FrameLayout) {
            ((FrameLayout) rootView).addView(progressContainer, containerParams);
        }
    }

    public static void showProgress() {
        if (progressContainer != null) {
            progressContainer.setVisibility(View.VISIBLE);
            progressIndicator.setVisibility(View.VISIBLE);
        }
    }

    // Método estático para ocultar el ProgressIndicator
    public static void hideProgress() {
        if (progressContainer != null) {
            progressContainer.setVisibility(View.GONE);
            progressIndicator.setVisibility(View.GONE);
        }
    }
}