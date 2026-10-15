package com.sagar.svkmcomplainttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Stores student complaint submissions on this device.
public class Database1 extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SVKMComplaints.db";
    private static final int DATABASE_VERSION = 1;

    public Database1(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE complaints (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_name TEXT NOT NULL, sap_id TEXT NOT NULL, branch TEXT NOT NULL, " +
                "year TEXT NOT NULL, phone TEXT NOT NULL, category TEXT NOT NULL, " +
                "description TEXT NOT NULL, created_at INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'Pending')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future versions will add non-destructive schema migrations here.
    }

    public long saveComplaint(String name, String sapId, String branch, String year,
                              String phone, String category, String description, long createdAt) {
        ContentValues values = new ContentValues();
        values.put("student_name", name);
        values.put("sap_id", sapId);
        values.put("branch", branch);
        values.put("year", year);
        values.put("phone", phone);
        values.put("category", category);
        values.put("description", description);
        values.put("created_at", createdAt);
        values.put("status", "Pending");
        return getWritableDatabase().insert("complaints", null, values);
    }

    public Cursor getStudentComplaints(String sapId) {
        return getReadableDatabase().rawQuery(
                "SELECT id, category, description, created_at, status FROM complaints " +
                        "WHERE sap_id = ? ORDER BY id DESC", new String[]{sapId});
    }
}
