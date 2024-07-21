package com.example.bmb.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class NetworkUtils {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    public static void showNoInternetDialog(Context context, DialogInterface.OnClickListener retryListener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Sin Conexión")
                .setMessage("No hay conexión a Internet. Por favor, verifica tu conexión y vuelve a intentarlo.")
                .setPositiveButton("Reintentar", retryListener)
                .setCancelable(false)
                .show();
    }
}
