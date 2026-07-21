package com.example.fonoss.ui.settings;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.ui.library.LibraryViewModel;

import android.Manifest;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import java.util.Locale;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.fonoss.utils.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    private static final String PREFS_NAME = "settings";
    private static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final int NOTIFICATION_PERMISSION_CODE = 2002;

    private LibraryViewModel libraryViewModel;
    private SharedPreferences preferences;
    private SwitchMaterial switchPushNotifications;
    private TextView textReminderTime;

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
        textReminderTime = view.findViewById(R.id.text_notification_time);

        view.findViewById(R.id.button_back).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp()
        );

        setupPushNotifications();
        setupReminderTime(view);
        view.findViewById(R.id.button_clear_data).setOnClickListener(v -> showClearDataWarning());
        view.findViewById(R.id.button_about_app).setOnClickListener(v -> showAboutDialog());

        TextView authActionBtn = view.findViewById(R.id.button_change_password);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isPasswordUser = false;
        if (user != null) {
            for (UserInfo userInfo : user.getProviderData()) {
                if ("password".equals(userInfo.getProviderId())) {
                    isPasswordUser = true;
                    break;
                }
            }
            
            authActionBtn.setVisibility(View.VISIBLE);
            if (isPasswordUser) {
                authActionBtn.setText("Change Password");
                authActionBtn.setOnClickListener(v -> showChangePasswordDialog(user));
            } else {
                authActionBtn.setText("Create Password");
                authActionBtn.setOnClickListener(v -> showCreatePasswordDialog(user, authActionBtn));
                
                if (getArguments() != null && getArguments().getBoolean("showCreatePassword", false)) {
                    authActionBtn.post(() -> showCreatePasswordDialog(user, authActionBtn));
                    getArguments().putBoolean("showCreatePassword", false);
                }
            }
        } else {
            authActionBtn.setVisibility(View.GONE);
        }
    }

    private void showChangePasswordDialog(FirebaseUser user) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);

        TextInputEditText inputCurrent = dialogView.findViewById(R.id.input_current_password);
        TextInputEditText inputNew = dialogView.findViewById(R.id.input_new_password);
        TextInputEditText inputConfirm = dialogView.findViewById(R.id.input_confirm_password);
        
        TextInputLayout layoutCurrent = dialogView.findViewById(R.id.input_current_password_layout);
        TextInputLayout layoutNew = dialogView.findViewById(R.id.input_new_password_layout);
        TextInputLayout layoutConfirm = dialogView.findViewById(R.id.input_confirm_password_layout);

        MaterialButton buttonCancel = dialogView.findViewById(R.id.button_cancel_change);
        MaterialButton buttonSave = dialogView.findViewById(R.id.button_save_password);
        TextView textForgotPassword = dialogView.findViewById(R.id.text_forgot_password);

        textForgotPassword.setOnClickListener(v -> {
            dialog.dismiss();
            showForgotPasswordDialog(user.getEmail());
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String currentPass = inputCurrent.getText().toString().trim();
            String newPass = inputNew.getText().toString().trim();
            String confirmPass = inputConfirm.getText().toString().trim();

            boolean valid = true;
            if (currentPass.isEmpty()) {
                layoutCurrent.setError("Required");
                valid = false;
            } else layoutCurrent.setError(null);
            
            if (newPass.length() < 6) {
                layoutNew.setError("Password must be at least 6 characters");
                valid = false;
            } else layoutNew.setError(null);
            
            if (!newPass.equals(confirmPass)) {
                layoutConfirm.setError("Passwords do not match");
                valid = false;
            } else layoutConfirm.setError(null);

            if (!valid) return;

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
            user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            UiNotifier.success(getContext(), "Password changed successfully");
                            dialog.dismiss();
                        } else {
                            UiNotifier.error(getContext(), "Failed to update password");
                        }
                    });
                } else {
                    Exception e = authTask.getException();
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        layoutCurrent.setError("Incorrect current password");
                    } else {
                        UiNotifier.error(getContext(), "Authentication failed");
                    }
                }
            });
        });

        dialog.show();
    }

    private void showCreatePasswordDialog(FirebaseUser user, TextView authActionBtn) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_password, null);
        dialog.setContentView(dialogView);

        TextInputEditText inputNew = dialogView.findViewById(R.id.input_new_password);
        TextInputEditText inputConfirm = dialogView.findViewById(R.id.input_confirm_password);
        TextInputLayout layoutNew = dialogView.findViewById(R.id.input_new_password_layout);
        TextInputLayout layoutConfirm = dialogView.findViewById(R.id.input_confirm_password_layout);

        MaterialButton buttonCancel = dialogView.findViewById(R.id.button_cancel_create);
        MaterialButton buttonSave = dialogView.findViewById(R.id.button_save_create);

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String newPass = inputNew.getText().toString().trim();
            String confirmPass = inputConfirm.getText().toString().trim();

            boolean valid = true;
            if (newPass.length() < 6) {
                layoutNew.setError("Password must be at least 6 characters");
                valid = false;
            } else layoutNew.setError(null);
            
            if (!newPass.equals(confirmPass)) {
                layoutConfirm.setError("Passwords do not match");
                valid = false;
            } else layoutConfirm.setError(null);

            if (!valid) return;

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), newPass);
            user.linkWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    UiNotifier.success(getContext(), "Password created successfully");
                    authActionBtn.setText("Change Password");
                    authActionBtn.setOnClickListener(btnV -> showChangePasswordDialog(user));
                    dialog.dismiss();
                } else {
                    UiNotifier.error(getContext(), "Failed to create password");
                }
            });
        });

        dialog.show();
    }

    private void showForgotPasswordDialog(String prefilledEmail) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        dialog.setContentView(dialogView);

        TextInputEditText emailInput = dialogView.findViewById(R.id.input_email_reset);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.input_email_layout_reset);
        MaterialButton buttonCancel = dialogView.findViewById(R.id.button_cancel_reset);
        MaterialButton buttonSend = dialogView.findViewById(R.id.button_send_reset);

        if (prefilledEmail != null) {
            emailInput.setText(prefilledEmail);
        }

        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSend.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.setError("Invalid email format");
                return;
            }
            emailLayout.setError(null);
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    UiNotifier.success(getContext(), "If registered, a reset link has been sent.");
                    dialog.dismiss();
                } else {
                    emailLayout.setError("Failed to send reset link");
                }
            });
        });

        dialog.show();
    }

    private void setupPushNotifications() {
        boolean enabled = preferences.getBoolean(KEY_PUSH_NOTIFICATIONS, true);
        switchPushNotifications.setChecked(enabled && hasNotificationPermission());
        
        NotificationHelper.createNotificationChannel(requireContext());

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
                NotificationHelper.cancelReminder(requireContext());
                UiNotifier.info(getContext(), "Notifications turned off");
            } else {
                updateReminderSchedule();
                UiNotifier.info(getContext(), "Notifications turned on");
            }
        });
    }

    private void setupReminderTime(View view) {
        int hour = preferences.getInt(KEY_REMINDER_HOUR, 8);
        int minute = preferences.getInt(KEY_REMINDER_MINUTE, 0);
        textReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));

        view.findViewById(R.id.layout_notification_time).setOnClickListener(v -> {
            int currentHour = preferences.getInt(KEY_REMINDER_HOUR, 8);
            int currentMin = preferences.getInt(KEY_REMINDER_MINUTE, 0);

            new TimePickerDialog(requireContext(), (view1, h, m) -> {
                preferences.edit()
                        .putInt(KEY_REMINDER_HOUR, h)
                        .putInt(KEY_REMINDER_MINUTE, m)
                        .apply();
                textReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
                if (switchPushNotifications.isChecked()) {
                    updateReminderSchedule();
                }
            }, currentHour, currentMin, true).show();
        });
    }

    private void updateReminderSchedule() {
        int h = preferences.getInt(KEY_REMINDER_HOUR, 8);
        int m = preferences.getInt(KEY_REMINDER_MINUTE, 0);
        NotificationHelper.scheduleDailyReminder(requireContext(), h, m);
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

