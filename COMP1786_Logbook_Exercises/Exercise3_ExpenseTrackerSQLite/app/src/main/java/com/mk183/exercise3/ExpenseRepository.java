package com.mk183.exercise3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class ExpenseRepository {
    private final DatabaseHelper dbHelper;

    public ExpenseRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public boolean insertExpense(Expense expense) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_NAME, expense.getName());
        values.put(DatabaseHelper.COL_AMOUNT, expense.getAmount());
        values.put(DatabaseHelper.COL_DATE, expense.getDate());
        values.put(DatabaseHelper.COL_CATEGORY, expense.getCategory());
        return db.insert(DatabaseHelper.TABLE_EXPENSES, null, values) != -1;
    }

    public boolean updateExpense(int id, Expense expense) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_NAME, expense.getName());
        values.put(DatabaseHelper.COL_AMOUNT, expense.getAmount());
        values.put(DatabaseHelper.COL_DATE, expense.getDate());
        values.put(DatabaseHelper.COL_CATEGORY, expense.getCategory());
        return db.update(DatabaseHelper.TABLE_EXPENSES, values,
                DatabaseHelper.COL_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean deleteExpense(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_EXPENSES,
                DatabaseHelper.COL_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public ArrayList<Expense> getAllExpenses() {
        ArrayList<Expense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_EXPENSES, null,
                null, null, null, null, DatabaseHelper.COL_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY));
                list.add(new Expense(id, name, amount, date, category));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
