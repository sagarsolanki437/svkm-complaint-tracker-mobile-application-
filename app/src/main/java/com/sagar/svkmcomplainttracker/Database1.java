package com.sagar.svkmcomplainttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Stores student complaints and the warden review details on this device.
public class Database1 extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SVKMComplaints.db";
    private static final int DATABASE_VERSION = 2;

    public Database1(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE complaints (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, student_name TEXT NOT NULL, " +
                "sap_id TEXT NOT NULL, branch TEXT NOT NULL, year TEXT NOT NULL, " +
                "phone TEXT NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'Pending', " +
                "warden_name TEXT, warden_reply TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_name TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_reply TEXT");
        }
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
                "SELECT id, category, description, created_at, status, warden_reply FROM complaints " +
                        "WHERE sap_id = ? ORDER BY id DESC", new String[]{sapId});
    }

    public Cursor getUnresolvedComplaints() {
        return getReadableDatabase().rawQuery(
                "SELECT id, student_name, category FROM complaints " +
                        "WHERE status = 'Pending' ORDER BY id DESC", null);
    }

    public Cursor getComplaint(long complaintId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM complaints WHERE id = ?", new String[]{String.valueOf(complaintId)});
    }
}
