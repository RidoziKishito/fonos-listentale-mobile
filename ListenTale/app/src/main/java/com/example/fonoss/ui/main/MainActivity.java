package com.example.fonoss.ui.main;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.auth.RegisterFragment;
import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.ui.player.AudioPlayerFragment;
import com.example.fonoss.ui.library.LibraryViewModel;
import com.example.fonoss.ui.home.WelcomeFragment;
import com.example.fonoss.ui.auth.LoginFragment;
import com.example.fonoss.ui.player.EbookReaderFragment;
import com.example.fonoss.ui.book.BookDetailFragment;
import com.example.fonoss.ui.auth.UserViewModel;
import com.example.fonoss.ui.home.SeeAllFragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private View noInternetLayout;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final String PREFS_NAME = "settings";
    private static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    private android.content.BroadcastReceiver networkWarningReceiver;
    private boolean hasShownGooglePasswordPrompt = false;

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePasswordPrompt();
    }

    private void checkGooglePasswordPrompt() {
        if (hasShownGooglePasswordPrompt) return;
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        
        boolean isGoogleUser = false;
        boolean hasPassword = false;
        for (com.google.firebase.auth.UserInfo userInfo : user.getProviderData()) {
            if ("google.com".equals(userInfo.getProviderId())) isGoogleUser = true;
            if ("password".equals(userInfo.getProviderId())) hasPassword = true;
        }
        
        if (isGoogleUser && !hasPassword) {
            hasShownGooglePasswordPrompt = true;
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Create Password")
                .setMessage("You logged in with Google. You can create a password to log in with your email next time.")
                .setPositiveButton("Create Now", (dialog, which) -> {
                    NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                    if (navHostFragment != null) {
                        try {
                            android.os.Bundle args = new android.os.Bundle();
                            args.putBoolean("showCreatePassword", true);
                            navHostFragment.getNavController().navigate(R.id.settingsFragment, args);
                        } catch (Exception e) {
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noInternetLayout = findViewById(R.id.layout_no_internet);
        findViewById(R.id.btn_close_no_internet).setOnClickListener(v -> noInternetLayout.setVisibility(View.GONE));

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNav, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.welcomeFragment || 
                    destination.getId() == R.id.loginFragment ||
                    destination.getId() == R.id.registerFragment ||
                    destination.getId() == R.id.seeAllFragment ||
                    destination.getId() == R.id.bookDetailFragment ||
                    destination.getId() == R.id.ebookReaderFragment ||
                    destination.getId() == R.id.audioPlayerFragment) {
                    bottomNav.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                }
            });

            handleNotificationIntent(getIntent(), navController);
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            private long backPressedTime = 0;
            @Override
            public void handleOnBackPressed() {
                if (navHostFragment != null) {
                    androidx.navigation.NavController navController = navHostFragment.getNavController();
                    androidx.navigation.NavDestination currentDest = navController.getCurrentDestination();
                    if (currentDest != null) {
                        int id = currentDest.getId();
                        if (id == R.id.booksFragment || 
                            id == R.id.searchFragment || 
                            id == R.id.libraryFragment || 
                            id == R.id.profileFragment ||
                            id == R.id.welcomeFragment ||
                            id == R.id.loginFragment) {
                            
                            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                                finish();
                            } else {
                                android.widget.Toast.makeText(MainActivity.this, "Nh?n quay l?i l?n n?a d? thoát", android.widget.Toast.LENGTH_SHORT).show();
                            }
                            backPressedTime = System.currentTimeMillis();
                            return;
                        }
                    }
                }
                
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            new ViewModelProvider(this).get(LibraryViewModel.class).fetchLibraryData();
            new ViewModelProvider(this).get(UserViewModel.class).fetchUserData();
        }

        registerNetworkCallback();
        requestNotificationPermission();

        networkWarningReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra("message");
                if (msg != null) {
                    UiNotifier.warning(MainActivity.this, msg);
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(networkWarningReceiver, new android.content.IntentFilter("com.example.fonoss.NETWORK_WARNING"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(networkWarningReceiver, new android.content.IntentFilter("com.example.fonoss.NETWORK_WARNING"));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            handleNotificationIntent(intent, navHostFragment.getNavController());
        }
    }

    private void handleNotificationIntent(Intent intent, NavController navController) {
        if (intent != null && intent.getBooleanExtra("OPEN_PLAYER", false)) {
            if (navController.getCurrentDestination() == null || 
                navController.getCurrentDestination().getId() != R.id.audioPlayerFragment) {
                navController.navigate(R.id.audioPlayerFragment);
            }
        }
    }

    private void requestNotificationPermission() {
        boolean notificationsEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_PUSH_NOTIFICATIONS, true);
        if (!notificationsEnabled) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void registerNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        updateInternetStatus(isNetworkAvailable());

        connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                updateInternetStatus(false);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) 
                                   && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                updateInternetStatus(hasInternet);
            }
        });
    }

    private void updateInternetStatus(boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                noInternetLayout.setVisibility(View.GONE);
            } else {
                noInternetLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                   && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
    @Override
    protected void onDestroy() {
        if (networkWarningReceiver != null) {
            unregisterReceiver(networkWarningReceiver);
        }
        super.onDestroy();
    }
}
