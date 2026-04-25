package com.mk183.exercise3;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseActionListener {

    private final ArrayList<Expense> expenses = new ArrayList<>();
    private ExpenseRepository repository;
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new ExpenseRepository(this);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewExpenses);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddExpense);

        adapter = new ExpenseAdapter(expenses, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showExpenseDialog(null));
        loadExpenses();
    }

    private void loadExpenses() {
        expenses.clear();
        expenses.addAll(repository.getAllExpenses());
        adapter.notifyDataSetChanged();
    }

    private void showExpenseDialog(Expense expenseToEdit) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_expense, null);
        EditText etName = view.findViewById(R.id.etExpenseName);
        EditText etAmount = view.findViewById(R.id.etExpenseAmount);
        EditText etDate = view.findViewById(R.id.etExpenseDate);
        Spinner spCategory = view.findViewById(R.id.spCategory);

        String[] categories = {"Select Category", "Food", "Transportation", "Utilities", "Shopping", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        etDate.setOnClickListener(v -> showDatePicker(etDate));

        if (expenseToEdit != null) {
            etName.setText(expenseToEdit.getName());
            etAmount.setText(String.valueOf(expenseToEdit.getAmount()));
            etDate.setText(expenseToEdit.getDate());
            int index = categoryAdapter.getPosition(expenseToEdit.getCategory());
            if (index >= 0) {
                spCategory.setSelection(index);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(expenseToEdit == null ? "Add Expense" : "Edit Expense")
                .setView(view)
                .setPositiveButton(expenseToEdit == null ? "Save" : "Update", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String amount = etAmount.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    String category = spCategory.getSelectedItem().toString();

                    if (!validateExpenseInput(name, amount, date, category, etName, etAmount, etDate)) {
                        return;
                    }

                    Expense expense = new Expense(name, Double.parseDouble(amount), date, category);
                    boolean success;
                    if (expenseToEdit == null) {
                        success = repository.insertExpense(expense);
                    } else {
                        success = repository.updateExpense(expenseToEdit.getId(), expense);
                    }

                    if (success) {
                        loadExpenses();
                    } else {
                        Toast.makeText(this, "Database operation failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean validateExpenseInput(String name, String amount, String date, String category,
                                         EditText etName, EditText etAmount, EditText etDate) {
        boolean valid = true;

        if (name.isEmpty()) {
            etName.setError("Name is required");
            valid = false;
        }
        if (amount.isEmpty()) {
            etAmount.setError("Amount is required");
            valid = false;
        } else {
            try {
                Double.parseDouble(amount);
            } catch (NumberFormatException ex) {
                etAmount.setError("Invalid amount");
                valid = false;
            }
        }
        if (date.isEmpty()) {
            etDate.setError("Date is required");
            valid = false;
        }
        if ("Select Category".equals(category)) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        return valid;
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                target.setText(String.format(Locale.getDefault(), "%02d/%02d/%d",
                        dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void onEdit(Expense expense) {
        showExpenseDialog(expense);
    }

    @Override
    public void onDelete(Expense expense) {
        repository.deleteExpense(expense.getId());
        loadExpenses();
    }
}
