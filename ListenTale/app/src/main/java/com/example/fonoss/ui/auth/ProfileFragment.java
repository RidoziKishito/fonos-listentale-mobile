package com.example.fonoss.ui.auth;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.library.LibraryViewModel;
import com.example.fonoss.ui.home.WelcomeFragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private UserViewModel userViewModel;
    private ImageView avatarImageView;

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Void> takePictureLauncher;
    private ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMediaLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        takePictureLauncher.launch(null);
                    } else {
                        Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        userViewModel.uploadAvatar(bitmap);
                    }
                }
        );

        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                            userViewModel.uploadAvatar(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        LibraryViewModel libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        TextView textName = view.findViewById(R.id.text_profile_name);
        avatarImageView = view.findViewById(R.id.image_profile_avatar);

        userViewModel.getUserName().observe(getViewLifecycleOwner(), name -> {
            if (textName != null) textName.setText(name);
        });

        userViewModel.getUserAvatar().observe(getViewLifecycleOwner(), base64Str -> {
            if (base64Str != null && !base64Str.isEmpty()) {
                try {
                    byte[] imageByteArray = Base64.decode(base64Str, Base64.DEFAULT);
                    Glide.with(this)
                            .load(imageByteArray)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(avatarImageView);
                } catch (Exception e) {
                    e.printStackTrace();
                    avatarImageView.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            } else {
                avatarImageView.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        });

        View editIcon = view.findViewById(R.id.icon_edit_avatar);
        View.OnClickListener avatarClickListener = v -> showAvatarOptionsDialog();
        avatarImageView.setOnClickListener(avatarClickListener);
        editIcon.setOnClickListener(avatarClickListener);

        view.findViewById(R.id.button_account_info).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_accountInfoFragment)
        );

        view.findViewById(R.id.button_settings).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_settingsFragment)
        );

        view.findViewById(R.id.button_logout).setOnClickListener(v -> {
            libraryViewModel.clearLocalDownloads();
            FirebaseAuth.getInstance().signOut();
            userViewModel.clearData();
            // Navigate to WelcomeFragment and clear backstack
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            Navigation.findNavController(v).navigate(R.id.welcomeFragment, null, navOptions);
        });
    }

    private void showAvatarOptionsDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo", "Remove Profile Picture"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickMediaLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                                .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
                    } else if (which == 1) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            takePictureLauncher.launch(null);
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else if (which == 2) {
                        userViewModel.removeAvatar();
                    }
                })
                .show();
    }
}
