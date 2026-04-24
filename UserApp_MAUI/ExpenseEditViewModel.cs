using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using CommunityToolkit.Maui.Alerts;
using CommunityToolkit.Maui.Core;

namespace ExpenseTrackerUser;

[QueryProperty(nameof(Project), "Project")]
[QueryProperty(nameof(ExpenseToEdit), "ExpenseToEdit")]
public partial class ExpenseEditViewModel : ObservableObject
{
    private readonly FirebaseService _firebaseService;
    private const string FirebaseProjectsUrl = "https://expensetrackercloud-891dd-default-rtdb.firebaseio.com/Projects.json";

    [ObservableProperty]
    private Project? project;

    [ObservableProperty]
    private Expense? expenseToEdit;

    [ObservableProperty]
    private string description = string.Empty;

    [ObservableProperty]
    private double amount;

    [ObservableProperty]
    private string type = "Materials";

    [ObservableProperty]
    private DateTime selectedDate = DateTime.Today;

    [ObservableProperty]
    private string paymentStatus = "Pending";

    [ObservableProperty]
    private string currency = "USD ($)";

    [ObservableProperty]
    private string paymentMethod = "Cash";

    [ObservableProperty]
    private string claimant = string.Empty;

    [ObservableProperty]
    private string location = string.Empty;

    public string Title => ExpenseToEdit == null ? "Add Expense" : "Edit Expense";

    public ExpenseEditViewModel(FirebaseService firebaseService)
    {
        _firebaseService = firebaseService;
    }

    partial void OnExpenseToEditChanged(Expense? value)
    {
        if (value != null)
        {
            Description = value.Description ?? string.Empty;
            Amount = value.Amount;
            Type = value.Type ?? "Miscellaneous";
            if (DateTime.TryParseExact(value.Date, "dd/MM/yyyy", null, System.Globalization.DateTimeStyles.None, out DateTime d))
            {
                SelectedDate = d;
            }
            else if (DateTime.TryParse(value.Date, out DateTime fallback))
            {
                SelectedDate = fallback;
            }
            else
            {
                SelectedDate = DateTime.Today;
            }
            PaymentStatus = value.PaymentStatus ?? "Pending";
            Currency = value.Currency ?? "USD ($)";
            PaymentMethod = value.PaymentMethod ?? "Cash";
            Claimant = value.Claimant ?? string.Empty;
            Location = value.Location ?? string.Empty;
        }
        else
        {
            SelectedDate = DateTime.Today;
            Type = "Materials";
            PaymentStatus = "Pending";
            Currency = "USD ($)";
            PaymentMethod = "Cash";
            Claimant = string.Empty;
            Location = string.Empty;
        }
        OnPropertyChanged(nameof(Title));
    }

    [RelayCommand]
    private async Task SaveAsync()
    {
        if (Project == null) return;
        if (string.IsNullOrWhiteSpace(Description) || string.IsNullOrWhiteSpace(Claimant) || Amount <= 0)
        {
            await Shell.Current.CurrentPage.DisplayAlertAsync("Validation Error", "Description, claimant and amount are required.", "OK");
            return;
        }

        try
        {
            string formattedDate = SelectedDate.ToString("dd/MM/yyyy");
            bool isNew = ExpenseToEdit == null;

            var expenseMutation = new Expense
            {
                ExpenseId = isNew ? 0 : ExpenseToEdit!.ExpenseId,
                ProjectId = Project.ProjectId,
                Description = Description,
                Amount = Amount,
                Type = Type,
                Date = formattedDate,
                PaymentStatus = PaymentStatus,
                Currency = Currency,
                PaymentMethod = PaymentMethod,
                Claimant = Claimant,
                Location = Location
            };

            // Fetch, merge and save to Firebase
            var updatedExpenses = await _firebaseService.ModifyExpenseAsync(FirebaseProjectsUrl, Project.ProjectId, expenseMutation, null);

            // Update local project model
            Project.Expenses = updatedExpenses;

            // Toast
            var toast = Toast.Make(isNew ? "Đã thêm chi tiêu!" : "Đã cập nhật chi tiêu!", ToastDuration.Short, 14);
            await toast.Show();

            // Go back
            await Shell.Current.GoToAsync("..");
        }
        catch (Exception ex)
        {
            await Shell.Current.CurrentPage.DisplayAlertAsync("Error", $"Failed to save expense: {ex.Message}", "OK");
        }
    }

    [RelayCommand]
    private async Task CancelAsync()
    {
        await Shell.Current.GoToAsync("..");
    }
}
