package com.sagar.svkmcomplainttracker;

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

public class MainActivity extends AppCompatActivity {
    private LinearLayout homeSection;
    private LinearLayout studentSection;
    private LinearLayout categorySection;
    private LinearLayout descriptionSection;
    private String selectedCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeSection = findViewById(R.id.homeSection);
        studentSection = findViewById(R.id.studentSection);
        categorySection = findViewById(R.id.categorySection);
        descriptionSection = findViewById(R.id.descriptionSection);

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
        findViewById(R.id.studentNextButton).setOnClickListener(v -> validateStudentDetails());
        findViewById(R.id.backToStudentButton).setOnClickListener(v -> showSection(studentSection));
        findViewById(R.id.backToCategoriesButton).setOnClickListener(v -> showSection(categorySection));
        findViewById(R.id.backToHomeButton).setOnClickListener(v -> showSection(homeSection));

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

        findViewById(R.id.postButton).setOnClickListener(v -> showSubmissionPreview());
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

    private void showSubmissionPreview() {
        EditText description = findViewById(R.id.complaintDescriptionInput);
        if (description.getText().toString().trim().isEmpty()) {
            description.setError("Enter complaint details");
            return;
        }
        Toast.makeText(this, "Complaint form ready. Saving will be added in a later step.",
                Toast.LENGTH_LONG).show();
    }

    private void showSection(LinearLayout selected) {
        homeSection.setVisibility(View.GONE);
        studentSection.setVisibility(View.GONE);
        categorySection.setVisibility(View.GONE);
        descriptionSection.setVisibility(View.GONE);
        selected.setVisibility(View.VISIBLE);
    }
}
