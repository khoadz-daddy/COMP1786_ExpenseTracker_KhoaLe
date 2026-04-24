package com.khoale.expensetracker;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Project implements Serializable {
    private int projectId;
    private String projectCode;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private String manager;
    private String status;
    private double budget;
    private String riskAssessment;
    private String specialRequirements;
    private String clientInfo;
    private List<Expense> expenses;

    public Project() {
        this.expenses = new ArrayList<>();
    }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public String getClientInfo() { return clientInfo; }
    public void setClientInfo(String clientInfo) { this.clientInfo = clientInfo; }

    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }

    @Exclude
    public double getTotalSpent() {
        double total = 0;
        if (expenses != null) {
            for (Expense e : expenses) {
                total += e.getAmount();
            }
        }
        return total;
    }

    @Exclude
    public int getUsagePercentage() {
        if (budget <= 0) return 0;
        return (int) ((getTotalSpent() / budget) * 100);
    }

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
