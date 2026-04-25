package com.mk183.exercise2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    private final ArrayList<Expense> expenses;
    private final OnExpenseActionListener listener;

    public ExpenseAdapter(ArrayList<Expense> expenses, OnExpenseActionListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.tvName.setText(expense.getName());
        holder.tvAmount.setText(String.format("$%.2f", expense.getAmount()));
        holder.tvDate.setText(expense.getDate());

        holder.itemView.setOnLongClickListener(v -> {
            listener.onEdit(holder.getAdapterPosition());
            return true;
        });
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAmount;
        TextView tvDate;
        TextView btnDelete;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvExpenseName);
            tvAmount = itemView.findViewById(R.id.tvExpenseAmount);
            tvDate = itemView.findViewById(R.id.tvExpenseDate);
            btnDelete = itemView.findViewById(R.id.tvDelete);
        }
    }
}
