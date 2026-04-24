package com.khoale.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    private final DatabaseHelper dbHelper;

    public ProjectRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // --- HELPER LOGIC (Đồng bộ 100% với User App) ---

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase().trim().replace("_", " ").replace("-", " ");
    }

    public boolean isAtRisk(Project p) {
        String status = normalize(p.getStatus());
        String riskAttr = normalize(p.getRiskAssessment());
        boolean isOverBudget = p.getTotalSpent() > p.getBudget();

        // Thỏa 1 trong 3: Status chứa "risk" OR RiskAssessment chứa "high/yes/risk" OR Over Budget
        return status.contains("risk") || 
               riskAttr.contains("high") || riskAttr.contains("yes") || riskAttr.contains("risk") || 
               isOverBudget;
    }

    public boolean isActive(Project p) {
        String status = normalize(p.getStatus());
        // Fix: Theo hình ảnh User App, Active CHỈ quan tâm đến exact match status
        // Không loại trừ At Risk để đảm bảo hiện đầy đủ dự án đang chạy
        return status.equals("active") || status.equals("in progress") || status.equals("on track");
    }

    public boolean isCompleted(Project p) {
        String status = normalize(p.getStatus());
        // Exact match: "completed" OR "complete" OR "done" OR "finished" OR "closed"
        boolean isCompletedStatus = status.equals("completed") || status.equals("complete") || 
                                   status.equals("done") || status.equals("finished") || status.equals("closed");
        // Theo yêu cầu: Completed PHẢI loại trừ At Risk (không lố tiền)
        return isCompletedStatus && !isAtRisk(p);
    }

    // --- CORE OPERATIONS ---

    public ArrayList<Project> searchProjects(String nameQuery, String filterCategory) {
        ArrayList<Project> allProjects = (ArrayList<Project>) getAllProjectsWithExpenses();
        ArrayList<Project> filteredList = new ArrayList<>();

        for (Project p : allProjects) {
            if (nameQuery != null && !nameQuery.isEmpty()) {
                String query = nameQuery.toLowerCase();
                boolean matchesSearch = p.getName().toLowerCase().contains(query) ||
                        (p.getDescription() != null && p.getDescription().toLowerCase().contains(query)) ||
                        (p.getManager() != null && p.getManager().toLowerCase().contains(query)) ||
                        (p.getStartDate() != null && p.getStartDate().contains(query)) ||
                        (p.getEndDate() != null && p.getEndDate().contains(query)) ||
                        (p.getStatus() != null && p.getStatus().toLowerCase().contains(query)) ||
                        (p.getSpecialRequirements() != null && p.getSpecialRequirements().toLowerCase().contains(query)) ||
                        (p.getClientInfo() != null && p.getClientInfo().toLowerCase().contains(query));
                
                if (!matchesSearch) continue;
            }

            boolean matches = false;
            if (filterCategory == null || filterCategory.equals("All") || filterCategory.equals("All Risks")) {
                matches = true;
            } else if (filterCategory.equals("Active")) {
                matches = isActive(p);
            } else if (filterCategory.equals("At Risk")) {
                matches = isAtRisk(p);
            } else if (filterCategory.equals("Done") || filterCategory.equals("Completed")) {
                matches = isCompleted(p);
            }

            if (matches) filteredList.add(p);
        }
        return filteredList;
    }

    // --- DATABASE CRUD ---

    public boolean insertProject(Project p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_CODE, p.getProjectCode()); // NEW
        cv.put(DatabaseHelper.COL_NAME, p.getName());
        cv.put(DatabaseHelper.COL_DESC, p.getDescription());
        cv.put(DatabaseHelper.COL_START, p.getStartDate());
        cv.put(DatabaseHelper.COL_END, p.getEndDate());
        cv.put(DatabaseHelper.COL_MANAGER, p.getManager());
        cv.put(DatabaseHelper.COL_STATUS, p.getStatus());
        cv.put(DatabaseHelper.COL_BUDGET, p.getBudget());
        cv.put(DatabaseHelper.COL_RISK, p.getRiskAssessment());
        cv.put(DatabaseHelper.COL_SPECIAL, p.getSpecialRequirements());
        cv.put(DatabaseHelper.COL_CLIENT, p.getClientInfo());
        return db.insert(DatabaseHelper.TABLE_PROJECTS, null, cv) != -1;
    }

    public boolean updateProject(Project p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_CODE, p.getProjectCode()); // NEW
        cv.put(DatabaseHelper.COL_NAME, p.getName());
        cv.put(DatabaseHelper.COL_DESC, p.getDescription());
        cv.put(DatabaseHelper.COL_START, p.getStartDate());
        cv.put(DatabaseHelper.COL_END, p.getEndDate());
        cv.put(DatabaseHelper.COL_MANAGER, p.getManager());
        cv.put(DatabaseHelper.COL_STATUS, p.getStatus());
        cv.put(DatabaseHelper.COL_BUDGET, p.getBudget());
        cv.put(DatabaseHelper.COL_RISK, p.getRiskAssessment());
        cv.put(DatabaseHelper.COL_SPECIAL, p.getSpecialRequirements());
        cv.put(DatabaseHelper.COL_CLIENT, p.getClientInfo());
        return db.update(DatabaseHelper.TABLE_PROJECTS, cv, DatabaseHelper.COL_PROJECT_ID + "=?", new String[]{String.valueOf(p.getProjectId())}) > 0;
    }

    public boolean deleteProject(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_PROJECTS, DatabaseHelper.COL_PROJECT_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public void resetDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DatabaseHelper.TABLE_EXPENSES, null, null);
            db.delete(DatabaseHelper.TABLE_PROJECTS, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Project> getAllProjectsWithExpenses() {
        List<Project> projectList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PROJECTS, null, null, null, null, null, DatabaseHelper.COL_PROJECT_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Project p = mapCursorToProject(cursor);
                p.setExpenses(getExpensesByProject(p.getProjectId()));
                projectList.add(p);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return projectList;
    }

    public ArrayList<Expense> getExpensesByProject(int projectId) {
        ArrayList<Expense> list = new ArrayList<>();
        if (projectId <= 0) return list;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_EXPENSES, null, DatabaseHelper.COL_EXP_PID + "=?", 
                new String[]{String.valueOf(projectId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Expense e = new Expense();
                e.setExpenseId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_ID)));
                e.setProjectId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_PID)));
                e.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_DATE)));
                e.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_AMOUNT)));
                e.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_CURRENCY)));
                e.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_TYPE)));
                e.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_METHOD)));
                e.setClaimant(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_CLAIMANT)));
                e.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_DESC)));
                e.setPaymentStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_STATUS)));
                e.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXP_LOCATION)));
                list.add(e);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean insertExpense(Expense e) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_EXP_PID, e.getProjectId());
        cv.put(DatabaseHelper.COL_EXP_DATE, e.getDate());
        cv.put(DatabaseHelper.COL_EXP_AMOUNT, e.getAmount());
        cv.put(DatabaseHelper.COL_EXP_CURRENCY, e.getCurrency());
        cv.put(DatabaseHelper.COL_EXP_TYPE, e.getType());
        cv.put(DatabaseHelper.COL_EXP_METHOD, e.getPaymentMethod());
        cv.put(DatabaseHelper.COL_EXP_CLAIMANT, e.getClaimant());
        cv.put(DatabaseHelper.COL_EXP_DESC, e.getDescription());
        cv.put(DatabaseHelper.COL_EXP_STATUS, e.getPaymentStatus());
        cv.put(DatabaseHelper.COL_EXP_LOCATION, e.getLocation());
        long id = db.insert(DatabaseHelper.TABLE_EXPENSES, null, cv);
        if (id != -1) { e.setExpenseId((int) id); return true; }
        return false;
    }

    public boolean updateExpense(Expense e) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_EXP_DATE, e.getDate());
        cv.put(DatabaseHelper.COL_EXP_AMOUNT, e.getAmount());
        cv.put(DatabaseHelper.COL_EXP_CURRENCY, e.getCurrency());
        cv.put(DatabaseHelper.COL_EXP_TYPE, e.getType());
        cv.put(DatabaseHelper.COL_EXP_METHOD, e.getPaymentMethod());
        cv.put(DatabaseHelper.COL_EXP_CLAIMANT, e.getClaimant());
        cv.put(DatabaseHelper.COL_EXP_DESC, e.getDescription());
        cv.put(DatabaseHelper.COL_EXP_STATUS, e.getPaymentStatus());
        cv.put(DatabaseHelper.COL_EXP_LOCATION, e.getLocation());
        return db.update(DatabaseHelper.TABLE_EXPENSES, cv, DatabaseHelper.COL_EXP_ID + "=?", 
                new String[]{String.valueOf(e.getExpenseId())}) > 0;
    }

    public boolean deleteExpense(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_EXPENSES, DatabaseHelper.COL_EXP_ID + "=?", 
                new String[]{String.valueOf(id)}) > 0;
    }

    public double[] getSystemTotals() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double[] totals = new double[2];
        Cursor c1 = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_BUDGET + ") FROM " + DatabaseHelper.TABLE_PROJECTS, null);
        if (c1 != null && c1.moveToFirst()) { totals[0] = c1.getDouble(0); c1.close(); }
        Cursor c2 = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_EXP_AMOUNT + ") FROM " + DatabaseHelper.TABLE_EXPENSES, null);
        if (c2 != null && c2.moveToFirst()) { totals[1] = c2.getDouble(0); c2.close(); }
        return totals;
    }

    public void replaceAllData(List<Project> newProjects) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DatabaseHelper.TABLE_EXPENSES, null, null);
            db.delete(DatabaseHelper.TABLE_PROJECTS, null, null);

            for (Project p : newProjects) {
                ContentValues pCv = new ContentValues();
                pCv.put(DatabaseHelper.COL_PROJECT_ID, p.getProjectId());
                pCv.put(DatabaseHelper.COL_CODE, p.getProjectCode());
                pCv.put(DatabaseHelper.COL_NAME, p.getName());
                pCv.put(DatabaseHelper.COL_DESC, p.getDescription());
                pCv.put(DatabaseHelper.COL_START, p.getStartDate());
                pCv.put(DatabaseHelper.COL_END, p.getEndDate());
                pCv.put(DatabaseHelper.COL_MANAGER, p.getManager());
                pCv.put(DatabaseHelper.COL_STATUS, p.getStatus());
                pCv.put(DatabaseHelper.COL_BUDGET, p.getBudget());
                pCv.put(DatabaseHelper.COL_RISK, p.getRiskAssessment());
                pCv.put(DatabaseHelper.COL_SPECIAL, p.getSpecialRequirements());
                pCv.put(DatabaseHelper.COL_CLIENT, p.getClientInfo());
                db.insert(DatabaseHelper.TABLE_PROJECTS, null, pCv);

                if (p.getExpenses() != null) {
                    for (Expense e : p.getExpenses()) {
                        ContentValues eCv = new ContentValues();
                        eCv.put(DatabaseHelper.COL_EXP_ID, e.getExpenseId());
                        eCv.put(DatabaseHelper.COL_EXP_PID, e.getProjectId());
                        eCv.put(DatabaseHelper.COL_EXP_DATE, e.getDate());
                        eCv.put(DatabaseHelper.COL_EXP_AMOUNT, e.getAmount());
                        eCv.put(DatabaseHelper.COL_EXP_CURRENCY, e.getCurrency());
                        eCv.put(DatabaseHelper.COL_EXP_TYPE, e.getType());
                        eCv.put(DatabaseHelper.COL_EXP_METHOD, e.getPaymentMethod());
                        eCv.put(DatabaseHelper.COL_EXP_CLAIMANT, e.getClaimant());
                        eCv.put(DatabaseHelper.COL_EXP_DESC, e.getDescription());
                        eCv.put(DatabaseHelper.COL_EXP_STATUS, e.getPaymentStatus());
                        eCv.put(DatabaseHelper.COL_EXP_LOCATION, e.getLocation());
                        db.insert(DatabaseHelper.TABLE_EXPENSES, null, eCv);
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private Project mapCursorToProject(Cursor cursor) {
        Project p = new Project();
        p.setProjectId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROJECT_ID)));
        p.setProjectCode(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CODE)));
        p.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)));
        p.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESC)));
        p.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_START)));
        p.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_END)));
        p.setManager(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MANAGER)));
        p.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_STATUS)));
        p.setBudget(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BUDGET)));
        p.setRiskAssessment(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_RISK)));
        p.setSpecialRequirements(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SPECIAL)));
        p.setClientInfo(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CLIENT)));
        return p;
    }
}
