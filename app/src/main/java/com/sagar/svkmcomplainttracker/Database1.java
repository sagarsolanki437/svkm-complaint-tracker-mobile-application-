package com.sagar.svkmcomplainttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Keeps complaint records, warden login times, and resolved complaint history on this device.
public class Database1 extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SVKMComplaints.db";
    private static final int DATABASE_VERSION = 7;

    public Database1(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creates one complete row for each complaint; student details are copied into every row.
    @Override
    public void onCreate(SQLiteDatabase db) {
        createComplaintTable(db);
        createWardenTables(db);
    }

    // Adds new columns/tables without deleting records saved by earlier app versions.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // One-time clean slate requested for the new use case; future upgrades preserve entries.
        if (oldVersion < 7) {
            db.execSQL("DROP TABLE IF EXISTS complaints");
            db.execSQL("DROP TABLE IF EXISTS students");
            db.execSQL("DROP TABLE IF EXISTS warden_sessions");
            db.execSQL("DROP TABLE IF EXISTS warden_log");
            createComplaintTable(db);
            createWardenTables(db);
            return;
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE complaints ADD COLUMN student_name TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN sap_id TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN branch TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN year TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN phone TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_name TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_reply TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_opened_at TEXT");
            db.execSQL("ALTER TABLE complaints ADD COLUMN is_resolved INTEGER DEFAULT 0");
            db.execSQL("UPDATE complaints SET " +
                    "student_name = (SELECT student_name FROM students WHERE students.id = complaints.student_id), " +
                    "sap_id = (SELECT sap_id FROM students WHERE students.id = complaints.student_id), " +
                    "branch = (SELECT branch FROM students WHERE students.id = complaints.student_id), " +
                    "year = (SELECT year FROM students WHERE students.id = complaints.student_id), " +
                    "phone = (SELECT phone FROM students WHERE students.id = complaints.student_id)");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS warden_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, warden_name TEXT, login_time TEXT)");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE complaints ADD COLUMN created_at INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE complaints ADD COLUMN resolved_at INTEGER DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS warden_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, warden_name TEXT, student_name TEXT, " +
                    "sap_id TEXT, branch TEXT, year TEXT, phone TEXT, category TEXT, description TEXT, " +
                    "complaint_date INTEGER, resolved_at INTEGER)");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE complaints ADD COLUMN warden_replied_at TEXT");
        }
    }

    private void createComplaintTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE complaints (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, student_name TEXT, sap_id TEXT, " +
                "branch TEXT, year TEXT, phone TEXT, category TEXT, description TEXT, " +
                "warden_name TEXT, warden_reply TEXT, warden_opened_at TEXT, " +
                "is_resolved INTEGER DEFAULT 0, created_at INTEGER DEFAULT 0, resolved_at INTEGER DEFAULT 0, " +
                "warden_replied_at TEXT)");
    }

    private void createWardenTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE warden_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, warden_name TEXT, login_time TEXT)");
        db.execSQL("CREATE TABLE warden_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, warden_name TEXT, student_name TEXT, " +
                "sap_id TEXT, branch TEXT, year TEXT, phone TEXT, category TEXT, description TEXT, " +
                "complaint_date INTEGER, resolved_at INTEGER)");
    }

    // Inserts a new independent complaint record when the student presses Post.
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
        values.put("is_resolved", 0);
        values.put("created_at", createdAt);
        return getWritableDatabase().insert("complaints", null, values);
    }

    // Short warden list contains only the student name and complaint category.
    public Cursor getUnresolvedComplaints() {
        return getReadableDatabase().rawQuery(
                "SELECT id, student_name, category FROM complaints WHERE is_resolved = 0 ORDER BY id DESC", null);
    }

    // Filters unresolved complaints by student, SAP ID, category, or assigned warden.
    public Cursor searchUnresolvedComplaints(String searchText) {
        String search = "%" + searchText + "%";
        return getReadableDatabase().rawQuery(
                "SELECT id, student_name, category FROM complaints " +
                        "WHERE is_resolved = 0 AND (student_name LIKE ? COLLATE NOCASE OR sap_id LIKE ? COLLATE NOCASE " +
                        "OR category LIKE ? COLLATE NOCASE OR warden_name LIKE ? COLLATE NOCASE) ORDER BY id DESC",
                new String[]{search, search, search, search});
    }

    // Faculty can inspect both pending and resolved records.
    public Cursor getAllComplaints() {
        return getReadableDatabase().rawQuery("SELECT * FROM complaints ORDER BY id DESC", null);
    }

    // Faculty can search the full current complaint list without changing any records.
    public Cursor searchAllComplaints(String searchText) {
        String search = "%" + searchText + "%";
        return getReadableDatabase().rawQuery(
                "SELECT * FROM complaints WHERE student_name LIKE ? COLLATE NOCASE OR sap_id LIKE ? COLLATE NOCASE " +
                        "OR category LIKE ? COLLATE NOCASE OR warden_name LIKE ? COLLATE NOCASE ORDER BY id DESC",
                new String[]{search, search, search, search});
    }

    // Returns every complaint submitted with the entered student SAP ID.
    public Cursor getStudentComplaints(String sapId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM complaints WHERE sap_id = ? ORDER BY id DESC", new String[]{sapId});
    }

    // Loads one complaint so the warden can view its full details.
    public Cursor getComplaint(long complaintId) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM complaints WHERE id = ?", new String[]{String.valueOf(complaintId)});
    }

    // Saves each successful warden login time for the Faculty supervision screen.
    public void recordWardenLogin(String wardenName, String loginTime) {
        ContentValues session = new ContentValues();
        session.put("warden_name", wardenName);
        session.put("login_time", loginTime);
        getWritableDatabase().insert("warden_sessions", null, session);
    }

    // Associates a warden and access time with the specific complaint they opened.
    public void recordWardenOpenedComplaint(long complaintId, String wardenName, String openedAt) {
        ContentValues values = new ContentValues();
        values.put("warden_name", wardenName);
        values.put("warden_opened_at", openedAt);
        getWritableDatabase().update("complaints", values, "id = ?",
                new String[]{String.valueOf(complaintId)});
    }

    // Archives the resolved item in that warden's log, then marks the original row resolved.
    public boolean resolveComplaint(long complaintId, String wardenName, String reply, long resolvedAt) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor complaint = getComplaint(complaintId);
        if (!complaint.moveToFirst()) {
            complaint.close();
            return false;
        }

        ContentValues log = new ContentValues();
        log.put("warden_name", wardenName);
        log.put("student_name", complaint.getString(complaint.getColumnIndexOrThrow("student_name")));
        log.put("sap_id", complaint.getString(complaint.getColumnIndexOrThrow("sap_id")));
        log.put("branch", complaint.getString(complaint.getColumnIndexOrThrow("branch")));
        log.put("year", complaint.getString(complaint.getColumnIndexOrThrow("year")));
        log.put("phone", complaint.getString(complaint.getColumnIndexOrThrow("phone")));
        log.put("category", complaint.getString(complaint.getColumnIndexOrThrow("category")));
        log.put("description", complaint.getString(complaint.getColumnIndexOrThrow("description")));
        log.put("complaint_date", complaint.getLong(complaint.getColumnIndexOrThrow("created_at")));
        log.put("resolved_at", resolvedAt);
        complaint.close();

        db.beginTransaction();
        try {
            if (db.insert("warden_log", null, log) == -1) return false;
            ContentValues update = new ContentValues();
            update.put("warden_name", wardenName);
            update.put("warden_reply", reply);
            update.put("is_resolved", 1);
            update.put("resolved_at", resolvedAt);
            update.put("warden_replied_at", String.valueOf(resolvedAt));
            if (db.update("complaints", update, "id = ? AND is_resolved = 0",
                    new String[]{String.valueOf(complaintId)}) == 0) return false;
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // Warden history is filtered by name and includes only complaints that warden resolved.
    public Cursor getWardenLog(String wardenName) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM warden_log WHERE warden_name = ? ORDER BY id DESC", new String[]{wardenName});
    }

    // Removes resolved complaints from the active table after 48 hours; the warden log remains.
    public int deleteExpiredResolvedComplaints(long currentTime) {
        long cutoff = currentTime - (48L * 60L * 60L * 1000L);
        return getWritableDatabase().delete("complaints",
                "is_resolved = 1 AND resolved_at > 0 AND resolved_at <= ?",
                new String[]{String.valueOf(cutoff)});
    }

    // Provides the most recent successful warden login timestamp to faculty.
    public String getLatestWardenLoginTime() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT login_time FROM warden_sessions ORDER BY id DESC LIMIT 1", null);
        try {
            return cursor.moveToFirst() ? cursor.getString(0) : "Not recorded";
        } finally {
            cursor.close();
        }
    }
}
