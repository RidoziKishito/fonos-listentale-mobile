package com.example.fonoss.ui.auth;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.library.LibraryViewModel;
import com.example.fonoss.ui.home.WelcomeFragment;
import com.example.fonoss.data.repository.UserRepository;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private UserViewModel userViewModel;
    private LibraryViewModel libraryViewModel;
    private ImageView avatarImageView;
    private BottomSheetDialog bottomSheetDialog;
    private int lastImageSource = 0; // 1: Gallery, 2: Camera

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Void> takePictureLauncher;
    private ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<Intent> customCropLauncher;

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
                        try {
                            // Save bitmap to temp file to get URI for cropper
                            File tempFile = File.createTempFile("temp_avatar", ".jpg", requireContext().getCacheDir());
                            FileOutputStream out = new FileOutputStream(tempFile);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.flush();
                            out.close();
                            launchCropper(Uri.fromFile(tempFile));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        launchCropper(uri);
                    }
                }
        );

        customCropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri croppedUri = result.getData().getParcelableExtra(CustomCropActivity.EXTRA_CROPPED_URI);
                        if (croppedUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), croppedUri);
                                
                                com.example.fonoss.utils.ImageModerationHelper modHelper = new com.example.fonoss.utils.ImageModerationHelper(requireContext());
                                boolean isAppropriate = modHelper.isImageAppropriate(bitmap);
                                modHelper.close();

                                if (!isAppropriate) {
                                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Content Warning")
                                            .setMessage("The image you uploaded contains inappropriate content (NSFW) or violates community standards.\n\nPlease select another image for your avatar.")
                                            .setPositiveButton("Got it", null)
                                            .show();
                                } else {
                                    userViewModel.uploadAvatar(bitmap);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (result.getResultCode() == CustomCropActivity.RESULT_RETAKE) {
                        if (lastImageSource == 1) {
                            pickMediaLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                                    .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                    .build());
                        } else if (lastImageSource == 2) {
                            takePictureLauncher.launch(null);
                        }
                    }
                }
        );
    }

    private void launchCropper(Uri uri) {
        Intent intent = new Intent(requireContext(), CustomCropActivity.class);
        intent.putExtra(CustomCropActivity.EXTRA_URI, uri);
        customCropLauncher.launch(intent);
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
        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        TextView textName = view.findViewById(R.id.text_profile_name);
        TextView textMembership = view.findViewById(R.id.text_profile_membership);
        avatarImageView = view.findViewById(R.id.image_profile_avatar);
        View buttonUpgrade = view.findViewById(R.id.button_upgrade_account);

        userViewModel.getUserName().observe(getViewLifecycleOwner(), name -> {
            if (textName != null) textName.setText(name);
        });

        userViewModel.getAccountType().observe(getViewLifecycleOwner(), type -> {
            if (textMembership != null) {
                if ("PREMIUM".equals(type)) {
                    textMembership.setText("Premium Member");
                    textMembership.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_600));
                    buttonUpgrade.setVisibility(View.GONE);
                } else {
                    textMembership.setText("Free Member");
                    textMembership.setTextColor(ContextCompat.getColor(requireContext(), R.color.slate_500));
                    buttonUpgrade.setVisibility(View.VISIBLE);
                }
            }
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
        View.OnClickListener avatarClickListener = v -> showAvatarBottomSheet();
        avatarImageView.setOnClickListener(avatarClickListener);
        editIcon.setOnClickListener(avatarClickListener);

        libraryViewModel.fetchLibraryData();

        Runnable refreshStats = () -> updateDashboardStats(view, libraryViewModel);
        refreshStats.run();
        
        libraryViewModel.getCompletedBooks().observe(getViewLifecycleOwner(), books -> refreshStats.run());
        libraryViewModel.getInProgressBooks().observe(getViewLifecycleOwner(), books -> refreshStats.run());
        libraryViewModel.getSavedBooks().observe(getViewLifecycleOwner(), books -> refreshStats.run());
        libraryViewModel.getBookProgressPos().observe(getViewLifecycleOwner(), map -> refreshStats.run());
        libraryViewModel.getTotalListeningSeconds().observe(getViewLifecycleOwner(), sec -> refreshStats.run());

        view.findViewById(R.id.button_account_info).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_accountInfoFragment)
        );

        buttonUpgrade.setOnClickListener(v -> showUpgradeDialog());

        view.findViewById(R.id.button_downloaded).setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_downloadedBooksFragment)
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

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && libraryViewModel != null) {
            updateDashboardStats(getView(), libraryViewModel);
        }
    }

    private void updateDashboardStats(View view, LibraryViewModel libraryViewModel) {
        if (view == null || libraryViewModel == null) return;

        TextView textRankTitle = view.findViewById(R.id.text_rank_title);
        TextView textRankSubtitle = view.findViewById(R.id.text_rank_subtitle);
        android.widget.ProgressBar progressRank = view.findViewById(R.id.progress_rank);
        TextView textRankProgressStatus = view.findViewById(R.id.text_rank_progress_status);

        TextView textStatHours = view.findViewById(R.id.text_stat_hours);
        TextView textStatBooksCompleted = view.findViewById(R.id.text_stat_books_completed);
        TextView textStatTopGenre = view.findViewById(R.id.text_stat_top_genre);

        java.util.List<com.example.fonoss.data.model.Book> completed = libraryViewModel.getCompletedBooks().getValue();
        int completedCount = (completed != null) ? completed.size() : 0;

        Long listeningSecsObj = libraryViewModel.getTotalListeningSeconds().getValue();
        double totalSeconds = (listeningSecsObj != null) ? listeningSecsObj.doubleValue() : 0.0;
        double totalHours = totalSeconds / 3600.0;
        int totalMinutes = (int) (totalSeconds / 60.0);

        if (textStatHours != null) {
            if (totalMinutes < 100) {
                textStatHours.setText(String.format(java.util.Locale.getDefault(), "%dm", totalMinutes));
            } else {
                textStatHours.setText(String.format(java.util.Locale.getDefault(), "%.1fh", totalHours));
            }
        }
        if (textStatBooksCompleted != null) {
            textStatBooksCompleted.setText(String.valueOf(completedCount));
        }

        // Calculate Top Genre
        String topGenre = "General";
        java.util.Map<String, Integer> genreCount = new java.util.HashMap<>();
        java.util.List<com.example.fonoss.data.model.Book> allUserBooks = new java.util.ArrayList<>();
        if (completed != null) allUserBooks.addAll(completed);
        java.util.List<com.example.fonoss.data.model.Book> inProgress = libraryViewModel.getInProgressBooks().getValue();
        if (inProgress != null) allUserBooks.addAll(inProgress);
        java.util.List<com.example.fonoss.data.model.Book> saved = libraryViewModel.getSavedBooks().getValue();
        if (saved != null) allUserBooks.addAll(saved);

        for (com.example.fonoss.data.model.Book b : allUserBooks) {
            if (b != null && b.getGenre() != null && !b.getGenre().trim().isEmpty()) {
                String g = b.getGenre().trim();
                genreCount.put(g, genreCount.getOrDefault(g, 0) + 1);
            }
        }
        int maxCount = 0;
        for (java.util.Map.Entry<String, Integer> entry : genreCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topGenre = entry.getKey();
            }
        }
        if (textStatTopGenre != null) {
            textStatTopGenre.setText(topGenre);
        }

        // Rank Progression Calculation (Lvl 1 - 7)
        int rankLevel = 1;
        String rankTitle = "Level 1 • Beginner";
        String rankSubtitle = "The journey of a thousand pages begins";
        String nextRankTitle = "Level 2 • Explorer";
        int targetBooks = 3;
        int targetHours = 5;

        if (completedCount >= 100 || totalHours >= 300) {
            rankLevel = 7;
            rankTitle = "Level 7 • Legend";
            rankSubtitle = "An extraordinary legacy of reading";
            nextRankTitle = "Max Level Reached!";
            targetBooks = 100;
            targetHours = 300;
        } else if (completedCount >= 50 || totalHours >= 150) {
            rankLevel = 6;
            rankTitle = "Level 6 • Luminary";
            rankSubtitle = "A beacon of knowledge and stories";
            nextRankTitle = "Level 7 • Legend";
            targetBooks = 100;
            targetHours = 300;
        } else if (completedCount >= 30 || totalHours >= 75) {
            rankLevel = 5;
            rankTitle = "Level 5 • Scholar";
            rankSubtitle = "Mastering wisdom through words";
            nextRankTitle = "Level 6 • Luminary";
            targetBooks = 50;
            targetHours = 150;
        } else if (completedCount >= 15 || totalHours >= 35) {
            rankLevel = 4;
            rankTitle = "Level 4 • Bibliophile";
            rankSubtitle = "A true lover of literature";
            nextRankTitle = "Level 5 • Scholar";
            targetBooks = 30;
            targetHours = 75;
        } else if (completedCount >= 7 || totalHours >= 15) {
            rankLevel = 3;
            rankTitle = "Level 3 • Bookworm";
            rankSubtitle = "Diving deeper into new worlds";
            nextRankTitle = "Level 4 • Bibliophile";
            targetBooks = 15;
            targetHours = 35;
        } else if (completedCount >= 3 || totalHours >= 5) {
            rankLevel = 2;
            rankTitle = "Level 2 • Explorer";
            rankSubtitle = "Finding your rhythm in stories";
            nextRankTitle = "Level 3 • Bookworm";
            targetBooks = 7;
            targetHours = 15;
        }

        int progressPercent = 0;
        if (rankLevel == 7) {
            progressPercent = 100;
        } else {
            double booksProgress = ((double) completedCount / targetBooks) * 100.0;
            double hoursProgress = (totalHours / targetHours) * 100.0;
            progressPercent = Math.min(100, (int) Math.max(booksProgress, hoursProgress));
        }

        if (textRankTitle != null) textRankTitle.setText(rankTitle);
        if (textRankSubtitle != null) textRankSubtitle.setText(rankSubtitle);
        if (progressRank != null) progressRank.setProgress(progressPercent);
        if (textRankProgressStatus != null) {
            if (rankLevel == 7) {
                textRankProgressStatus.setText("100% • Legend Rank Achieved!");
            } else {
                textRankProgressStatus.setText(String.format(java.util.Locale.getDefault(),
                        "%d%% to %s (%d/%d books completed)",
                        progressPercent, nextRankTitle, completedCount, targetBooks));
            }
        }
    }

    private void showUpgradeDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("Generating payment request...")
                .setCancelable(false)
                .show();

        userViewModel.startUpgradeRequest(new UserRepository.UpgradeRequestCallback() {
            @Override
            public void onSuccess(JSONObject result) {
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    try {
                        String qrUrl = result.getString("qrUrl");
                        String paymentCode = result.getString("paymentCode");
                        String bank = result.getString("bank");
                        String accountNo = result.getString("accountNumber");
                        String accountName = result.getString("accountName");
                        int amount = result.getInt("amount");

                        displayRealUpgradeDialog(qrUrl, paymentCode, bank, accountNo, accountName, amount);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to create request: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private ListenerRegistration paymentListener;

    private void displayRealUpgradeDialog(String qrUrl, String paymentCode, String bank, String accountNo, String accountName, int amount) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(getLayoutInflater().inflate(R.layout.dialog_upgrade_account, null))
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        ImageView qrImage = dialog.findViewById(R.id.image_qr_code);
        TextView memoText = dialog.findViewById(R.id.text_payment_memo);
        TextView bankText = dialog.findViewById(R.id.text_bank_info);
        TextView accountNoText = dialog.findViewById(R.id.text_account_number);
        TextView accountNameText = dialog.findViewById(R.id.text_account_name);
        View btnPaid = dialog.findViewById(R.id.button_paid);
        View btnCancel = dialog.findViewById(R.id.button_cancel);

        if (memoText != null) memoText.setText("Memo: " + paymentCode);
        if (bankText != null) bankText.setText("Bank: " + bank);
        if (accountNoText != null) accountNoText.setText("Account: " + accountNo);
        if (accountNameText != null) accountNameText.setText("Holder: " + accountName);

        if (qrImage != null) Glide.with(this).load(qrUrl).into(qrImage);

        if (btnCancel != null) btnCancel.setOnClickListener(view -> {
            if (paymentListener != null) paymentListener.remove();
            dialog.dismiss();
        });

        if (btnPaid != null) btnPaid.setOnClickListener(view -> {
            btnPaid.setEnabled(false);
            Toast.makeText(getContext(), "Verifying payment simulation...", Toast.LENGTH_SHORT).show();
            
            userViewModel.verifyAndUpgrade(aVoid -> {
                if (paymentListener != null) paymentListener.remove();
                dialog.dismiss();
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Success (Simulation)")
                        .setMessage("Your account has been upgraded to Premium for testing. Enjoy!")
                        .setPositiveButton("Awesome", null)
                        .show();
            }, e -> {
                btnPaid.setEnabled(true);
                Toast.makeText(getContext(), "Simulation failed.", Toast.LENGTH_SHORT).show();
            });
        });

        paymentListener = userViewModel.listenToPaymentStatus(paymentCode, aVoid -> {
            if (paymentListener != null) paymentListener.remove();
            dialog.dismiss();
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Upgrade Successful!")
                    .setMessage("Welcome to Premium! Your account has been upgraded successfully.")
                    .setPositiveButton("Explore Premium", null)
                    .show();
        });

        dialog.setOnDismissListener(d -> {
            if (paymentListener != null) paymentListener.remove();
        });
    }

    private void showAvatarBottomSheet() {
        if (bottomSheetDialog == null) {
            bottomSheetDialog = new BottomSheetDialog(requireContext(), com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog);
            View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_avatar_options, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            bottomSheetDialog.setOnShowListener(dialogInterface -> {
                View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundResource(android.R.color.transparent);
                }
            });

            bottomSheetView.findViewById(R.id.option_gallery).setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                lastImageSource = 1;
                pickMediaLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                        .setMediaType(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            });

            bottomSheetView.findViewById(R.id.option_camera).setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                lastImageSource = 2;
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    takePictureLauncher.launch(null);
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });

            bottomSheetView.findViewById(R.id.option_remove).setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                userViewModel.removeAvatar();
            });
        }
        bottomSheetDialog.show();
    }
}
