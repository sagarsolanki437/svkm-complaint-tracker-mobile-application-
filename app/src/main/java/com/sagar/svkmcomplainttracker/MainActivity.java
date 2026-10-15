package com.sagar.svkmcomplainttracker;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Database1 database;
    private LinearLayout[] sections;
    private String selectedCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        database = new Database1(this);

        sections = new LinearLayout[]{findViewById(R.id.homeSection),
                findViewById(R.id.studentSection), findViewById(R.id.categorySection),
                findViewById(R.id.descriptionSection), findViewById(R.id.statusSection)};

        setupSpinner(R.id.branchSpinner, R.array.student_branches);
        setupSpinner(R.id.yearSpinner, R.array.student_years);

        findViewById(R.id.studentOption).setOnClickListener(v -> showSection(1));
        findViewById(R.id.checkStatusButton).setOnClickListener(v -> showSection(4));
        findViewById(R.id.studentNextButton).setOnClickListener(v -> validateStudentDetails());
        findViewById(R.id.backToStudentButton).setOnClickListener(v -> showSection(1));
        findViewById(R.id.backToCategoriesButton).setOnClickListener(v -> showSection(2));
        findViewById(R.id.backToHomeButton).setOnClickListener(v -> showSection(0));
        findViewById(R.id.backToHomeFromStatusButton).setOnClickListener(v -> showSection(0));
        findViewById(R.id.statusSearchButton).setOnClickListener(v -> loadStudentComplaints());

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
                showSection(3);
            });
        }

        findViewById(R.id.postButton).setOnClickListener(v -> saveStudentComplaint());
    }

    private void setupSpinner(int viewId, int arrayId) {
        Spinner spinner = findViewById(viewId);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, arrayId,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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
        if (valid) showSection(2);
    }

    private void saveStudentComplaint() {
        EditText description = findViewById(R.id.complaintDescriptionInput);
        String details = description.getText().toString().trim();
        if (details.isEmpty()) {
            description.setError("Enter complaint details");
            return;
        }

        long id = database.saveComplaint(
                ((EditText) findViewById(R.id.studentNameInput)).getText().toString().trim(),
                ((EditText) findViewById(R.id.sapIdInput)).getText().toString().trim(),
                ((Spinner) findViewById(R.id.branchSpinner)).getSelectedItem().toString(),
                ((Spinner) findViewById(R.id.yearSpinner)).getSelectedItem().toString(),
                ((EditText) findViewById(R.id.phoneInput)).getText().toString().trim(),
                selectedCategory, details, System.currentTimeMillis());

        if (id == -1) {
            Toast.makeText(this, "Could not save the complaint. Please try again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Complaint saved. Reference ID: " + id, Toast.LENGTH_LONG).show();
        description.setText("");
        selectedCategory = "";
        showSection(0);
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
                addStatusMessage(list, "No complaints found for this SAP ID.");
                return;
            }
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                addStatusMessage(list, "Reference #" + id + "  •  " + category + "\n" +
                        "Submitted: " + DateFormat.getDateTimeInstance().format(new Date(createdAt)) +
                        "\nStatus: " + status + "\n" + description);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
    }

    private void addStatusMessage(LinearLayout list, String message) {
        TextView row = new TextView(this);
        row.setText(message);
        row.setTextSize(16);
        row.setTextColor(0xFF222222);
        row.setPadding(14, 14, 14, 14);
        list.addView(row);
    }

    private void showSection(int selectedIndex) {
        for (int i = 0; i < sections.length; i++) {
            sections[i].setVisibility(i == selectedIndex ? View.VISIBLE : View.GONE);
        }
    }
}
