/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dialer.callingcard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dialer.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CallingCardEditorActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    public static final String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";
    public static final String EXTRA_DELETE_CARD = "EXTRA_DELETE_CARD";
    public static final String EXTRA_VIEW_ONLY = "EXTRA_VIEW_ONLY";

    private EditText etPhoneNumber;
    private ImageView ivPreview;
    private TextView tvEmptyHint;
    private Uri selectedImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling_card_editor);

        etPhoneNumber = findViewById(R.id.et_phone_number);
        ivPreview = findViewById(R.id.iv_preview);
        tvEmptyHint = findViewById(R.id.tv_empty_hint);

        Button btnPickImage = findViewById(R.id.btn_pick_image);
        Button btnSave = findViewById(R.id.btn_save_card);
        View imageContainer = findViewById(R.id.image_container);
        View actionButtons = findViewById(R.id.action_buttons_container);
        TextView tvTitle = findViewById(R.id.tv_title);

        boolean isViewOnly = getIntent() != null && getIntent().getBooleanExtra(EXTRA_VIEW_ONLY, false);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_PHONE_NUMBER)) {
            String passedNumber = getIntent().getStringExtra(EXTRA_PHONE_NUMBER);
            if (passedNumber != null && !passedNumber.trim().isEmpty()) {

                if (getIntent().getBooleanExtra(EXTRA_DELETE_CARD, false)) {
                    CallingCardManager.deleteCard(this, passedNumber.trim());
                    Toast.makeText(this, "Calling Card deleted", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                etPhoneNumber.setText(passedNumber.trim());
                etPhoneNumber.setEnabled(false);

                String existingUriString = CallingCardManager.getCardUri(this, passedNumber.trim());
                if (existingUriString != null) {
                    selectedImageUri = Uri.parse(existingUriString);
                    ivPreview.setImageURI(selectedImageUri);
                    tvEmptyHint.setVisibility(View.GONE);
                } else if (isViewOnly) {
                    // If clicked "View" but no image exists, toast and exit immediately
                    Toast.makeText(this, "No calling card set for this contact", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        }

        if (isViewOnly) {
            tvTitle.setVisibility(View.GONE);
            etPhoneNumber.setVisibility(View.GONE);
            actionButtons.setVisibility(View.GONE);

            // Remove padding so the image goes edge-to-edge
            findViewById(android.R.id.content).setPadding(0, 0, 0, 0);

            // Allow tapping the image to close the viewer
            ivPreview.setOnClickListener(v -> finish());
            return;
        }

        View.OnClickListener pickImageListener = v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        };

        // Allow tapping both the button OR the image frame to pick an image
        btnPickImage.setOnClickListener(pickImageListener);
        imageContainer.setOnClickListener(pickImageListener);

        btnSave.setOnClickListener(v -> {
            String number = etPhoneNumber.getText().toString().trim();
            if (number.isEmpty() || selectedImageUri == null) {
                Toast.makeText(CallingCardEditorActivity.this,
                    "Please select a background image first",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            String savedLocalPath = copyImageToInternalStorage(selectedImageUri, number);

            if (savedLocalPath != null) {
                CallingCardManager.saveCard(CallingCardEditorActivity.this, number, savedLocalPath);
                Toast.makeText(CallingCardEditorActivity.this, "Calling Card Saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(CallingCardEditorActivity.this, "Failed to save image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                ivPreview.setImageURI(selectedImageUri);
                tvEmptyHint.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Copies the selected Gallery URI to a private local directory so it persists forever.
     */
    private String copyImageToInternalStorage(Uri sourceUri, String phoneNumber) {
        try {
            // Create a dedicated directory: /data/data/com.android.dialer/files/calling_cards
            File directory = new File(getFilesDir(), "calling_cards");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create the file named after the phone number
            File destinationFile = new File(directory, phoneNumber.replaceAll("[^0-9+]", "") + ".jpg");

            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // Return the local file URI to be saved in the SQLite database
            return Uri.fromFile(destinationFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
