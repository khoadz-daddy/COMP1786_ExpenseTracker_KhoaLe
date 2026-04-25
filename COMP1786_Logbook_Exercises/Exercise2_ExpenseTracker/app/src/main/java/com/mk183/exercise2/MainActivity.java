package com.mk183.exercise2;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseActionListener {

    private final ArrayList<Expense> expenseList = new ArrayList<>();
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewExpenses);
        Button btnAddExpense = findViewById(R.id.btnAddExpense);

        adapter = new ExpenseAdapter(expenseList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnAddExpense.setOnClickListener(v -> showExpenseDialog(-1));
    }

    private void showExpenseDialog(int position) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_expense, null);
        EditText etName = view.findViewById(R.id.etExpenseName);
        EditText etAmount = view.findViewById(R.id.etExpenseAmount);
        EditText etDate = view.findViewById(R.id.etExpenseDate);

        etDate.setOnClickListener(v -> showDatePicker(etDate));

        if (position >= 0) {
            Expense expense = expenseList.get(position);
            etName.setText(expense.getName());
            etAmount.setText(String.valueOf(expense.getAmount()));
            etDate.setText(expense.getDate());
        }

        new AlertDialog.Builder(this)
                .setTitle(position >= 0 ? "Edit Expense" : "Add Expense")
                .setView(view)
                .setPositiveButton(position >= 0 ? "Update" : "Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String amountText = etAmount.getText().toString().trim();
                    String date = etDate.getText().toString().trim();

                    if (!validateExpenseInput(name, amountText, date, etName, etAmount, etDate)) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount = Double.parseDouble(amountText);
                    if (position >= 0) {
                        Expense expense = expenseList.get(position);
                        expense.setName(name);
                        expense.setAmount(amount);
                        expense.setDate(date);
                    } else {
                        expenseList.add(new Expense(name, amount, date));
                    }
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private boolean validateExpenseInput(String name, String amount, String date,
                                         EditText etName, EditText etAmount, EditText etDate) {
        boolean valid = true;
        if (name.isEmpty()) {
            etName.setError("Expense name is required");
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
        return valid;
    }

    @Override
    public void onEdit(int position) {
        showExpenseDialog(position);
    }

    @Override
    public void onDelete(int position) {
        expenseList.remove(position);
        adapter.notifyItemRemoved(position);
    }
}
