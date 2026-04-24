package com.khoale.expensetracker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ExpenseTracker.db";
    private static final int DB_VERSION = 11; // Cập nhật schema

    // Bảng Projects
    public static final String TABLE_PROJECTS = "projects";
    public static final String COL_PROJECT_ID = "project_id";
    public static final String COL_CODE = "project_code"; // Thêm trường Code thủ công
    public static final String COL_NAME = "name";
    public static final String COL_DESC = "description";
    public static final String COL_START = "start_date";
    public static final String COL_END = "end_date";
    public static final String COL_MANAGER = "manager";
    public static final String COL_STATUS = "status";
    public static final String COL_BUDGET = "budget";
    public static final String COL_RISK = "risk_assessment";
    public static final String COL_SPECIAL = "special_requirements";
    public static final String COL_CLIENT = "client_info";

    // Bảng Expenses
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COL_EXP_ID = "expense_id";
    public static final String COL_EXP_PID = "project_id";
    public static final String COL_EXP_DATE = "date";
    public static final String COL_EXP_AMOUNT = "amount";
    public static final String COL_EXP_CURRENCY = "currency"; // NEW
    public static final String COL_EXP_TYPE = "type";
    public static final String COL_EXP_METHOD = "payment_method"; // NEW
    public static final String COL_EXP_CLAIMANT = "claimant"; // NEW
    public static final String COL_EXP_DESC = "description";
    public static final String COL_EXP_STATUS = "payment_status";
    public static final String COL_EXP_LOCATION = "location"; // NEW

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createProjectTable = "CREATE TABLE " + TABLE_PROJECTS + " (" +
                COL_PROJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CODE + " TEXT, " +
                COL_NAME + " TEXT NOT NULL, " +
                COL_DESC + " TEXT, " +
                COL_START + " TEXT, " +
                COL_END + " TEXT, " +
                COL_MANAGER + " TEXT, " +
                COL_STATUS + " TEXT, " +
                COL_BUDGET + " REAL, " +
                COL_RISK + " TEXT, " +
                COL_SPECIAL + " TEXT, " +
                COL_CLIENT + " TEXT)";

        String createExpenseTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                COL_EXP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EXP_PID + " INTEGER, " +
                COL_EXP_DATE + " TEXT, " +
                COL_EXP_AMOUNT + " REAL, " +
                COL_EXP_CURRENCY + " TEXT, " +
                COL_EXP_TYPE + " TEXT, " +
                COL_EXP_METHOD + " TEXT, " +
                COL_EXP_CLAIMANT + " TEXT, " +
                COL_EXP_DESC + " TEXT, " +
                COL_EXP_STATUS + " TEXT, " +
                COL_EXP_LOCATION + " TEXT, " +
                "FOREIGN KEY(" + COL_EXP_PID + ") REFERENCES " + TABLE_PROJECTS + "(" + COL_PROJECT_ID + ") ON DELETE CASCADE)";

        db.execSQL(createProjectTable);
        db.execSQL(createExpenseTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROJECTS);
        onCreate(db);
    }

    public Cursor getAllProjects() {
        return getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_PROJECTS + " ORDER BY " + COL_PROJECT_ID + " DESC", null);
    }
}
