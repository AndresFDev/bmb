package com.example.bmb.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageManager {

    private Context context;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;

    public ImageManager(Context context) {
        this.context = context;
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    public void selectImage() {
        if (checkAndRequestPermissions()) {
            openImageSelector();
        }
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissions = new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ((Activity) context).requestPermissions(permissions, REQUEST_CODE_STORAGE_PERMISSION);
            return false;
        }

        return true;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults, Intent data) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                openImageSelector();
            } else {
                Toast.makeText(context, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImageSelector() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST);
    }

    public void onImageSelected(int requestCode, int resultCode, Intent data, OnSuccessListener<Bitmap> onSuccessListener, OnFailureListener onFailureListener) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), filePath);
                onSuccessListener.onSuccess(bitmap);
            } catch (IOException e) {
                onFailureListener.onFailure(e);
            }
        }
    }

    public Task<Void> uploadImageToFirebase(Bitmap bitmap, OnSuccessListener<String> onSuccessListener, OnFailureListener onFailureListener) {
        long timestamp = System.currentTimeMillis();
        String fileName = "img_" + timestamp + ".webp";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, 50, baos); // Aquí puedes ajustar la calidad de compresión
        byte[] data = baos.toByteArray();

        StorageReference imageRef = storageRef.child("images/" + fileName);
        UploadTask uploadTask = imageRef.putBytes(data);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                onSuccessListener.onSuccess(imageUrl);
            }).addOnFailureListener(onFailureListener);
        }).addOnFailureListener(onFailureListener);
        return null;
    }
}