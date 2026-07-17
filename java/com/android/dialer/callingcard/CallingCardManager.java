/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dialer.callingcard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.PhoneNumberUtils;

public class CallingCardManager {

    // Helper to strip spaces, dashes, and parentheses for cleaner DB storage
    private static String normalizeNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[^0-9+]", "");
    }

    public static void saveCard(Context context, String number, String imageUri) {
        if (number == null || imageUri == null) return;

        String cleanNumber = normalizeNumber(number);
        CallingCardDatabaseHelper dbHelper = new CallingCardDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CallingCardDatabaseHelper.COLUMN_NUMBER, cleanNumber);
        values.put(CallingCardDatabaseHelper.COLUMN_IMAGE_URI, imageUri);

        db.replace(CallingCardDatabaseHelper.TABLE_NAME, null, values);
        db.close();
    }

    public static String getCardUri(Context context, String number) {
        if (number == null) return null;

        CallingCardDatabaseHelper dbHelper = new CallingCardDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Fetch ALL saved calling cards instead of doing a strict SQL string match
        Cursor cursor = db.query(CallingCardDatabaseHelper.TABLE_NAME,
                new String[]{CallingCardDatabaseHelper.COLUMN_NUMBER, CallingCardDatabaseHelper.COLUMN_IMAGE_URI},
                null, null, null, null, null);

        String uri = null;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String dbNumber = cursor.getString(0);

                // Use Android's powerful Telecom comparator to fuzzy-match the numbers
                if (PhoneNumberUtils.compare(number, dbNumber)) {
                    uri = cursor.getString(1);
                    break;
                }
            }
            cursor.close();
        }
        db.close();
        return uri;
    }

    public static void deleteCard(Context context, String number) {
        if (number == null) return;

        CallingCardDatabaseHelper dbHelper = new CallingCardDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Find the exact number key and URI to delete the physical file
        Cursor cursor = db.query(CallingCardDatabaseHelper.TABLE_NAME,
                new String[]{CallingCardDatabaseHelper.COLUMN_NUMBER, CallingCardDatabaseHelper.COLUMN_IMAGE_URI},
                null, null, null, null, null);

        String exactDbKey = null;
        String uriString = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String dbNumber = cursor.getString(0);
                if (PhoneNumberUtils.compare(number, dbNumber)) {
                    exactDbKey = dbNumber;
                    uriString = cursor.getString(1);
                    break;
                }
            }
            cursor.close();
        }

        // Delete the physical image file
        if (uriString != null) {
            try {
                android.net.Uri uri = android.net.Uri.parse(uriString);
                java.io.File file = new java.io.File(uri.getPath());
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Delete the database record
        if (exactDbKey != null) {
            db.delete(CallingCardDatabaseHelper.TABLE_NAME,
                    CallingCardDatabaseHelper.COLUMN_NUMBER + " = ?",
                    new String[]{exactDbKey});
        }
        db.close();
    }
}
