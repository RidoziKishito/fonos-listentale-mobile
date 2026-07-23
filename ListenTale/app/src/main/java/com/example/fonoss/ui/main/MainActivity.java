package com.example.fonoss.ui.main;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.fonoss.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private long backPressedTime;
    private View layoutNoInternet;
    private ImageView btnCloseNoInternet;
    private boolean isBannerDismissed = false;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutNoInternet = findViewById(R.id.layout_no_internet);
        btnCloseNoInternet = findViewById(R.id.btn_close_no_internet);

        if (btnCloseNoInternet != null) {
            btnCloseNoInternet.setOnClickListener(v -> {
                isBannerDismissed = true;
                if (layoutNoInternet != null) {
                    layoutNoInternet.setVisibility(View.GONE);
                }
            });
        }

        setupNetworkCallback();

        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.loginFragment
                    || id == R.id.registerFragment
                    || id == R.id.welcomeFragment
                    || id == R.id.audioPlayerFragment
                    || id == R.id.bookDetailFragment
                    || id == R.id.ebookReaderFragment
                    || id == R.id.downloadedBooksFragment
                    || id == R.id.chatFragment) {
                navView.setVisibility(View.GONE);
            } else {
                navView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    isBannerDismissed = false;
                    if (layoutNoInternet != null) {
                        layoutNoInternet.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (layoutNoInternet != null && !isBannerDismissed) {
                        layoutNoInternet.setVisibility(View.VISIBLE);
                    }
                });
            }
        };

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isConnected = isOnline(connectivityManager);
        if (layoutNoInternet != null) {
            layoutNoInternet.setVisibility(!isConnected && !isBannerDismissed ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isOnline(ConnectivityManager cm) {
        if (cm == null) return true;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                try {
                    cm.unregisterNetworkCallback(networkCallback);
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onBackPressed() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        if (navController.getCurrentDestination() != null) {
            int id = navController.getCurrentDestination().getId();
            if (id == R.id.booksFragment || id == R.id.welcomeFragment || id == R.id.loginFragment) {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish();
                } else {
                    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
                backPressedTime = System.currentTimeMillis();
                return;
            }
        }
        super.onBackPressed();
    }
}

