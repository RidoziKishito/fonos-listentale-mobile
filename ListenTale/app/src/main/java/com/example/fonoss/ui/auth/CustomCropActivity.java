package com.example.fonoss.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.canhub.cropper.CropImageView;
import com.example.fonoss.R;

public class CustomCropActivity extends AppCompatActivity {

    public static final int RESULT_RETAKE = 101;
    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_CROPPED_URI = "extra_cropped_uri";

    private CropImageView cropImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_crop);

        cropImageView = findViewById(R.id.cropImageView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        View btnCrop = findViewById(R.id.btn_crop);

        toolbar.setNavigationOnClickListener(v -> handleBackPress());
        btnCrop.setOnClickListener(v -> {
            try {
                Bitmap cropped = cropImageView.getCroppedImage();
                if (cropped != null) {
                    java.io.File tempFile = java.io.File.createTempFile("cropped_avatar", ".jpg", getCacheDir());
                    java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
                    cropped.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();

                    Intent data = new Intent();
                    data.putExtra(EXTRA_CROPPED_URI, Uri.fromFile(tempFile));
                    setResult(RESULT_OK, data);
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        Uri imageUri = getIntent().getParcelableExtra(EXTRA_URI);
        if (imageUri != null) {
            cropImageView.setImageUriAsync(imageUri);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }


    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    private void handleBackPress() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Update")
                .setMessage("What do you want to do?")
                .setNeutralButton("Stay Here", null) // Do nothing, just close dialog
                .setPositiveButton("Retake/Reselect", (dialog, which) -> {
                    setResult(RESULT_RETAKE);
                    finish();
                })
                .setNegativeButton("Cancel Entirely", (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .show();
    }
}
