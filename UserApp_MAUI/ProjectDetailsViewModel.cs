using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Maui.Storage;

namespace ExpenseTrackerUser;

[QueryProperty(nameof(Project), "Project")]
public partial class ProjectDetailsViewModel : ObservableObject
{
    private const string FavoritesPreferenceKey = "favorite_project_ids";
    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(HasNoExpenses))]
    private Project? project;

    public bool HasNoExpenses => Project?.Expenses == null || !Project.Expenses.Any();
    public bool IsFavorite => Project != null && Project.IsFavorite;

    private readonly FirebaseService _firebaseService;
    private const string FirebaseProjectsUrl = "https://expensetrackercloud-891dd-default-rtdb.firebaseio.com/Projects.json";

    public ProjectDetailsViewModel(FirebaseService firebaseService)
    {
        _firebaseService = firebaseService;
    }

    public void Refresh()
    {
        if (Project != null)
        {
            var favorites = LoadFavoriteIds();
            Project.IsFavorite = favorites.Contains(Project.ProjectId);
        }
        OnPropertyChanged(nameof(Project));
        OnPropertyChanged(nameof(HasNoExpenses));
        OnPropertyChanged(nameof(IsFavorite));
    }

    [RelayCommand]
    private async Task AddExpenseAsync()
    {
        if (Project == null) return;
        var navigationParameter = new Dictionary<string, object>
        {
            { "Project", Project },
            { "ExpenseToEdit", null }
        };
        await Shell.Current.GoToAsync("ExpenseEditPage", navigationParameter);
    }

    [RelayCommand]
    private async Task EditExpenseAsync(Expense expense)
    {
        if (Project == null || expense == null) return;
        var navigationParameter = new Dictionary<string, object>
        {
            { "Project", Project },
            { "ExpenseToEdit", expense }
        };
        await Shell.Current.GoToAsync("ExpenseEditPage", navigationParameter);
    }

    [RelayCommand]
    private async Task DeleteExpenseAsync(Expense expense)
    {
        if (Project == null || expense == null) return;
        
        bool confirm = await Shell.Current.CurrentPage.DisplayAlertAsync("Xác nhận", "Xóa chi tiêu này?", "Yes", "No");
        if (!confirm) return;

        try
        {
            var updatedExpenses = await _firebaseService.ModifyExpenseAsync(FirebaseProjectsUrl, Project.ProjectId, null, expense.ExpenseId);
            
            Project.Expenses = updatedExpenses;
            Refresh();
        }
        catch (Exception ex)
        {
            await Shell.Current.CurrentPage.DisplayAlertAsync("Error", $"Failed to delete expense: {ex.Message}", "OK");
        }
    }

    [RelayCommand]
    private async Task ExportToCsvAsync()
    {
        if (Project?.Expenses == null || !Project.Expenses.Any())
        {
            await Shell.Current.CurrentPage.DisplayAlertAsync("Export", "No expenses to export.", "OK");
            return;
        }

        try
        {
            var csvContent = "Expense ID,Project ID,Description,Amount,Currency,Type,Date,Payment Method,Claimant,Location,Payment Status\n";
            foreach (var expense in Project.Expenses)
            {
                csvContent += $"{expense.ExpenseId},{expense.ProjectId},\"{expense.Description}\",{expense.Amount},\"{expense.Currency}\",\"{expense.Type}\",\"{expense.Date}\",\"{expense.PaymentMethod}\",\"{expense.Claimant}\",\"{expense.Location}\",\"{expense.PaymentStatus}\"\n";
            }

            var fileName = $"Project_{Project.ProjectId}_Expenses.csv";
            var filePath = Path.Combine(FileSystem.AppDataDirectory, fileName);
            await File.WriteAllTextAsync(filePath, csvContent);

            await Share.Default.RequestAsync(new ShareFileRequest
            {
                Title = "Export Expenses CSV",
                File = new ShareFile(filePath)
            });
        }
        catch (Exception ex)
        {
            await Shell.Current.CurrentPage.DisplayAlertAsync("Export Error", $"Failed to export: {ex.Message}", "OK");
        }
    }

    [RelayCommand]
    private async Task GoBackAsync()
    {
        await Shell.Current.GoToAsync("..");
    }

    [RelayCommand]
    private void ToggleFavorite()
    {
        if (Project == null)
            return;

        var favorites = LoadFavoriteIds();
        if (favorites.Contains(Project.ProjectId))
        {
            favorites.Remove(Project.ProjectId);
            Project.IsFavorite = false;
        }
        else
        {
            favorites.Add(Project.ProjectId);
            Project.IsFavorite = true;
        }

        Preferences.Default.Set(FavoritesPreferenceKey, string.Join(",", favorites.OrderBy(id => id)));
        OnPropertyChanged(nameof(Project));
        OnPropertyChanged(nameof(IsFavorite));
    }

    private HashSet<int> LoadFavoriteIds()
    {
        var raw = Preferences.Default.Get(FavoritesPreferenceKey, string.Empty);
        return raw.Split(',', StringSplitOptions.RemoveEmptyEntries)
            .Select(value => int.TryParse(value, out var id) ? id : 0)
            .Where(id => id > 0)
            .ToHashSet();
    }
}
