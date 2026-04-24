using System.Collections.Generic;
using System.Linq;
using System.Text.Json.Serialization;

namespace ExpenseTrackerUser
{
    public class Project
    {
        [JsonPropertyName("projectId")]
        [JsonNumberHandling(JsonNumberHandling.AllowReadingFromString | JsonNumberHandling.WriteAsString)]
        public int ProjectId { get; set; }

        [JsonPropertyName("name")]
        public string? Name { get; set; }

        [JsonPropertyName("projectCode")]
        public string? ProjectCode { get; set; }

        [JsonPropertyName("budget")]
        [JsonNumberHandling(JsonNumberHandling.AllowReadingFromString | JsonNumberHandling.WriteAsString)]
        public double Budget { get; set; }

        [JsonPropertyName("description")]
        public string? Description { get; set; }

        [JsonPropertyName("status")]
        public string? Status { get; set; }

        [JsonPropertyName("riskAssessment")]
        public string? RiskAssessment { get; set; }

        [JsonPropertyName("specialRequirements")]
        public string? SpecialRequirements { get; set; }

        [JsonPropertyName("clientInfo")]
        public string? ClientInfo { get; set; }

        [JsonPropertyName("manager")]
        public string? Manager { get; set; }

        [JsonPropertyName("startDate")]
        public string? StartDate { get; set; }

        [JsonPropertyName("endDate")]
        public string? EndDate { get; set; }

        [JsonPropertyName("date")]
        public string? Date { get; set; }

        [JsonPropertyName("expenses")]
        public List<Expense>? Expenses { get; set; }

        [JsonIgnore]
        public bool IsFavorite { get; set; }

        [JsonIgnore]
        public string DisplayDate => !string.IsNullOrWhiteSpace(EndDate)
            ? EndDate!
            : !string.IsNullOrWhiteSpace(StartDate)
                ? StartDate!
                : !string.IsNullOrWhiteSpace(Date)
                    ? Date!
                    : "Unknown";

        [JsonIgnore]
        public IEnumerable<Expense> ExpenseList => Expenses?.OrderByDescending(e => e.Date ?? string.Empty).ToList() ?? Enumerable.Empty<Expense>();

        [JsonIgnore]
        public double TotalSpent => Expenses?.Sum(x => x.Amount) ?? 0.0;

        [JsonIgnore]
        public double RemainingBudget => Budget - TotalSpent;

        [JsonIgnore]
        public double Progress => Budget <= 0 ? 0 : Math.Clamp(TotalSpent / Budget, 0, 1);

        [JsonIgnore]
        public bool IsOverBudget => TotalSpent > Budget;

        [JsonIgnore]
        public string BudgetStatus => IsOverBudget ? "Over Budget" : "Within Budget";

        [JsonIgnore]
        public string ProgressColor => IsOverBudget
            ? "#EF4444"
            : Progress >= 0.75
                ? "#FB923C"
                : "#38BDF8";

        [JsonIgnore]
        public string StatusIcon 
        {
            get
            {
                var s = (Status ?? string.Empty).ToLowerInvariant();
                var r = (RiskAssessment ?? string.Empty).ToLowerInvariant();

                // Priority 1: High risk or Over Budget
                if (s.Contains("risk") || r.Contains("risk") || r.Contains("high") || IsOverBudget || s == "critical")
                    return "⚠️";
                
                if (s == "blocked" || s == "stop")
                    return "⛔";

                if (s == "delayed")
                    return "⏳";

                // Priority 2: Healthy/Active statuses
                if (s == "on track" || s == "ontrack" || s == "safe" || s == "active" || s == "in progress")
                    return "🟢";

                // Priority 3: Completed statuses
                if (s == "completed" || s == "complete" || s == "done" || s == "finished" || s == "closed")
                    return "✅";

                return "ℹ️";
            }
        }

        [JsonIgnore]
        public string StatusLabel => string.IsNullOrWhiteSpace(Status) ? "Unknown" : Status!;

        [JsonIgnore]
        public string RiskLabel => string.IsNullOrWhiteSpace(RiskAssessment) ? "Unknown" : RiskAssessment!;

        [JsonIgnore]
        public string FavoriteLabel => IsFavorite ? "★ Favourite" : "☆ Mark favourite";
    }
}
