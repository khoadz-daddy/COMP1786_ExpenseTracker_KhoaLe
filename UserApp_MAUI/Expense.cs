using System.Text.Json.Serialization;

namespace ExpenseTrackerUser
{
    public class Expense
    {
        [JsonPropertyName("expenseId")]
        [JsonNumberHandling(JsonNumberHandling.AllowReadingFromString | JsonNumberHandling.WriteAsString)]
        public int ExpenseId { get; set; }

        [JsonPropertyName("projectId")]
        [JsonNumberHandling(JsonNumberHandling.AllowReadingFromString | JsonNumberHandling.WriteAsString)]
        public int ProjectId { get; set; }

        [JsonPropertyName("description")]
        public string? Description { get; set; }

        [JsonPropertyName("amount")]
        [JsonNumberHandling(JsonNumberHandling.AllowReadingFromString | JsonNumberHandling.WriteAsString)]
        public double Amount { get; set; }

        [JsonPropertyName("type")]
        public string? Type { get; set; }

        [JsonPropertyName("date")]
        public string? Date { get; set; }

        [JsonPropertyName("paymentStatus")]
        public string? PaymentStatus { get; set; }

        [JsonPropertyName("currency")]
        public string? Currency { get; set; }

        [JsonPropertyName("paymentMethod")]
        public string? PaymentMethod { get; set; }

        [JsonPropertyName("claimant")]
        public string? Claimant { get; set; }

        [JsonPropertyName("location")]
        public string? Location { get; set; }

        public string DisplayAmount => $"💰 {Amount.ToString("C0")} {(string.IsNullOrWhiteSpace(Currency) ? string.Empty : Currency)}";
        public string DisplayDate => $"📅 {(string.IsNullOrWhiteSpace(Date) ? "Unknown" : Date)}";
        public string DisplayStatus => $"📌 {(string.IsNullOrWhiteSpace(PaymentStatus) ? "Unknown" : PaymentStatus)}";
        public string DisplayMeta => $"🏷 {(string.IsNullOrWhiteSpace(Type) ? "Unknown" : Type)} • {(string.IsNullOrWhiteSpace(PaymentMethod) ? "Unknown method" : PaymentMethod)}";
        public string DisplayOwner => $"👤 {(string.IsNullOrWhiteSpace(Claimant) ? "Unknown claimant" : Claimant)}";
    }
}
