/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dialer.callingcard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dialer.R;

import java.io.File;
import java.io.FileOutputStream;

public class CallingCardEditorActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    public static final String EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER";
    public static final String EXTRA_DELETE_CARD = "EXTRA_DELETE_CARD";
    public static final String EXTRA_VIEW_ONLY = "EXTRA_VIEW_ONLY";

    private EditText etPhoneNumber;
    private ImageView ivPreview;
    private TextView tvEmptyHint;
    private Uri selectedImageUri = null;
    private boolean isViewOnly = false;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF start = new PointF();
    private float[] matrixValues = new float[9];
    private float mMinScale = 1f;

    private ScaleGestureDetector scaleDetector;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

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
        TextView tvTitle = findViewById(R.id.tv_title);

        // Convert the ImageView to use a Matrix
        ivPreview.setScaleType(ImageView.ScaleType.MATRIX);

        isViewOnly = getIntent() != null && getIntent().getBooleanExtra(EXTRA_VIEW_ONLY, false);

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
                    loadImageIntoPreview(selectedImageUri);
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
            findViewById(R.id.action_buttons_container).setVisibility(View.GONE);

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
        ivPreview.setOnClickListener(pickImageListener);

        setupImagePanningAndZooming();

        btnSave.setOnClickListener(v -> {
            String number = etPhoneNumber.getText().toString().trim();
            if (number.isEmpty() || selectedImageUri == null) {
                Toast.makeText(CallingCardEditorActivity.this,
                    "Please select a background image first",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            String savedLocalPath = captureAndSaveImage(number);

            if (savedLocalPath != null) {
                CallingCardManager.saveCard(CallingCardEditorActivity.this, number, savedLocalPath);
                Toast.makeText(CallingCardEditorActivity.this, "Calling Card Saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(CallingCardEditorActivity.this, "Failed to save image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImageIntoPreview(Uri uri) {
        ivPreview.setImageURI(uri);
        tvEmptyHint.setVisibility(View.GONE);

        ivPreview.post(() -> {
            Drawable drawable = ivPreview.getDrawable();
            if (drawable == null) return;

            int dwidth = drawable.getIntrinsicWidth();
            int dheight = drawable.getIntrinsicHeight();
            int vwidth = ivPreview.getWidth();
            int vheight = ivPreview.getHeight();

            float scale;
            float dx = 0, dy = 0;

            if (dwidth * vheight > vwidth * dheight) {
                scale = (float) vheight / (float) dheight;
                dx = (vwidth - dwidth * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) dwidth;
                dy = (vheight - dheight * scale) * 0.5f;
            }

            mMinScale = scale;
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            ivPreview.setImageMatrix(matrix);
        });
    }

    private void setupImagePanningAndZooming() {
        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                matrix.getValues(matrixValues);
                float currentScale = matrixValues[Matrix.MSCALE_X];

                // Prevent zooming out past bounds, or zooming in insanely far
                if ((currentScale <= mMinScale && scaleFactor < 1) || (currentScale >= mMinScale * 5 && scaleFactor > 1)) {
                    return true;
                }

                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                checkAndFixBounds();
                ivPreview.setImageMatrix(matrix);
                return true;
            }
        });

        ivPreview.setOnTouchListener((v, event) -> {
            if (isViewOnly) return false;

            scaleDetector.onTouchEvent(event);

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ZOOM;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        float dx = event.getX() - start.x;
                        float dy = event.getY() - start.y;
                        matrix.postTranslate(dx, dy);
                        checkAndFixBounds();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // Treat short drags as standard clicks to allow the picker to open
                    float dxUp = Math.abs(event.getX() - start.x);
                    float dyUp = Math.abs(event.getY() - start.y);
                    if (mode == DRAG && dxUp < 15 && dyUp < 15) {
                        v.performClick();
                    }
                    mode = NONE;
                    break;
            }
            ivPreview.setImageMatrix(matrix);
            return true;
        });
    }

    private void checkAndFixBounds() {
        matrix.getValues(matrixValues);
        float currentX = matrixValues[Matrix.MTRANS_X];
        float currentY = matrixValues[Matrix.MTRANS_Y];
        float currentScale = matrixValues[Matrix.MSCALE_X];

        Drawable d = ivPreview.getDrawable();
        if (d == null) return;

        float scaledWidth = d.getIntrinsicWidth() * currentScale;
        float scaledHeight = d.getIntrinsicHeight() * currentScale;

        float minX = ivPreview.getWidth() - scaledWidth;
        float minY = ivPreview.getHeight() - scaledHeight;

        // Ensure bounds are always negative or zero
        if (minX > 0) minX = 0;
        if (minY > 0) minY = 0;

        float dx = 0, dy = 0;

        if (currentX > 0) dx = -currentX;
        else if (currentX < minX) dx = minX - currentX;

        if (currentY > 0) dy = -currentY;
        else if (currentY < minY) dy = minY - currentY;

        matrix.postTranslate(dx, dy);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                loadImageIntoPreview(selectedImageUri);
            }
        }
    }

    /**
     * Snapshots the exact visible portion of the ImageView and saves it.
     */
    private String captureAndSaveImage(String phoneNumber) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(ivPreview.getWidth(), ivPreview.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            ivPreview.draw(canvas);

            // Create a dedicated directory: /data/data/com.android.dialer/files/calling_cards
            File directory = new File(getFilesDir(), "calling_cards");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create the file named after the phone number
            File destinationFile = new File(directory, phoneNumber.replaceAll("[^0-9+]", "") + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            // Compress to JPEG and save
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();

            // Return the local file URI to be saved in the SQLite database
            return Uri.fromFile(destinationFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
