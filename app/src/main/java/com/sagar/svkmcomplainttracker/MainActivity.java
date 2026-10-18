package com.sagar.svkmcomplainttracker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Database1 database;
    private LinearLayout homeSection;
    private LinearLayout studentSection;
    private LinearLayout categorySection;
    private LinearLayout descriptionSection;
    private LinearLayout studentStatusSection;
    private LinearLayout wardenLoginSection;
    private LinearLayout wardenDashboardSection;
    private LinearLayout wardenLogSection;
    private LinearLayout wardenDetailSection;
    private LinearLayout facultyLoginSection;
    private LinearLayout facultySection;
    private LinearLayout facultyDetailSection;
    private String selectedCategory = "";
    private String currentWardenName = "";
    private long currentComplaintId = -1;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 10;
    private static final String NOTIFICATION_CHANNEL_ID = "svkm_complaint_updates";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        database = new Database1(this);
        database.deleteExpiredResolvedComplaints(System.currentTimeMillis());
        createNotificationChannel();
        requestNotificationPermission();

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton menuButton = findViewById(R.id.menuButton);
        LinearLayout navigationDrawer = findViewById(R.id.navigationDrawer);

        homeSection = findViewById(R.id.homeSection);
        studentSection = findViewById(R.id.studentSection);
        categorySection = findViewById(R.id.categorySection);
        descriptionSection = findViewById(R.id.descriptionSection);
        studentStatusSection = findViewById(R.id.studentStatusSection);
        wardenLoginSection = findViewById(R.id.wardenLoginSection);
        wardenDashboardSection = findViewById(R.id.wardenDashboardSection);
        wardenLogSection = findViewById(R.id.wardenLogSection);
        wardenDetailSection = findViewById(R.id.wardenDetailSection);
        facultyLoginSection = findViewById(R.id.facultyLoginSection);
        facultySection = findViewById(R.id.facultySection);
        facultyDetailSection = findViewById(R.id.facultyDetailSection);

        // Each home choice opens its own screen; the Back arrow returns to the home screen.
        findViewById(R.id.studentOption).setOnClickListener(v -> showSection(studentSection));
        findViewById(R.id.facultyOption).setOnClickListener(v -> showSection(facultyLoginSection));
        findViewById(R.id.wardenOption).setOnClickListener(v -> showSection(wardenLoginSection));

        // Students can look up their own stored complaint records by SAP ID.
        findViewById(R.id.checkStatusButton).setOnClickListener(v -> showSection(studentStatusSection));
        findViewById(R.id.studentNextButton).setOnClickListener(v -> validateStudentDetails());

        TextView complaintTitle = findViewById(R.id.complaintTitle);
        int[] categoryButtonIds = {R.id.hostelOption, R.id.messOption, R.id.theftOption,
                R.id.staffBehaviorOption, R.id.academicsOption};
        String[] categoryNames = {"Hostel", "Mess", "Theft Complaint", "Staff Behavior", "Academics Problem"};
        for (int i = 0; i < categoryButtonIds.length; i++) {
            final String categoryName = categoryNames[i];
            findViewById(categoryButtonIds[i]).setOnClickListener(v -> {
                selectedCategory = categoryName;
                complaintTitle.setText(categoryName);
                showSection(descriptionSection);
            });
        }

        findViewById(R.id.postButton).setOnClickListener(v -> saveStudentComplaint());
        findViewById(R.id.statusSearchButton).setOnClickListener(v -> loadStudentComplaints());
        // The warden log is available only after a successful warden login.
        findViewById(R.id.wardenEnterButton).setOnClickListener(v -> wardenLogin());
        findViewById(R.id.facultyNextButton).setOnClickListener(v -> facultyLogin());
        findViewById(R.id.resolveButton).setOnClickListener(v -> resolveCurrentComplaint());
        findViewById(R.id.wardenLogButton).setOnClickListener(v -> {
            loadWardenLog();
            showSection(wardenLogSection);
        });
        findViewById(R.id.wardenSearchButton).setOnClickListener(v ->
                loadWardenComplaints(((EditText) findViewById(R.id.wardenSearchInput)).getText().toString().trim()));
        findViewById(R.id.facultySearchButton).setOnClickListener(v ->
                loadFacultyComplaints(((EditText) findViewById(R.id.facultySearchInput)).getText().toString().trim()));
        findViewById(R.id.wardenLogoutButton).setOnClickListener(v -> logoutWarden());
        findViewById(R.id.facultyLogoutButton).setOnClickListener(v -> logoutFaculty());

        // Refreshes the warden complaint list while a search phrase is being entered.
        EditText wardenSearchInput = findViewById(R.id.wardenSearchInput);
        wardenSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (wardenDashboardSection.getVisibility() == View.VISIBLE) {
                    loadWardenComplaints(s.toString().trim());
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        EditText replyInput = findViewById(R.id.wardenReplyInput);
        Button resolveButton = findViewById(R.id.resolveButton);
        replyInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                resolveButton.setEnabled(s.toString().trim().length() > 0);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(navigationDrawer));
        backButton.setOnClickListener(v -> goBack());
    }

    // Checks every required student field before allowing the student to choose a category.
    private void validateStudentDetails() {
        EditText name = findViewById(R.id.studentNameInput);
        EditText sap = findViewById(R.id.sapIdInput);
        EditText phone = findViewById(R.id.phoneInput);
        Spinner branch = findViewById(R.id.branchSpinner);
        Spinner year = findViewById(R.id.yearSpinner);
        boolean valid = true;

        if (name.getText().toString().trim().isEmpty()) {
            name.setError("Please fill the details");
            valid = false;
        }
        if (sap.getText().toString().trim().isEmpty()) {
            sap.setError("Please fill the details");
            valid = false;
        }
        if (phone.getText().toString().trim().length() != 10) {
            phone.setError("Please Enter a 10 digit phone number");
            valid = false;
        }
        if (branch.getSelectedItemPosition() == 0 || year.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select branch and year", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (valid) showSection(categorySection);
        else Toast.makeText(this, "Please complete all student details", Toast.LENGTH_SHORT).show();
    }

    // Saves the student details and complaint together as a new independent SQLite row.
    private void saveStudentComplaint() {
        EditText description = findViewById(R.id.complaintDescriptionInput);
        String complaintText = description.getText().toString().trim();
        if (complaintText.isEmpty()) {
            description.setError("Enter complaint details");
            return;
        }

        EditText name = findViewById(R.id.studentNameInput);
        EditText sap = findViewById(R.id.sapIdInput);
        EditText phone = findViewById(R.id.phoneInput);
        Spinner branch = findViewById(R.id.branchSpinner);
        Spinner year = findViewById(R.id.yearSpinner);
        long result = database.saveComplaint(name.getText().toString().trim(),
                sap.getText().toString().trim(), branch.getSelectedItem().toString(),
                year.getSelectedItem().toString(), phone.getText().toString().trim(),
                selectedCategory, complaintText, System.currentTimeMillis());

        if (result != -1) {
            Toast.makeText(this, "Complaint saved. Reference ID: " + result, Toast.LENGTH_LONG).show();
            description.setText("");
            name.setText("");
            sap.setText("");
            phone.setText("");
            branch.setSelection(0);
            year.setSelection(0);
            selectedCategory = "";
            showSection(homeSection);
        } else Toast.makeText(this, "Could not save complaint", Toast.LENGTH_SHORT).show();
    }

    // Checks the fixed demonstration password and records when the dashboard was opened.
    private void wardenLogin() {
        EditText nameInput = findViewById(R.id.wardenNameInput);
        EditText passwordInput = findViewById(R.id.wardenPasswordInput);
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameInput.setError("Enter warden name");
            return;
        }
        if (!passwordInput.getText().toString().equals("warden1234")) {
            passwordInput.setError("Incorrect password");
            return;
        }

        currentWardenName = name;
        String now = currentTimestamp();
        database.recordWardenLogin(currentWardenName, now);
        passwordInput.setText("");
        ((EditText) findViewById(R.id.wardenSearchInput)).setText("");
        loadWardenComplaints();
        showSection(wardenDashboardSection);
    }

    // Faculty uses the same fixed demonstration password and gets read-only access.
    private void facultyLogin() {
        EditText name = findViewById(R.id.facultyNameInput);
        EditText password = findViewById(R.id.facultyPasswordInput);
        if (name.getText().toString().trim().isEmpty()) {
            name.setError("Enter faculty name");
            return;
        }
        if (!password.getText().toString().equals("faculty1234")) {
            password.setError("Incorrect password");
            return;
        }
        password.setText("");
        database.deleteExpiredResolvedComplaints(System.currentTimeMillis());
        loadFacultyComplaints();
        showSection(facultySection);
    }

    private void loadWardenComplaints() {
        loadWardenComplaints("");
    }

    // Reloads the warden list, optionally matching a simple search phrase.
    private void loadWardenComplaints(String searchText) {
        database.deleteExpiredResolvedComplaints(System.currentTimeMillis());
        LinearLayout list = findViewById(R.id.wardenComplaintList);
        list.removeAllViews();
        Cursor cursor = database.searchUnresolvedComplaints(searchText);
        try {
            if (!cursor.moveToFirst()) {
                addMessage(list, searchText.isEmpty()
                        ? "There are no unresolved complaints."
                        : "No unresolved complaints match your search.");
                return;
            }
            do {
                long complaintId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String studentName = cursor.getString(cursor.getColumnIndexOrThrow("student_name"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                TextView row = makeRow(studentName + "  •  " + category);
                row.setOnClickListener(v -> openWardenComplaint(complaintId));
                list.addView(row);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    private void openWardenComplaint(long complaintId) {
        currentComplaintId = complaintId;
        database.recordWardenOpenedComplaint(complaintId, currentWardenName, currentTimestamp());
        Cursor cursor = database.getComplaint(complaintId);
        try {
            if (cursor.moveToFirst()) {
                TextView details = findViewById(R.id.wardenComplaintDetails);
                details.setText(complaintDetails(cursor, true));
                EditText reply = findViewById(R.id.wardenReplyInput);
                reply.setText("");
                findViewById(R.id.resolveButton).setEnabled(false);
                showSection(wardenDetailSection);
            }
        } finally {
            cursor.close();
        }
    }

    private void resolveCurrentComplaint() {
        EditText replyInput = findViewById(R.id.wardenReplyInput);
        String reply = replyInput.getText().toString().trim();
        if (reply.isEmpty()) {
            replyInput.setError("Enter the action taken before resolving");
            return;
        }
        long resolvedAt = System.currentTimeMillis();
        if (database.resolveComplaint(currentComplaintId, currentWardenName, reply, resolvedAt)) {
            scheduleComplaintCleanup(currentComplaintId, resolvedAt);
            notifyStudentComplaintResolved(currentComplaintId);
            Toast.makeText(this, "Complaint resolved", Toast.LENGTH_SHORT).show();
            loadWardenComplaints();
            showSection(wardenDashboardSection);
        }
    }

    private void loadStudentComplaints() {
        database.deleteExpiredResolvedComplaints(System.currentTimeMillis());
        EditText sapInput = findViewById(R.id.statusSapInput);
        String sapId = sapInput.getText().toString().trim();
        if (sapId.isEmpty()) {
            sapInput.setError("Enter your SAP ID");
            return;
        }
        LinearLayout list = findViewById(R.id.studentStatusList);
        list.removeAllViews();
        Cursor cursor = database.getStudentComplaints(sapId);
        try {
            if (!cursor.moveToFirst()) {
                addMessage(list, "No complaints found for this SAP ID.");
                return;
            }
            do {
                String status = complaintStatus(cursor);
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                String reply = cursor.getString(cursor.getColumnIndexOrThrow("warden_reply"));
                String replyTime = cursor.getString(cursor.getColumnIndexOrThrow("warden_replied_at"));
                TextView row = makeRow(category + "\nComplaint: " + description +
                        "\nStatus: " + status + "\nWarden response: " + valueOrNone(reply) +
                        "\nResponse time: " + formatTimestamp(replyTime));
                row.setTextColor(statusColor(status));
                list.addView(row);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    private void loadFacultyComplaints() {
        loadFacultyComplaints("");
    }

    // Reloads the faculty list with optional matching on the name, ID, category, or warden.
    private void loadFacultyComplaints(String searchText) {
        database.deleteExpiredResolvedComplaints(System.currentTimeMillis());
        LinearLayout list = findViewById(R.id.facultyComplaintList);
        list.removeAllViews();
        TextView latestLogin = findViewById(R.id.facultyLatestLogin);
        latestLogin.setText("Latest warden dashboard login: " + database.getLatestWardenLoginTime());
        Cursor cursor = database.searchAllComplaints(searchText);
        try {
            if (!cursor.moveToFirst()) {
                addMessage(list, "No complaints have been submitted.");
                return;
            }
            do {
                long complaintId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String studentName = valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("student_name")));
                String category = valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("category")));
                String wardenName = cursor.getString(cursor.getColumnIndexOrThrow("warden_name"));
                String status = complaintStatus(cursor);
                String timer = "";
                if (status.equals("Resolved")) {
                    long resolvedAt = cursor.getLong(cursor.getColumnIndexOrThrow("resolved_at"));
                    timer = "\nRemoval in: " + remainingTime(resolvedAt);
                }
                TextView row = makeRow(studentName + "\nCategory: " + category +
                        "\nWarden: " + (wardenName == null ? "" : wardenName) +
                        "\nStatus: " + status + timer);
                row.setTextColor(statusColor(status));
                row.setOnClickListener(v -> openFacultyComplaint(complaintId));
                list.addView(row);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    // Shows full faculty details only after a compact complaint row is selected.
    private void openFacultyComplaint(long complaintId) {
        Cursor cursor = database.getComplaint(complaintId);
        try {
            if (cursor.moveToFirst()) {
                String status = complaintStatus(cursor);
                String details = complaintDetails(cursor, false) + "\nStatus: " + status;
                TextView view = findViewById(R.id.facultyComplaintDetails);
                view.setText(details);
                view.setTextColor(statusColor(status));
                showSection(facultyDetailSection);
            }
        } finally {
            cursor.close();
        }
    }

    // Shows only the signed-in warden's resolved complaints, with complaint text and registration time.
    private void loadWardenLog() {
        LinearLayout list = findViewById(R.id.wardenLogList);
        list.removeAllViews();
        Cursor cursor = database.getWardenLog(currentWardenName);
        try {
            if (!cursor.moveToFirst()) {
                addMessage(list, "You have not resolved any complaints yet.");
                return;
            }
            do {
                String student = valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("student_name")));
                String category = valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("category")));
                String description = valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                long registered = cursor.getLong(cursor.getColumnIndexOrThrow("complaint_date"));
                list.addView(makeRow("Student: " + student + "\nCategory: " + category +
                        "\nDescription: " + description + "\nRegistered: " + formatDate(registered)));
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    private String complaintDetails(Cursor cursor, boolean includeDescription) {
        StringBuilder details = new StringBuilder();
        details.append("Student: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("student_name"))));
        details.append("\nSAP ID: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("sap_id"))));
        details.append("\nBranch: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("branch"))));
        details.append("\nYear: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("year"))));
        details.append("\nPhone: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("phone"))));
        details.append("\nCategory: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("category"))));
        if (includeDescription) {
            details.append("\nComplaint: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("description"))));
        } else {
            details.append("\nComplaint: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("description"))));
            details.append("\nWarden opened at: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("warden_opened_at"))));
            details.append("\nWarden: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("warden_name"))));
            details.append("\nWarden response: ").append(valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("warden_reply"))));
            details.append("\nWarden response time: ").append(formatTimestamp(
                    cursor.getString(cursor.getColumnIndexOrThrow("warden_replied_at"))));
            details.append("\nStatus: ").append(complaintStatus(cursor));
        }
        return details.toString();
    }

    private TextView makeRow(String text) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(Color.BLACK);
        row.setTextSize(16);
        row.setPadding(14, 16, 14, 16);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 10;
        row.setLayoutParams(params);
        return row;
    }

    private void addMessage(LinearLayout list, String message) {
        TextView row = makeRow(message);
        row.setBackgroundColor(Color.WHITE);
        list.addView(row);
    }

    private String currentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    // Displays the remaining retention period for resolved entries on the faculty list.
    private String remainingTime(long resolvedAt) {
        long remaining = Math.max(0, resolvedAt + 48L * 60L * 60L * 1000L - System.currentTimeMillis());
        long days = remaining / (24L * 60L * 60L * 1000L);
        long hours = (remaining / (60L * 60L * 1000L)) % 24;
        long minutes = (remaining / (60L * 1000L)) % 60;
        return days + " day " + hours + " hr " + minutes + " min";
    }

    private String formatDate(long time) {
        if (time <= 0) return "Not available";
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(time));
    }

    // Shows the simple workflow status: Pending, In Progress, or Resolved.
    private String complaintStatus(Cursor cursor) {
        if (cursor.getInt(cursor.getColumnIndexOrThrow("is_resolved")) == 1) return "Resolved";
        String openedAt = cursor.getString(cursor.getColumnIndexOrThrow("warden_opened_at"));
        return openedAt == null || openedAt.trim().isEmpty() ? "Pending" : "In Progress";
    }

    private int statusColor(String status) {
        return status.equals("Resolved") ? Color.rgb(0, 128, 0) : Color.RED;
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) return "Not available";
        try {
            return formatDate(Long.parseLong(timestamp));
        } catch (NumberFormatException e) {
            return timestamp;
        }
    }

    // Clears the warden session name and returns to the home screen.
    private void logoutWarden() {
        currentWardenName = "";
        ((EditText) findViewById(R.id.wardenNameInput)).setText("");
        ((EditText) findViewById(R.id.wardenPasswordInput)).setText("");
        showSection(homeSection);
    }

    // Returns faculty to the home screen; the faculty screen has no editing actions.
    private void logoutFaculty() {
        ((EditText) findViewById(R.id.facultyNameInput)).setText("");
        ((EditText) findViewById(R.id.facultyPasswordInput)).setText("");
        showSection(homeSection);
    }

    // Posts a local device notification after the complaint is marked resolved.
    private void notifyStudentComplaintResolved(long complaintId) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        Cursor cursor = database.getComplaint(complaintId);
        String category = "Complaint";
        String student = "Student";
        try {
            if (cursor.moveToFirst()) {
                category = valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("category")));
                student = valueOrNone(cursor.getString(cursor.getColumnIndexOrThrow("student_name")));
            }
        } finally {
            cursor.close();
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        android.app.Notification notification;
        if (Build.VERSION.SDK_INT >= 26) {
            notification = new android.app.Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Complaint resolved")
                    .setContentText(student + ", your " + category + " complaint was resolved.")
                    .setAutoCancel(true)
                    .build();
        } else {
            notification = new android.app.Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Complaint resolved")
                    .setContentText(student + ", your " + category + " complaint was resolved.")
                    .setAutoCancel(true)
                    .build();
        }
        manager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Complaint updates", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Updates about complaint resolution");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    // Schedules cleanup near the 48-hour mark, even if the app is no longer open.
    private void scheduleComplaintCleanup(long complaintId, long resolvedAt) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, ComplaintCleanupReceiver.class);
        intent.setData(Uri.parse("svkmcomplaint://cleanup/" + complaintId));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) complaintId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                resolvedAt + 48L * 60L * 60L * 1000L, pendingIntent);
    }

    private String valueOrNone(String value) {
        return value == null || value.trim().isEmpty() ? "Not available" : value;
    }

    private void showSection(LinearLayout sectionToShow) {
        LinearLayout[] sections = {homeSection, studentSection, categorySection, descriptionSection,
                studentStatusSection, wardenLoginSection, wardenDashboardSection,
                wardenLogSection, wardenDetailSection, facultyLoginSection, facultySection,
                facultyDetailSection};
        for (LinearLayout section : sections) section.setVisibility(View.GONE);
        sectionToShow.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    private void goBack() {
        if (descriptionSection.getVisibility() == View.VISIBLE) showSection(categorySection);
        else if (categorySection.getVisibility() == View.VISIBLE) showSection(studentSection);
        else if (studentSection.getVisibility() == View.VISIBLE || studentStatusSection.getVisibility() == View.VISIBLE
                || wardenLoginSection.getVisibility() == View.VISIBLE || facultyLoginSection.getVisibility() == View.VISIBLE
                || facultySection.getVisibility() == View.VISIBLE) {
            showSection(homeSection);
        } else if (wardenDetailSection.getVisibility() == View.VISIBLE) {
            loadWardenComplaints();
            showSection(wardenDashboardSection);
        } else if (wardenLogSection.getVisibility() == View.VISIBLE) {
            showSection(wardenDashboardSection);
        } else if (wardenDashboardSection.getVisibility() == View.VISIBLE) {
            showSection(wardenLoginSection);
        } else if (facultyDetailSection.getVisibility() == View.VISIBLE) {
            loadFacultyComplaints();
            showSection(facultySection);
        } else super.onBackPressed();
    }

    // Alarm callback deletes only complaints whose resolved time is already 48 hours old.
    public static class ComplaintCleanupReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Database1 database = new Database1(context);
            database.deleteExpiredResolvedComplaints(System.currentTimeMillis());
            database.close();
        }
    }
}
