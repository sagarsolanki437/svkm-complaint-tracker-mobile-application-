package com.sagar.svkmcomplainttracker;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String NOTIFICATION_CHANNEL_ID = "svkm_complaint_updates";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 10;
    private Database1 database;
    private LinearLayout homeSection;
    private LinearLayout studentSection;
    private LinearLayout categorySection;
    private LinearLayout descriptionSection;
    private LinearLayout statusSection;
    private LinearLayout wardenLoginSection;
    private LinearLayout wardenDashboardSection;
    private LinearLayout wardenDetailSection;
    private LinearLayout facultyLoginSection;
    private LinearLayout facultySection;
    private LinearLayout facultyDetailSection;
    private String selectedCategory = "";
    private String currentWardenName = "";
    private long currentComplaintId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        requestNotificationPermission();
        database = new Database1(this);
        homeSection = findViewById(R.id.homeSection);
        studentSection = findViewById(R.id.studentSection);
        categorySection = findViewById(R.id.categorySection);
        descriptionSection = findViewById(R.id.descriptionSection);
        statusSection = findViewById(R.id.statusSection);
        wardenLoginSection = findViewById(R.id.wardenLoginSection);
        wardenDashboardSection = findViewById(R.id.wardenDashboardSection);
        wardenDetailSection = findViewById(R.id.wardenDetailSection);
        facultyLoginSection = findViewById(R.id.facultyLoginSection);
        facultySection = findViewById(R.id.facultySection);
        facultyDetailSection = findViewById(R.id.facultyDetailSection);

        Spinner branch = findViewById(R.id.branchSpinner);
        ArrayAdapter<CharSequence> branchAdapter = ArrayAdapter.createFromResource(this,
                R.array.student_branches, android.R.layout.simple_spinner_item);
        branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        branch.setAdapter(branchAdapter);

        Spinner year = findViewById(R.id.yearSpinner);
        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(this,
                R.array.student_years, android.R.layout.simple_spinner_item);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        year.setAdapter(yearAdapter);

        findViewById(R.id.studentOption).setOnClickListener(v -> showSection(studentSection));
        findViewById(R.id.checkStatusButton).setOnClickListener(v -> showSection(statusSection));
        findViewById(R.id.wardenOption).setOnClickListener(v -> showSection(wardenLoginSection));
        findViewById(R.id.facultyOption).setOnClickListener(v -> showSection(facultyLoginSection));
        findViewById(R.id.studentNextButton).setOnClickListener(v -> validateStudentDetails());
        findViewById(R.id.backToStudentButton).setOnClickListener(v -> showSection(studentSection));
        findViewById(R.id.backToCategoriesButton).setOnClickListener(v -> showSection(categorySection));
        findViewById(R.id.backToHomeButton).setOnClickListener(v -> showSection(homeSection));
        findViewById(R.id.backToHomeFromStatusButton).setOnClickListener(v -> showSection(homeSection));
        findViewById(R.id.backToHomeFromWardenButton).setOnClickListener(v -> showSection(homeSection));
        findViewById(R.id.wardenEnterButton).setOnClickListener(v -> wardenLogin());
        findViewById(R.id.wardenLogoutButton).setOnClickListener(v -> showSection(homeSection));
        findViewById(R.id.statusSearchButton).setOnClickListener(v -> loadStudentComplaints());
        findViewById(R.id.wardenBackButton).setOnClickListener(v -> showSection(wardenDashboardSection));
        findViewById(R.id.resolveComplaintButton).setOnClickListener(v -> resolveCurrentComplaint());

        int[] categoryButtons = {R.id.hostelOption, R.id.messOption, R.id.theftOption,
                R.id.staffBehaviorOption, R.id.academicsOption};
        String[] categories = {"Hostel", "Mess", "Theft Complaint", "Staff Behavior",
                "Academics Problem"};
        TextView title = findViewById(R.id.complaintTitle);
        for (int i = 0; i < categoryButtons.length; i++) {
            final String category = categories[i];
            findViewById(categoryButtons[i]).setOnClickListener(v -> {
                selectedCategory = category;
                title.setText(category);
                showSection(descriptionSection);
            });
        }

        findViewById(R.id.postButton).setOnClickListener(v -> saveStudentComplaint());
        findViewById(R.id.wardenRefreshButton).setOnClickListener(v -> loadWardenComplaints());
        findViewById(R.id.facultyEnterButton).setOnClickListener(v -> facultyLogin());
        findViewById(R.id.facultySearchButton).setOnClickListener(v -> loadFacultyComplaints());
        findViewById(R.id.facultyLogoutButton).setOnClickListener(v -> showSection(homeSection));
        findViewById(R.id.facultyBackButton).setOnClickListener(v -> showSection(facultySection));
    }

    private void validateStudentDetails() {
        EditText name = findViewById(R.id.studentNameInput);
        EditText sap = findViewById(R.id.sapIdInput);
        EditText phone = findViewById(R.id.phoneInput);
        Spinner branch = findViewById(R.id.branchSpinner);
        Spinner year = findViewById(R.id.yearSpinner);
        boolean valid = true;

        if (name.getText().toString().trim().isEmpty()) {
            name.setError("Enter your name");
            valid = false;
        }
        if (sap.getText().toString().trim().isEmpty()) {
            sap.setError("Enter your SAP ID");
            valid = false;
        }
        if (!phone.getText().toString().trim().matches("[0-9]{10}")) {
            phone.setError("Enter a 10 digit phone number");
            valid = false;
        }
        if (branch.getSelectedItemPosition() == 0 || year.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select your branch and year", Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (valid) showSection(categorySection);
    }

    private void saveStudentComplaint() {
        EditText description = findViewById(R.id.complaintDescriptionInput);
        String complaintText = description.getText().toString().trim();
        if (complaintText.isEmpty()) {
            description.setError("Enter complaint details");
            return;
        }

        long id = database.saveComplaint(
                ((EditText) findViewById(R.id.studentNameInput)).getText().toString().trim(),
                ((EditText) findViewById(R.id.sapIdInput)).getText().toString().trim(),
                ((Spinner) findViewById(R.id.branchSpinner)).getSelectedItem().toString(),
                ((Spinner) findViewById(R.id.yearSpinner)).getSelectedItem().toString(),
                ((EditText) findViewById(R.id.phoneInput)).getText().toString().trim(),
                selectedCategory, complaintText, System.currentTimeMillis());
        if (id == -1) {
            Toast.makeText(this, "Could not save complaint", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Complaint saved. Reference ID: " + id, Toast.LENGTH_LONG).show();
        description.setText("");
        selectedCategory = "";
        showSection(homeSection);
    }

    private void loadStudentComplaints() {
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
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String reply = cursor.getString(cursor.getColumnIndexOrThrow("warden_reply"));
                addMessage(list, "Reference #" + id + " • " + category + "\n" +
                        "Submitted: " + DateFormat.getDateTimeInstance().format(new Date(createdAt)) +
                        "\nStatus: " + status +
                        (reply == null || reply.isEmpty() ? "" : "\nWarden response: " + reply) +
                        "\n" + description);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    private void wardenLogin() {
        EditText name = findViewById(R.id.wardenNameInput);
        EditText password = findViewById(R.id.wardenPasswordInput);
        currentWardenName = name.getText().toString().trim();
        if (currentWardenName.isEmpty()) {
            name.setError("Enter your name");
            return;
        }
        if (!password.getText().toString().equals("warden1234")) {
            password.setError("Incorrect password");
            return;
        }
        password.setText("");
        database.recordWardenLogin(currentWardenName, System.currentTimeMillis());
        loadWardenComplaints();
        showSection(wardenDashboardSection);
    }

    private void loadWardenComplaints() {
        LinearLayout list = findViewById(R.id.wardenComplaintList);
        list.removeAllViews();
        Cursor cursor = database.getUnresolvedComplaints();
        try {
            if (!cursor.moveToFirst()) {
                addMessage(list, "There are no pending complaints.");
                return;
            }
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String studentName = cursor.getString(cursor.getColumnIndexOrThrow("student_name"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                Button row = new Button(this);
                row.setText(studentName + " • " + category);
                row.setOnClickListener(v -> openWardenComplaint(id));
                list.addView(row);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    private void openWardenComplaint(long complaintId) {
        currentComplaintId = complaintId;
        Cursor cursor = database.getComplaint(complaintId);
        try {
            if (!cursor.moveToFirst()) return;
            String details = "Reference #" + complaintId + "\n" +
                    "Student: " + cursor.getString(cursor.getColumnIndexOrThrow("student_name")) + "\n" +
                    "SAP ID: " + cursor.getString(cursor.getColumnIndexOrThrow("sap_id")) + "\n" +
                    "Branch: " + cursor.getString(cursor.getColumnIndexOrThrow("branch")) + "\n" +
                    "Year: " + cursor.getString(cursor.getColumnIndexOrThrow("year")) + "\n" +
                    "Phone: " + cursor.getString(cursor.getColumnIndexOrThrow("phone")) + "\n" +
                    "Category: " + cursor.getString(cursor.getColumnIndexOrThrow("category")) + "\n\n" +
                    cursor.getString(cursor.getColumnIndexOrThrow("description"));
            ((TextView) findViewById(R.id.wardenComplaintDetails)).setText(details);
            ((EditText) findViewById(R.id.wardenReplyInput)).setText("");
            showSection(wardenDetailSection);
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
        if (database.resolveComplaint(currentComplaintId, currentWardenName, reply,
                System.currentTimeMillis())) {
            notifyStudentComplaintResolved(currentComplaintId);
            Toast.makeText(this, "Complaint resolved", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "This complaint is no longer pending", Toast.LENGTH_SHORT).show();
        }
        loadWardenComplaints();
        showSection(wardenDashboardSection);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Complaint updates", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Updates when a complaint is resolved");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void notifyStudentComplaintResolved(long complaintId) {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) return;

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) complaintId,
                openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this,
                NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Complaint resolved")
                .setContentText("A response is available for complaint #" + complaintId + ".")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(this).notify((int) (complaintId & 0x7fffffff),
                notification.build());
    }

    private void addMessage(LinearLayout list, String message) {
        TextView row = new TextView(this);
        row.setText(message);
        row.setTextSize(16);
        row.setTextColor(0xFF222222);
        row.setPadding(14, 14, 14, 14);
        list.addView(row);
    }

    private void facultyLogin() {
        EditText name = findViewById(R.id.facultyNameInput);
        EditText password = findViewById(R.id.facultyPasswordInput);
        if (name.getText().toString().trim().isEmpty()) {
            name.setError("Enter your name");
            return;
        }
        if (!password.getText().toString().equals("faculty1234")) {
            password.setError("Incorrect password");
            return;
        }
        password.setText("");
        ((EditText) findViewById(R.id.facultySearchInput)).setText("");
        loadFacultyComplaints();
        showSection(facultySection);
    }

    private void loadFacultyComplaints() {
        String searchText = ((EditText) findViewById(R.id.facultySearchInput))
                .getText().toString().trim();
        LinearLayout list = findViewById(R.id.facultyComplaintList);
        list.removeAllViews();

        long latestLogin = database.getLatestWardenLoginTime();
        String loginLabel = latestLogin == 0 ? "Not recorded" :
                DateFormat.getDateTimeInstance().format(new Date(latestLogin));
        ((TextView) findViewById(R.id.facultyLatestLogin))
                .setText("Latest warden login: " + loginLabel);

        Cursor cursor = searchText.isEmpty()
                ? database.getAllComplaints() : database.searchAllComplaints(searchText);
        try {
            if (!cursor.moveToFirst()) {
                addMessage(list, searchText.isEmpty()
                        ? "No complaints have been submitted."
                        : "No complaints match your search.");
                return;
            }
            do {
                long complaintId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String studentName = cursor.getString(cursor.getColumnIndexOrThrow("student_name"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                Button row = new Button(this);
                row.setText(studentName + " • " + category + " • " + status);
                row.setOnClickListener(v -> openFacultyComplaint(complaintId));
                list.addView(row);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    private void openFacultyComplaint(long complaintId) {
        Cursor cursor = database.getComplaint(complaintId);
        try {
            if (!cursor.moveToFirst()) return;
            String wardenName = cursor.getString(cursor.getColumnIndexOrThrow("warden_name"));
            String reply = cursor.getString(cursor.getColumnIndexOrThrow("warden_reply"));
            String details = "Reference #" + complaintId + "\n" +
                    "Student: " + cursor.getString(cursor.getColumnIndexOrThrow("student_name")) + "\n" +
                    "SAP ID: " + cursor.getString(cursor.getColumnIndexOrThrow("sap_id")) + "\n" +
                    "Branch: " + cursor.getString(cursor.getColumnIndexOrThrow("branch")) + "\n" +
                    "Year: " + cursor.getString(cursor.getColumnIndexOrThrow("year")) + "\n" +
                    "Phone: " + cursor.getString(cursor.getColumnIndexOrThrow("phone")) + "\n" +
                    "Category: " + cursor.getString(cursor.getColumnIndexOrThrow("category")) + "\n" +
                    "Status: " + cursor.getString(cursor.getColumnIndexOrThrow("status")) + "\n" +
                    "Warden: " + (wardenName == null ? "Not assigned" : wardenName) + "\n" +
                    "Warden response: " + (reply == null || reply.isEmpty() ? "None" : reply) +
                    "\n\n" + cursor.getString(cursor.getColumnIndexOrThrow("description"));
            ((TextView) findViewById(R.id.facultyComplaintDetails)).setText(details);
            showSection(facultyDetailSection);
        } finally {
            cursor.close();
        }
    }

    private void showSection(LinearLayout selected) {
        homeSection.setVisibility(View.GONE);
        studentSection.setVisibility(View.GONE);
        categorySection.setVisibility(View.GONE);
        descriptionSection.setVisibility(View.GONE);
        statusSection.setVisibility(View.GONE);
        wardenLoginSection.setVisibility(View.GONE);
        wardenDashboardSection.setVisibility(View.GONE);
        wardenDetailSection.setVisibility(View.GONE);
        facultyLoginSection.setVisibility(View.GONE);
        facultySection.setVisibility(View.GONE);
        facultyDetailSection.setVisibility(View.GONE);
        selected.setVisibility(View.VISIBLE);
    }
}
