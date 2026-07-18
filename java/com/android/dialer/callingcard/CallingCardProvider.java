/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dialer.callingcard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class CallingCardProvider extends ContentProvider {
    public static final String AUTHORITY = "com.android.dialer.callingcard";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String number = uri.getLastPathSegment();
        if (number == null) return null;

        // Check the local Dialer database for the card
        String cardUri = CallingCardManager.getCardUri(getContext(), number);

        // Return 1 row if the card exists, or an empty cursor if it doesn't
        MatrixCursor cursor = new MatrixCursor(new String[]{"has_card"});
        if (cardUri != null) {
            cursor.addRow(new Object[]{1});
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) { return null; }

    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
