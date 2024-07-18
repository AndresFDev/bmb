package com.example.bmb.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Menu;

import com.bumptech.glide.Glide;
import com.example.bmb.ui.main.AddPostFragment;
import com.example.bmb.ui.main.AdoptionFragment;
import com.example.bmb.ui.main.FavoritesFragment;
import com.example.bmb.ui.main.HomeFragment;
import com.example.bmb.ui.main.ProfileFragment;
import com.example.bmb.R;
import com.example.bmb.ui.main.VetsFragment;
import com.example.bmb.auth.AuthManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private MaterialToolbar topAppBar;
    private FragmentManager fragmentManager;
    private ImageView ivUser;
    private MaterialTextView tvUserName;
    private LinearLayout llUser;
    private boolean isKeyboardVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        setupListeners();
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
            setupKeyboardVisibilityListener();
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
        } else if (itemId == R.id.vet) {
            selectedFragment = new VetsFragment();
        } else if (itemId == R.id.adop) {
            selectedFragment = new AdoptionFragment();
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

    private void setupKeyboardVisibilityListener() {
        final View rootView = findViewById(android.R.id.content);
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean isKeyboardOpen = imm.isAcceptingText();

            if (isKeyboardOpen) {
                hideBottomNavigationView();
            } else {
                showBottomNavigationView();
            }
        });
    }

    private void hideBottomNavigationView() {
        bottomNavigation.setVisibility(View.GONE);
    }

    private void showBottomNavigationView() {
        bottomNavigation.setVisibility(View.VISIBLE);
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