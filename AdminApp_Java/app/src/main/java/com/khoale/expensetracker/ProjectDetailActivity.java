package com.khoale.expensetracker;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectDetailActivity extends AppCompatActivity {

    private TextView tvProjectHeader;
    private EditText etExpName, etExpAmount, etExpDate, etClaimant, etLocation;
    private Spinner spType, spStatus, spCurrency, spMethod;
    private Button btnAddExp;
    private ImageButton btnBack, btnSyncDetail;
    private RecyclerView rvExpenses;
    private SwipeRefreshLayout swipeRefresh;
    
    private ProjectRepository repository;
    private Project currentProject;
    private ArrayList<Expense> expenseList;
    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        currentProject = (Project) getIntent().getSerializableExtra("PROJECT_OBJECT");
        if (currentProject == null) {
            finish();
            return;
        }

        repository = new ProjectRepository(this);
        initViews();
        tvProjectHeader.setText(currentProject.getName());
        
        btnBack.setOnClickListener(v -> finish());
        btnSyncDetail.setOnClickListener(v -> pullFromFirebase());
        
        setupRecyclerView();
        setupExpenseSwipeActions();
        setupDatePicker();
        
        swipeRefresh.setOnRefreshListener(this::pullFromFirebase);
        
        loadExpenses();

        btnAddExp.setOnClickListener(v -> handleAddExpense());
    }

    private void initViews() {
        tvProjectHeader = findViewById(R.id.tvProjectHeader);
        etExpName = findViewById(R.id.etExpenseName);
        etExpAmount = findViewById(R.id.etExpenseAmount);
        etExpDate = findViewById(R.id.etExpenseDate);
        etClaimant = findViewById(R.id.etClaimant);
        etLocation = findViewById(R.id.etLocation);
        
        spType = findViewById(R.id.spExpenseType);
        spStatus = findViewById(R.id.spPaymentStatus);
        spCurrency = findViewById(R.id.spCurrency);
        spMethod = findViewById(R.id.spPaymentMethod);
        
        btnAddExp = findViewById(R.id.btnAddExpense);
        btnBack = findViewById(R.id.btnBack);
        btnSyncDetail = findViewById(R.id.btnSyncDetail);
        rvExpenses = findViewById(R.id.recyclerViewExpenses);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // Pre-fill date
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        etExpDate.setText(today);
    }

    private void setupDatePicker() {
        etExpDate.setOnClickListener(v -> {
            android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                String date = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                etExpDate.setText(date);
            }, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
               java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
               java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH));
            dialog.show();
        });
    }

    private void setupRecyclerView() {
        expenseList = new ArrayList<>();
        adapter = new ExpenseAdapter(expenseList, null, null);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(adapter);
    }

    private void setupExpenseSwipeActions() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final ColorDrawable deleteBg = new ColorDrawable(Color.parseColor("#EF4444"));
            private final ColorDrawable editBg = new ColorDrawable(Color.parseColor("#3B82F6"));

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Expense expense = expenseList.get(position);
                if (direction == ItemTouchHelper.LEFT) showDeleteExpenseConfirm(expense, position);
                else showEditExpenseDialog(expense, position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                if (dX < 0) {
                    deleteBg.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    deleteBg.draw(c);
                } else if (dX > 0) {
                    editBg.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                    editBg.draw(c);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rvExpenses);
    }

    private void pullFromFirebase() {
        swipeRefresh.setRefreshing(true);
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Projects");
        
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Project> cloudProjects = new ArrayList<>();
                Project updatedCurrentProject = null;
                
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Project p = postSnapshot.getValue(Project.class);
                    if (p != null) {
                        cloudProjects.add(p);
                        if (p.getProjectId() == currentProject.getProjectId()) {
                            updatedCurrentProject = p;
                        }
                    }
                }
                
                if (!cloudProjects.isEmpty()) {
                    repository.replaceAllData(cloudProjects);
                    if (updatedCurrentProject != null) {
                        currentProject = updatedCurrentProject;
                    }
                    loadExpenses();
                    Toast.makeText(ProjectDetailActivity.this, "Data refreshed from Cloud", Toast.LENGTH_SHORT).show();
                }
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ProjectDetailActivity.this, "Sync failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteExpenseConfirm(Expense expense, int position) {
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (repository.deleteExpense(expense.getExpenseId())) loadExpenses();
                    else {
                        Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void showEditExpenseDialog(Expense expense, int position) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_expense, null);
        EditText etEditName = view.findViewById(R.id.etEditExpName);
        EditText etEditAmount = view.findViewById(R.id.etEditExpAmount);
        EditText etEditDate = view.findViewById(R.id.etEditExpDate);
        EditText etEditClaimant = view.findViewById(R.id.etEditExpClaimant);
        EditText etEditLocation = view.findViewById(R.id.etEditExpLocation);
        Spinner spEditCurrency = view.findViewById(R.id.spEditExpCurrency);
        Spinner spEditType = view.findViewById(R.id.spEditExpType);
        Spinner spEditMethod = view.findViewById(R.id.spEditExpMethod);
        Spinner spEditStatus = view.findViewById(R.id.spEditExpStatus);

        etEditName.setText(expense.getDescription());
        etEditAmount.setText(String.valueOf(expense.getAmount()));
        etEditDate.setText(expense.getDate());
        etEditClaimant.setText(expense.getClaimant());
        etEditLocation.setText(expense.getLocation());
        
        etEditDate.setOnClickListener(v -> {
            android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(this, (view1, year, month, day) -> {
                String d = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                etEditDate.setText(d);
            }, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
               java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
               java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        setSpinnerSelection(spEditCurrency, R.array.currency_options, expense.getCurrency());
        setSpinnerSelection(spEditType, R.array.expense_types, expense.getType());
        setSpinnerSelection(spEditMethod, R.array.method_options, expense.getPaymentMethod());
        setSpinnerSelection(spEditStatus, R.array.payment_statuses, expense.getPaymentStatus());

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Update Expense Details")
                .setView(view)
                .setPositiveButton("Update", (d, w) -> {
                    try {
                        String name = etEditName.getText().toString().trim();
                        String amtStr = etEditAmount.getText().toString().trim();
                        String date = etEditDate.getText().toString().trim();
                        String claimant = etEditClaimant.getText().toString().trim();

                        if (name.isEmpty() || amtStr.isEmpty() || date.isEmpty() || claimant.isEmpty()) {
                            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                            adapter.notifyItemChanged(position);
                            return;
                        }

                        expense.setDescription(name);
                        expense.setAmount(Double.parseDouble(amtStr));
                        expense.setDate(date);
                        expense.setClaimant(claimant);
                        expense.setLocation(etEditLocation.getText().toString().trim());
                        expense.setCurrency(spEditCurrency.getSelectedItem().toString());
                        expense.setType(spEditType.getSelectedItem().toString());
                        expense.setPaymentMethod(spEditMethod.getSelectedItem().toString());
                        expense.setPaymentStatus(spEditStatus.getSelectedItem().toString());

                        if (repository.updateExpense(expense)) {
                            loadExpenses();
                            Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
                        } else {
                            adapter.notifyItemChanged(position);
                        }
                    } catch (Exception e) {
                        adapter.notifyItemChanged(position);
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> adapter.notifyItemChanged(position))
                .show();
    }

    private void setSpinnerSelection(Spinner spinner, int arrayId, String value) {
        if (value == null) return;
        String[] items = getResources().getStringArray(arrayId);
        int pos = Arrays.asList(items).indexOf(value);
        if (pos >= 0) spinner.setSelection(pos);
    }

    private void handleAddExpense() {
        String name = etExpName.getText().toString().trim();
        String amountStr = etExpAmount.getText().toString().trim();
        String date = etExpDate.getText().toString().trim();
        String claimant = etClaimant.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String currency = spCurrency.getSelectedItem().toString();
        String type = spType.getSelectedItem().toString();
        String method = spMethod.getSelectedItem().toString();
        String status = spStatus.getSelectedItem().toString();

        if (name.isEmpty()) { etExpName.setError("Required"); return; }
        if (amountStr.isEmpty()) { etExpAmount.setError("Required"); return; }
        if (date.isEmpty()) { etExpDate.setError("Required"); return; }
        if (claimant.isEmpty()) { etClaimant.setError("Required"); return; }

        try {
            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(currentProject.getProjectId(), date, amount, currency, type, method, claimant, name, status, location);

            if (repository.insertExpense(expense)) {
                clearInputs();
                hideKeyboard();
                loadExpenses();
                Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            etExpAmount.setError("Invalid amount");
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void clearInputs() {
        etExpName.setText("");
        etExpAmount.setText("");
        etClaimant.setText("");
        etLocation.setText("");
        etExpName.requestFocus();
    }

    private void loadExpenses() {
        new Thread(() -> {
            ArrayList<Expense> data = repository.getExpensesByProject(currentProject.getProjectId());
            runOnUiThread(() -> {
                expenseList.clear();
                expenseList.addAll(data);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
}
