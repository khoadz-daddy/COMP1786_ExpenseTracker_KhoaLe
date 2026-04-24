package com.khoale.expensetracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity - Project entry and Cloud synchronization
 */
public class MainActivity extends AppCompatActivity {

    private EditText etCode, etName, etDesc, etManager, etStartDate, etEndDate, etBudget, etSpecialRequirements, etClientInfo;
    private Spinner spRisk, spStatus;
    private Button btnSave, btnViewList, btnSync, btnResetDb;
    private ProjectRepository repository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new ProjectRepository(this);
        initViews();
        setupDatePickers();

        btnSave.setOnClickListener(v -> handleSave());
        btnViewList.setOnClickListener(v -> startActivity(new Intent(this, ProjectListActivity.class)));
        btnSync.setOnClickListener(v -> syncToFirebase());
        btnResetDb.setOnClickListener(v -> showResetDatabaseConfirm());
    }

    private void initViews() {
        etCode = findViewById(R.id.etProjectCode);
        etName = findViewById(R.id.etProjectName);
        etDesc = findViewById(R.id.etDescription);
        etManager = findViewById(R.id.etManager);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etBudget = findViewById(R.id.etBudget);
        etSpecialRequirements = findViewById(R.id.etSpecialRequirements);
        etClientInfo = findViewById(R.id.etClientInfo);
        spStatus = findViewById(R.id.spStatus);
        spRisk = findViewById(R.id.spRiskAssessment);
        
        btnSave = findViewById(R.id.btnSave);
        btnViewList = findViewById(R.id.btnViewList);
        btnSync = findViewById(R.id.btnSync);
        btnResetDb = findViewById(R.id.btnResetDb);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        
        String currentText = target.getText().toString();
        if (!currentText.isEmpty()) {
            try {
                Date d = dateFormat.parse(currentText);
                if (d != null) c.setTime(d);
            } catch (ParseException ignored) {}
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
            target.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void handleSave() {
        String code = etCode.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String manager = etManager.getText().toString().trim();
        String budgetStr = etBudget.getText().toString().trim();
        String startDateStr = etStartDate.getText().toString().trim();
        String endDateStr = etEndDate.getText().toString().trim();
        String risk = spRisk.getSelectedItem().toString();
        String status = spStatus.getSelectedItem().toString();
        String specialRequirements = etSpecialRequirements.getText().toString().trim();
        String clientInfo = etClientInfo.getText().toString().trim();

        // 1. Validation: ALL Required fields check (Specification Page 3)
        boolean hasError = false;
        if (code.isEmpty()) { etCode.setError("Project ID/Code is required"); hasError = true; }
        if (name.isEmpty()) { etName.setError("Project Name is required"); hasError = true; }
        if (desc.isEmpty()) { etDesc.setError("Project Description is required"); hasError = true; }
        if (manager.isEmpty()) { etManager.setError("Project Manager/Owner is required"); hasError = true; }
        if (startDateStr.isEmpty()) { etStartDate.setError("Start Date is required"); hasError = true; }
        if (endDateStr.isEmpty()) { etEndDate.setError("End Date is required"); hasError = true; }
        if (budgetStr.isEmpty()) { etBudget.setError("Project Budget is required"); hasError = true; }

        if (hasError) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Date logic check
        try {
            Date start = dateFormat.parse(startDateStr);
            Date end = dateFormat.parse(endDateStr);
            if (start != null && end != null && end.before(start)) {
                etEndDate.setError("End date must be after start date");
                return;
            }
        } catch (ParseException e) {
            etEndDate.setError("Invalid date format");
            return;
        }

        // 3. Number format check
        double budget;
        try {
            budget = Double.parseDouble(budgetStr);
        } catch (NumberFormatException e) {
            etBudget.setError("Invalid budget amount");
            return;
        }

        Project project = new Project();
        project.setProjectCode(code);
        project.setName(name);
        project.setDescription(desc);
        project.setManager(manager);
        project.setBudget(budget);
        project.setStartDate(startDateStr);
        project.setEndDate(endDateStr);
        project.setRiskAssessment(risk);
        project.setStatus(status);
        project.setSpecialRequirements(specialRequirements);
        project.setClientInfo(clientInfo);

        // Confirmation Dialog with FULL DETAILS (Specification Page 3)
        String details = "Code: " + code + "\n" +
                         "Name: " + name + "\n" +
                         "Manager: " + manager + "\n" +
                         "Budget: $" + budgetStr + "\n" +
                         "Status: " + status + "\n" +
                         "Timeline: " + startDateStr + " - " + endDateStr + "\n" +
                         "Special Requirements: " + (specialRequirements.isEmpty() ? "N/A" : specialRequirements) + "\n" +
                         "Client/Department: " + (clientInfo.isEmpty() ? "N/A" : clientInfo);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Project Details")
                .setMessage(details + "\n\nDo you want to save?")
                .setPositiveButton(R.string.yes_save, (d, w) -> {
                    if (repository.insertProject(project)) {
                        Toast.makeText(this, R.string.project_saved_successfully, Toast.LENGTH_SHORT).show();
                        clearForm();
                    }
                })
                .setNegativeButton("Edit Info", null)
                .show();
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager 
            = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void syncToFirebase() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Projects");
            List<Project> allData = repository.getAllProjectsWithExpenses();

            if (allData.isEmpty()) {
                Toast.makeText(this, R.string.no_data_sync, Toast.LENGTH_SHORT).show();
                return;
            }

            btnSync.setEnabled(false);
            btnSync.setText(R.string.syncing);

            mDatabase.setValue(allData).addOnSuccessListener(aVoid -> {
                btnSync.setEnabled(true);
                btnSync.setText(R.string.cloud_sync);
                Toast.makeText(MainActivity.this, R.string.sync_success, Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                btnSync.setEnabled(true);
                btnSync.setText(R.string.cloud_sync);
                String msg = getString(R.string.sync_failed, e.getMessage());
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearForm() {
        etCode.setText("");
        etName.setText("");
        etDesc.setText("");
        etManager.setText("");
        etStartDate.setText("");
        etEndDate.setText("");
        etBudget.setText("");
        etSpecialRequirements.setText("");
        etClientInfo.setText("");
        spStatus.setSelection(0);
        spRisk.setSelection(0);
    }

    private void showResetDatabaseConfirm() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_database)
                .setMessage(R.string.reset_database_message)
                .setPositiveButton(R.string.reset_database, (dialog, which) -> {
                    repository.resetDatabase();
                    Toast.makeText(this, R.string.database_reset_success, Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
