package com.sagar.svkmcomplainttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Stores student complaints and the warden review details on this device.
public class Database1 extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SVKMComplaints.db";
    private static final int DATABASE_VERSION = 4;

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
                "warden_name TEXT, warden_reply TEXT, resolved_at INTEGER NOT NULL DEFAULT 0)");
        createWardenSessionsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_name TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_reply TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE complaints ADD COLUMN resolved_at INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) createWardenSessionsTable(db);
    }

    private void createWardenSessionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS warden_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, warden_name TEXT NOT NULL, " +
                "login_time INTEGER NOT NULL)");
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

    public void recordWardenLogin(String wardenName, long loginTime) {
        ContentValues values = new ContentValues();
        values.put("warden_name", wardenName);
        values.put("login_time", loginTime);
        getWritableDatabase().insert("warden_sessions", null, values);
    }

    public long getLatestWardenLoginTime() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT login_time FROM warden_sessions ORDER BY id DESC LIMIT 1", null);
        try {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public Cursor getAllComplaints() {
        return getReadableDatabase().rawQuery("SELECT * FROM complaints ORDER BY id DESC", null);
    }

    public Cursor searchAllComplaints(String searchText) {
        String search = "%" + searchText.trim() + "%";
        return getReadableDatabase().rawQuery(
                "SELECT * FROM complaints WHERE student_name LIKE ? COLLATE NOCASE " +
                        "OR sap_id LIKE ? COLLATE NOCASE OR category LIKE ? COLLATE NOCASE " +
                        "OR warden_name LIKE ? COLLATE NOCASE ORDER BY id DESC",
                new String[]{search, search, search, search});
    }

    // Saves the warden's response and resolves a pending complaint atomically.
    public boolean resolveComplaint(long complaintId, String wardenName, String reply,
                                    long resolvedAt) {
        if (wardenName == null || wardenName.trim().isEmpty() ||
                reply == null || reply.trim().isEmpty()) return false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", "Resolved");
        values.put("warden_name", wardenName.trim());
        values.put("warden_reply", reply.trim());
        values.put("resolved_at", resolvedAt);

        db.beginTransaction();
        try {
            if (db.update("complaints", values, "id = ? AND status = 'Pending'",
                    new String[]{String.valueOf(complaintId)}) != 1) return false;
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }
}
