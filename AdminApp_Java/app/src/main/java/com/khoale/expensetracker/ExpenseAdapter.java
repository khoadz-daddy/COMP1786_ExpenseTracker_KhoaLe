package com.khoale.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final ArrayList<Expense> expenseList;

    public interface OnExpenseClick {
        void onClick(Expense expense);
    }

    public interface OnExpenseLongClick {
        void onLongClick(Expense expense);
    }

    private final OnExpenseClick clickListener;
    private final OnExpenseLongClick longClickListener;

    public ExpenseAdapter(ArrayList<Expense> expenseList,
                          OnExpenseClick clickListener,
                          OnExpenseLongClick longClickListener) {
        this.expenseList = expenseList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvAmount, tvDate, tvStatus, tvClaimant, tvLocation;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvExpenseName);
            tvType = itemView.findViewById(R.id.tvExpenseType);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvClaimant = itemView.findViewById(R.id.tvClaimant);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }

        public void bind(Expense e) {
            tvName.setText(e.getDescription());
            tvType.setText(e.getType());
            
            String currency = (e.getCurrency() != null) ? e.getCurrency() : "USD ($)";
            tvAmount.setText(String.format(Locale.getDefault(), "💰 %.2f %s", e.getAmount(), currency));
            tvDate.setText("📅 " + e.getDate());
            tvStatus.setText("📌 " + e.getPaymentStatus());
            tvClaimant.setText("👤 Claimant: " + (e.getClaimant() != null ? e.getClaimant() : "-"));
            
            if (e.getLocation() != null && !e.getLocation().isEmpty()) {
                tvLocation.setVisibility(View.VISIBLE);
                tvLocation.setText("📍 Location: " + e.getLocation());
            } else {
                tvLocation.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onClick(e);
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onLongClick(e);
                return true;
            });
        }
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        if (expenseList == null || expenseList.isEmpty()) return;
        holder.bind(expenseList.get(position));
    }

    @Override
    public int getItemCount() {
        return expenseList != null ? expenseList.size() : 0;
    }
}