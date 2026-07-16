/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.dialer.callingcard;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CallingCardDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "calling_cards.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "cards";
    public static final String COLUMN_NUMBER = "phone_number";
    public static final String COLUMN_IMAGE_URI = "image_uri";

    public CallingCardDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_NUMBER + " TEXT PRIMARY KEY, " +
                COLUMN_IMAGE_URI + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
