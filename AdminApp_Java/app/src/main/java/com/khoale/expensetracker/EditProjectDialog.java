package com.khoale.expensetracker;

import androidx.appcompat.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditProjectDialog {

    public interface OnProjectUpdated {
        void onUpdated(Project project);
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    private final Context context;
    private final Project project;
    private final OnProjectUpdated callback;
    private final OnDismissListener dismissListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public EditProjectDialog(Context context, Project project, OnProjectUpdated callback, OnDismissListener dismissListener) {
        this.context = context;
        this.project = project;
        this.callback = callback;
        this.dismissListener = dismissListener;
    }

    public void show() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        layout.addView(createLabel("Project Name:"));
        EditText etName = new EditText(context);
        etName.setText(project.getName());
        layout.addView(etName);

        layout.addView(createLabel("Manager:"));
        EditText etManager = new EditText(context);
        etManager.setText(project.getManager());
        layout.addView(etManager);

        layout.addView(createLabel("Description:"));
        EditText etDescription = new EditText(context);
        etDescription.setText(project.getDescription());
        layout.addView(etDescription);

        layout.addView(createLabel("Budget ($):"));
        EditText etBudget = new EditText(context);
        etBudget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etBudget.setText(String.valueOf(project.getBudget()));
        layout.addView(etBudget);

        layout.addView(createLabel("Start Date:"));
        EditText etStart = new EditText(context);
        etStart.setText(project.getStartDate());
        etStart.setFocusable(false);
        etStart.setOnClickListener(v -> showDatePicker(etStart));
        layout.addView(etStart);

        layout.addView(createLabel("End Date:"));
        EditText etEnd = new EditText(context);
        etEnd.setText(project.getEndDate());
        etEnd.setFocusable(false);
        etEnd.setOnClickListener(v -> showDatePicker(etEnd));
        layout.addView(etEnd);

        layout.addView(createLabel("Status:"));
        Spinner spStatus = new Spinner(context);
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(context,
                R.array.status_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);
        
        if (project.getStatus() != null) {
            int pos = statusAdapter.getPosition(project.getStatus());
            if (pos >= 0) spStatus.setSelection(pos);
        }
        layout.addView(spStatus);

        layout.addView(createLabel("Risk Assessment:"));
        Spinner spRisk = new Spinner(context);
        ArrayAdapter<CharSequence> riskAdapter = ArrayAdapter.createFromResource(context,
                R.array.risk_options, android.R.layout.simple_spinner_item);
        riskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRisk.setAdapter(riskAdapter);
        if (project.getRiskAssessment() != null) {
            int pos = riskAdapter.getPosition(project.getRiskAssessment());
            if (pos >= 0) spRisk.setSelection(pos);
        }
        layout.addView(spRisk);

        layout.addView(createLabel("Special Requirements:"));
        EditText etSpecialRequirements = new EditText(context);
        etSpecialRequirements.setText(project.getSpecialRequirements());
        layout.addView(etSpecialRequirements);

        layout.addView(createLabel("Client/Department:"));
        EditText etClientInfo = new EditText(context);
        etClientInfo.setText(project.getClientInfo());
        layout.addView(etClientInfo);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Edit Project Details")
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, w) -> {
                    // Dialog will dismiss and trigger onDismissListener
                })
                .setOnDismissListener(d -> {
                    if (dismissListener != null) dismissListener.onDismiss();
                })
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String budgetStr = etBudget.getText().toString().trim();
            String startStr = etStart.getText().toString().trim();
            String endStr = etEnd.getText().toString().trim();

            if (name.isEmpty() || budgetStr.isEmpty() || startStr.isEmpty()) {
                Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!endStr.isEmpty()) {
                try {
                    Date start = dateFormat.parse(startStr);
                    Date end = dateFormat.parse(endStr);
                    if (start != null && end != null && end.before(start)) {
                        Toast.makeText(context, "End date must be after start date", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (ParseException e) {
                    return;
                }
            }

            try {
                project.setName(name);
                project.setDescription(etDescription.getText().toString().trim());
                project.setManager(etManager.getText().toString().trim());
                project.setBudget(Double.parseDouble(budgetStr));
                project.setStartDate(startStr);
                project.setEndDate(endStr);
                project.setStatus(spStatus.getSelectedItem().toString());
                project.setRiskAssessment(spRisk.getSelectedItem().toString());
                project.setSpecialRequirements(etSpecialRequirements.getText().toString().trim());
                project.setClientInfo(etClientInfo.getText().toString().trim());
                callback.onUpdated(project);
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(context, "Invalid data", Toast.LENGTH_SHORT).show();
            }
        });
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

        new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, (month + 1), year);
            target.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setPadding(0, 10, 0, 0);
        tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }
}
