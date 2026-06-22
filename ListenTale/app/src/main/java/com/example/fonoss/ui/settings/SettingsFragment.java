package com.example.fonoss.ui.settings;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.ui.library.LibraryViewModel;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    private static final String PREFS_NAME = "settings";
    private static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    private static final int NOTIFICATION_PERMISSION_CODE = 2002;

    private LibraryViewModel libraryViewModel;
    private SharedPreferences preferences;
    private SwitchMaterial switchPushNotifications;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        switchPushNotifications = view.findViewById(R.id.switch_push_notifications);

        view.findViewById(R.id.button_back).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp()
        );

        setupPushNotifications();
        view.findViewById(R.id.button_clear_data).setOnClickListener(v -> showClearDataWarning());
        view.findViewById(R.id.button_about_app).setOnClickListener(v -> showAboutDialog());
    }

    private void setupPushNotifications() {
        boolean enabled = preferences.getBoolean(KEY_PUSH_NOTIFICATIONS, true);
        switchPushNotifications.setChecked(enabled && hasNotificationPermission());
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasNotificationPermission()) {
                requestNotificationPermission();
                return;
            }
            preferences.edit().putBoolean(KEY_PUSH_NOTIFICATIONS, isChecked).apply();
            if (!isChecked) {
                NotificationManager manager = (NotificationManager) requireContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancelAll();
                UiNotifier.info(getContext(), "Notifications turned off");
            } else {
                UiNotifier.info(getContext(), "Notifications turned on");
            }
        });
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
        }
    }

    private void showClearDataWarning() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Data?")
                .setMessage("This will remove all downloaded books from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    libraryViewModel.clearLocalDownloads();
                    UiNotifier.success(getContext(), "Local downloads cleared");
                })
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About ListenTale")
                .setMessage("Owners: Quoc Huy - Huu Truc - Ngoc Bao")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            preferences.edit().putBoolean(KEY_PUSH_NOTIFICATIONS, granted).apply();
            switchPushNotifications.setChecked(granted);
            UiNotifier.info(getContext(), granted ? "Notifications turned on" : "Notifications turned off");
        }
    }
}



