package com.example.bmb.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Menu;

import com.bumptech.glide.Glide;
import com.example.bmb.MessageNotificationService;
import com.example.bmb.ui.main.ChatListFragment;
import com.example.bmb.ui.main.AddPostFragment;
import com.example.bmb.ui.main.FavoritesFragment;
import com.example.bmb.ui.main.HomeFragment;
import com.example.bmb.ui.main.ProfileFragment;
import com.example.bmb.R;
import com.example.bmb.ui.main.VetsFragment;
import com.example.bmb.auth.AuthManager;
import com.example.bmb.utils.KeyboardVisibilityHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements KeyboardVisibilityHelper.OnKeyboardVisibilityListener {

    private BottomNavigationView bottomNavigation;
    private MaterialToolbar topAppBar;
    private FragmentManager fragmentManager;
    private ImageView ivUser;
    private MaterialTextView tvUserName;
    private LinearLayout llUser;
    private KeyboardVisibilityHelper keyboardVisibilityHelper;

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, MessageNotificationService.class);
        startService(serviceIntent);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        bottomNavigation = findViewById(R.id.bottomNavigation);
        llUser = findViewById(R.id.llUser);
        topAppBar = findViewById(R.id.topAppBar);
        ivUser = findViewById(R.id.ivUser);
        tvUserName = findViewById(R.id.tvUserName);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            AuthManager authManager = new AuthManager(this);
            authManager.fetchUserData(currentUser, new AuthManager.OnUserDataFetchListener() {
                @Override
                public void onSuccess(Map<String, Object> userData) {
                    String userName = (String) userData.get("name");
                    String userPhotoUrl = (String) userData.get("userPhoto");

                    updateUI(userName, userPhotoUrl);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MainActivity.this, "Error al obtener datos del usuario: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }

        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.home);
            replaceFragment(new HomeFragment());
        }

        keyboardVisibilityHelper = new KeyboardVisibilityHelper(this, findViewById(android.R.id.content), this);

        setupListeners();
    }

    @Override
    public void onKeyboardVisibilityChanged(boolean isVisible) {
        if (isVisible) {
            bottomNavigation.setVisibility(View.GONE);
        } else {
            bottomNavigation.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
            } else {
                // Permiso denegado
            }
        }
    }

    private void setupListeners() {
        topAppBar.setOnMenuItemClickListener(this::onMenuItemClick);
        llUser.setOnClickListener(v -> selectFragment(new ProfileFragment()));
        bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);
        bottomNavigation.setOnItemReselectedListener(this::onNavigationItemReselected);

        fragmentManager.addOnBackStackChangedListener(() -> {
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer);
            if (currentFragment instanceof HomeFragment) {
                setupRecyclerViewScrollListener(((HomeFragment) currentFragment).getRvHome());
            }
        });
    }

    private boolean onMenuItemClick(MenuItem item) {
        deselectAllMenuItems();
        return handleNavigationItemSelection(item);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return handleNavigationItemSelection(item);
    }

    private void onNavigationItemReselected(@NonNull MenuItem item) {
        handleNavigationItemSelection(item);
    }

    private boolean handleNavigationItemSelection(MenuItem item) {
        Fragment selectedFragment = null;

        resetIconState();
        resetTopAppBarIcons();

        int itemId = item.getItemId();

        if (itemId == R.id.add) {
            item.setIcon(R.drawable.ic_add_selected);
            selectedFragment = new AddPostFragment();
        } else if (itemId == R.id.favorite) {
            item.setIcon(R.drawable.ic_favorite_selected);
            selectedFragment = new FavoritesFragment();
        } else if (itemId == R.id.logOut) {
            signOut();
            return true;
        } else if (itemId == R.id.home) {
            item.setIcon(R.drawable.ic_home_selected);
            selectedFragment = new HomeFragment();
        }  else if (itemId == R.id.messages) {
            item.setIcon(R.drawable.ic_chat_selected);
            selectedFragment = new ChatListFragment();
        } else if (itemId == R.id.vet) {
            selectedFragment = new VetsFragment();
        }

        if (selectedFragment != null) {
            replaceFragment(selectedFragment);
            return true;
        }

        return false;
    }

    private void selectFragment(Fragment fragment) {
        deselectAllMenuItems();
        replaceFragment(fragment);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void resetTopAppBarIcons() {
        setMenuItemIcon(topAppBar.getMenu(), R.id.add, R.drawable.ic_add);
        setMenuItemIcon(topAppBar.getMenu(), R.id.favorite, R.drawable.ic_favorite);
    }

    private void setMenuItemIcon(Menu menu, int menuItemId, int iconResId) {
        MenuItem menuItem = menu.findItem(menuItemId);
        if (menuItem != null) {
            menuItem.setIcon(iconResId);
        }
    }

    private void resetIconState() {
        Menu menu = bottomNavigation.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            int itemId = menuItem.getItemId();
            if (itemId == R.id.home) {
                menuItem.setIcon(R.drawable.ic_home);
            }
        }
    }

    private void deselectAllMenuItems() {
        bottomNavigation.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigation.getMenu().size(); i++) {
            bottomNavigation.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigation.getMenu().setGroupCheckable(0, true, true);
    }

    @SuppressLint("ResourceAsColor")
    private void updateUI(String userName, String userPhotoUrl) {
        Glide.with(this)
                .load(userPhotoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(ivUser);

        tvUserName.setText(userName);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar, menu);

        if (menu instanceof MenuBuilder) {
            MenuBuilder menuBuilder = (MenuBuilder) menu;
            menuBuilder.setOptionalIconsVisible(true);
        }

        return true;
    }

    private void signOut() {
        AuthManager authManager = new AuthManager(this);
        authManager.signOut();
    }

    private void setupRecyclerViewScrollListener(RecyclerView rvHome) {
        if (rvHome != null) {
            rvHome.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (dy > 0 && bottomNavigation.isShown()) {
                        bottomNavigation.setVisibility(View.GONE);
                    } else if (dy < 0) {
                        bottomNavigation.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }
}