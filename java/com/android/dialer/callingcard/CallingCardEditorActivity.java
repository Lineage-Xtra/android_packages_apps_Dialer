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
import android.widget.Toast;

import com.android.dialer.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CallingCardEditorActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    public static final String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";

    private EditText etPhoneNumber;
    private ImageView ivPreview;
    private Uri selectedImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling_card_editor);

        etPhoneNumber = findViewById(R.id.et_phone_number);
        ivPreview = findViewById(R.id.iv_preview);
        Button btnPickImage = findViewById(R.id.btn_pick_image);
        Button btnSave = findViewById(R.id.btn_save_card);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_PHONE_NUMBER)) {
            String passedNumber = getIntent().getStringExtra(EXTRA_PHONE_NUMBER);
            if (passedNumber != null && !passedNumber.trim().isEmpty()) {
                etPhoneNumber.setText(passedNumber.trim());
                etPhoneNumber.setEnabled(false);

                String existingUriString = CallingCardManager.getCardUri(this, passedNumber.trim());
                if (existingUriString != null) {
                    selectedImageUri = Uri.parse(existingUriString);
                    ivPreview.setImageURI(selectedImageUri);
                }
            }
        }

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = etPhoneNumber.getText().toString().trim();
                if (number.isEmpty() || selectedImageUri == null) {
                    Toast.makeText(CallingCardEditorActivity.this,
                        "Please ensure a phone number and background image are specified",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                String savedLocalPath = copyImageToInternalStorage(selectedImageUri, number);

                if (savedLocalPath != null) {
                    CallingCardManager.saveCard(CallingCardEditorActivity.this, number, savedLocalPath);
                    Toast.makeText(CallingCardEditorActivity.this, "Calling Card Updated Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CallingCardEditorActivity.this, "Failed to save image.", Toast.LENGTH_SHORT).show();
                }
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
