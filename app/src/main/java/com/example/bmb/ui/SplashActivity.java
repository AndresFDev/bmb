package com.example.bmb.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bmb.R;
import com.example.bmb.auth.AuthManager;
import com.example.bmb.utils.NetworkUtils;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ImageView imgLogo;
    private MaterialTextView tvNameLogo, tvSlogan;
    private static FirebaseAuth mAuth;


    private static final int SPLASH_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        imgLogo = findViewById(R.id.imgLogo);
        tvNameLogo = findViewById(R.id.tvNameLogo);
        tvSlogan = findViewById(R.id.tvSlogan);

        mAuth = FirebaseAuth.getInstance();

        if (NetworkUtils.isNetworkAvailable(this)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user != null) {
                        AuthManager authManager = new AuthManager(SplashActivity.this);
                        authManager.fetchUserData(user, new AuthManager.OnUserDataFetchListener() {
                            @Override
                            public void onSuccess(Map<String, Object> userData) {
                                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                intent.putExtra("userData", new HashMap<>(userData));
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(SplashActivity.this, "Error al obtener datos del usuario: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }, SPLASH_DELAY);
        } else {
            NetworkUtils.showNoInternetDialog(this);
        }

    }
}