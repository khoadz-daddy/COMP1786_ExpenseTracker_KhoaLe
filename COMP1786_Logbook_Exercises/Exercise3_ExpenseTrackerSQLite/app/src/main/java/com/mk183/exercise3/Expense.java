package com.mk183.exercise3;

public class Expense {
    private int id;
    private String name;
    private double amount;
    private String date;
    private String category;

    public Expense(int id, String name, double amount, String date, String category) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.date = date;
        this.category = category;
    }

    public Expense(String name, double amount, String date, String category) {
        this(0, name, amount, date, category);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }
}
