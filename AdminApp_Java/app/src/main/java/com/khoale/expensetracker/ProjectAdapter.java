package com.khoale.expensetracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private final ArrayList<Project> projectList;
    private final OnProjectClick clickListener;
    private final OnProjectLongClick longClickListener;

    public interface OnProjectClick {
        void onClick(Project project);
    }

    public interface OnProjectLongClick {
        void onLongClick(Project project);
    }

    public ProjectAdapter(ArrayList<Project> projectList, OnProjectClick clickListener, OnProjectLongClick longClickListener) {
        this.projectList = projectList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvName, tvManager, tvBudget, tvStartDate, tvStatus, tvUsage;
        ProgressBar pbBudget;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvProjectCode);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvManager = itemView.findViewById(R.id.tvManager);
            tvBudget = itemView.findViewById(R.id.tvBudget);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUsage = itemView.findViewById(R.id.tvUsageInfo);
            pbBudget = itemView.findViewById(R.id.pbBudget);
        }

        public void bind(Project project) {
            Context context = itemView.getContext();
            tvCode.setText(project.getProjectCode() != null ? project.getProjectCode() : "N/A");
            tvName.setText(project.getName());
            tvManager.setText(project.getManager());
            tvStatus.setText(project.getStatus());

            tvBudget.setText(context.getString(R.string.budget_label, project.getBudget()));
            tvStartDate.setText(context.getString(R.string.start_label, project.getStartDate()));

            int percentage = project.getUsagePercentage();
            double spent = project.getTotalSpent();
            tvUsage.setText(context.getString(R.string.usage_label, percentage, spent));

            // Cập nhật ProgressBar
            pbBudget.setProgress(Math.min(percentage, 100));

            // Logic màu sắc sống động (Dynamic Colors)
            int statusColor;
            if (percentage < 50) {
                // Dưới 50%: Xanh lá (An toàn)
                statusColor = ContextCompat.getColor(context, R.color.accent_green);
            } else if (percentage < 80) {
                // 50% - 80%: Xanh dương (Bình thường)
                statusColor = ContextCompat.getColor(context, R.color.accent_blue);
            } else if (percentage <= 100) {
                // 80% - 100%: Cam (Cảnh báo gần đầy)
                statusColor = ContextCompat.getColor(context, R.color.accent_orange);
            } else {
                // Trên 100%: Đỏ (Vượt ngân sách)
                statusColor = ContextCompat.getColor(context, R.color.accent_red);
            }

            pbBudget.setProgressTintList(ColorStateList.valueOf(statusColor));
            tvUsage.setTextColor(statusColor);

            itemView.setOnClickListener(v -> clickListener.onClick(project));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onLongClick(project);
                return true;
            });
        }
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.bind(projectList.get(position));
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }
}