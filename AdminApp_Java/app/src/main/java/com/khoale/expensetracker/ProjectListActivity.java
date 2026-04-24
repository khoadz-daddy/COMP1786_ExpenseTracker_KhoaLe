package com.khoale.expensetracker;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import android.content.res.ColorStateList;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProjectAdapter adapter;
    private ProjectRepository repository;
    private ArrayList<Project> projectList;

    private EditText etSearchName;
    private TextView tvTotalBudget, tvTotalSpent;
    private LinearLayout layoutEmpty;
    private FloatingActionButton fabAdd;
    private ImageButton btnExport, btnSyncCloud;
    private SwipeRefreshLayout swipeRefresh;

    private String currentStatusFilter = "All";
    private static final int COLOR_ACTIVE_FILTER  = 0xFF4CAF50;
    private static final int COLOR_INACTIVE_FILTER = 0xFF2B2B2B;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_list);

        repository = new ProjectRepository(this);
        initViews();
        setupFilterButtons();
        setupSearchLogic();
        setupSwipeActions();

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        btnExport.setOnClickListener(v -> exportToCSV());
        btnSyncCloud.setOnClickListener(v -> pullFromFirebase());
        
        swipeRefresh.setOnRefreshListener(this::pullFromFirebase);

        refreshData();
    }

    private void initViews() {
        recyclerView   = findViewById(R.id.recyclerViewProjects);
        etSearchName   = findViewById(R.id.etSearch);
        tvTotalBudget  = findViewById(R.id.tvTotalBudget);
        tvTotalSpent   = findViewById(R.id.tvTotalSpent);
        layoutEmpty    = findViewById(R.id.layoutEmpty);
        fabAdd         = findViewById(R.id.fabAddProject);
        btnExport      = findViewById(R.id.btnExport);
        btnSyncCloud   = findViewById(R.id.btnSyncCloud);
        swipeRefresh   = findViewById(R.id.swipeRefreshList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        projectList = new ArrayList<>();
        adapter = new ProjectAdapter(projectList, this::onItemClick, this::onItemLongClick);
        recyclerView.setAdapter(adapter);
    }

    private void setupFilterButtons() {
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> setStatusFilter("All"));
        findViewById(R.id.btnFilterActive).setOnClickListener(v -> setStatusFilter("Active"));
        findViewById(R.id.btnFilterAtRisk).setOnClickListener(v -> setStatusFilter("At Risk"));
        findViewById(R.id.btnFilterDone).setOnClickListener(v -> setStatusFilter("Done"));
        updateFilterButtonColors();
    }

    private void setStatusFilter(String status) {
        currentStatusFilter = status;
        updateFilterButtonColors();
        performSearch();
    }

    private void updateFilterButtonColors() {
        setFilterBtnColor(findViewById(R.id.btnFilterAll), "All");
        setFilterBtnColor(findViewById(R.id.btnFilterActive), "Active");
        setFilterBtnColor(findViewById(R.id.btnFilterAtRisk), "At Risk");
        setFilterBtnColor(findViewById(R.id.btnFilterDone), "Done");
    }

    private void setFilterBtnColor(Button btn, String filter) {
        boolean selected = filter.equals(currentStatusFilter);
        int color = selected ? COLOR_ACTIVE_FILTER : COLOR_INACTIVE_FILTER;
        ViewCompat.setBackgroundTintList(btn, ColorStateList.valueOf(color));
    }

    private void pullFromFirebase() {
        swipeRefresh.setRefreshing(true);
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Projects");
        
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Project> cloudProjects = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Project p = postSnapshot.getValue(Project.class);
                    if (p != null) cloudProjects.add(p);
                }
                
                if (!cloudProjects.isEmpty()) {
                    repository.replaceAllData(cloudProjects);
                    refreshData();
                    Toast.makeText(ProjectListActivity.this, "Database updated from Cloud", Toast.LENGTH_SHORT).show();
                }
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ProjectListActivity.this, "Sync failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportToCSV() {
        List<Project> allProjects = repository.getAllProjectsWithExpenses();
        if (allProjects.isEmpty()) return;

        StringBuilder csvData = new StringBuilder();
        csvData.append("Project Code,Project Name,Budget,Status,Manager,Start Date,End Date,Expense Name,Expense Type,Expense Amount,Currency,Expense Date,Payment Method,Claimant,Location,Expense Status\n");

        for (Project p : allProjects) {
            List<Expense> expenses = p.getExpenses();
            if (expenses == null || expenses.isEmpty()) {
                csvData.append(String.format(Locale.getDefault(), "%s,%s,%.2f,%s,%s,%s,%s,,,,,,,,,\n",
                    p.getProjectCode(), p.getName(), p.getBudget(), p.getStatus(), p.getManager(), p.getStartDate(), p.getEndDate()));
            } else {
                for (Expense e : expenses) {
                    csvData.append(String.format(Locale.getDefault(), "%s,%s,%.2f,%s,%s,%s,%s,%s,%s,%.2f,%s,%s,%s,%s,%s,%s\n",
                        p.getProjectCode(), p.getName(), p.getBudget(), p.getStatus(), p.getManager(), p.getStartDate(), p.getEndDate(),
                        e.getDescription(), e.getType(), e.getAmount(), e.getCurrency(), e.getDate(), e.getPaymentMethod(), e.getClaimant(), e.getLocation(), e.getPaymentStatus()));
                }
            }
        }

        try {
            File file = new File(getExternalFilesDir(null), "ProjectReport.csv");
            FileOutputStream out = new FileOutputStream(file);
            out.write(csvData.toString().getBytes());
            out.close();

            Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Project Expense Report");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(intent, "Share Report via"));
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSystemOverview() {
        new Thread(() -> {
            double[] totals = repository.getSystemTotals();
            runOnUiThread(() -> {
                tvTotalBudget.setText(String.format(Locale.getDefault(), "$%.2f", totals[0]));
                tvTotalSpent.setText(String.format(Locale.getDefault(), "$%.2f", totals[1]));
            });
        }).start();
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final ColorDrawable deleteBg = new ColorDrawable(Color.parseColor("#EF4444"));
            private final ColorDrawable editBg   = new ColorDrawable(Color.parseColor("#3B82F6"));

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Project project = projectList.get(position);
                if (direction == ItemTouchHelper.LEFT) {
                    new AlertDialog.Builder(ProjectListActivity.this)
                            .setTitle(R.string.delete_project)
                            .setMessage(R.string.are_you_sure)
                            .setPositiveButton(R.string.delete_project, (d, w) -> {
                                if (repository.deleteProject(project.getProjectId())) refreshData();
                            })
                            .setNegativeButton(android.R.string.cancel, (d, w) -> adapter.notifyItemChanged(position))
                            .show();
                } else {
                    new EditProjectDialog(ProjectListActivity.this, project, updated -> {
                        if (repository.updateProject(updated)) refreshData();
                    }, () -> adapter.notifyItemChanged(position)).show();
                }
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
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void setupSearchLogic() {
        etSearchName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { performSearch(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch() {
        String nameQuery = etSearchName.getText().toString();
        new Thread(() -> {
            ArrayList<Project> results = repository.searchProjects(nameQuery, currentStatusFilter);
            runOnUiThread(() -> {
                projectList.clear();
                projectList.addAll(results);
                layoutEmpty.setVisibility(projectList.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(projectList.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.notifyDataSetChanged();
                updateSystemOverview();
            });
        }).start();
    }

    private void refreshData() { performSearch(); }

    private void onItemClick(Project project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra("PROJECT_OBJECT", project);
        startActivity(intent);
    }

    private void onItemLongClick(Project project) {}

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }
}
