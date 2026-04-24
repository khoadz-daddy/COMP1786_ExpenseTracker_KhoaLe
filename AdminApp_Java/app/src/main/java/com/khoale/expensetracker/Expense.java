package com.khoale.expensetracker;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

/**
 * Model class for Expense - COMP1786 Standard
 */
@IgnoreExtraProperties
public class Expense implements Serializable {
    private int expenseId;
    private int projectId; // Link to Project
    private String date;
    private double amount;
    private String currency;
    private String type;
    private String paymentMethod;
    private String claimant;
    private String description;
    private String paymentStatus;
    private String location;

    public Expense() {}

    public Expense(int projectId, String date, double amount, String currency, String type, String paymentMethod, String claimant, String description, String paymentStatus, String location) {
        this.projectId = projectId;
        this.date = date;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.claimant = claimant;
        this.description = description;
        this.paymentStatus = paymentStatus;
        this.location = location;
    }

    // Getters and Setters with robust conversion
    public int getExpenseId() { return expenseId; }
    public void setExpenseId(Object expenseId) { 
        this.expenseId = convertToInt(expenseId); 
    }

    public int getProjectId() { return projectId; }
    public void setProjectId(Object projectId) { 
        this.projectId = convertToInt(projectId); 
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getAmount() { return amount; }
    public void setAmount(Object amount) { 
        this.amount = convertToDouble(amount); 
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getClaimant() { return claimant; }
    public void setClaimant(String claimant) { this.claimant = claimant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // Helper methods to handle dirty data from Cloud
    private double convertToDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble((String) obj); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private int convertToInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); } catch (Exception e) { return 0; }
        }
        return 0;
    }
}
