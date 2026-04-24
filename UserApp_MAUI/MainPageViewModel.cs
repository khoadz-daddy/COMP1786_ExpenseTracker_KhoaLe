using CommunityToolkit.Mvvm.Input;
using Microsoft.Maui.Storage;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;

namespace ExpenseTrackerUser;

public partial class MainPageViewModel : INotifyPropertyChanged
{
    private const string FavoritesPreferenceKey = "favorite_project_ids";
    private readonly FirebaseService _firebaseService;
    private const string FirebaseProjectsUrl = "https://expensetrackercloud-891dd-default-rtdb.firebaseio.com/Projects.json";
    private CancellationTokenSource? _searchCancellationTokenSource;
    private HashSet<int> _favoriteProjectIds = new();

    public event PropertyChangedEventHandler? PropertyChanged;

    private bool isBusy;
    public bool IsBusy
    {
        get => isBusy;
        set => SetProperty(ref isBusy, value);
    }

    private string statusMessage = string.Empty;
    public string StatusMessage
    {
        get => statusMessage;
        set => SetProperty(ref statusMessage, value);
    }

    private string searchText = string.Empty;
    public string SearchText
    {
        get => searchText;
        set 
        { 
            if (SetProperty(ref searchText, value))
            {
                // Debounce search - cancel previous search if any
                _searchCancellationTokenSource?.Cancel();
                _searchCancellationTokenSource = new CancellationTokenSource();
                
                var cts = _searchCancellationTokenSource;
                _ = Task.Delay(300, cts.Token).ContinueWith(async _ =>
                {
                    if (!cts.Token.IsCancellationRequested)
                    {
                        await FilterProjectsAsync(searchText);
                    }
                }, TaskScheduler.Default);
            }
        }
    }

    private string selectedStatusFilter = "All";
    public string SelectedStatusFilter
    {
        get => selectedStatusFilter;
        set => SetProperty(ref selectedStatusFilter, value);
    }

    private ObservableCollection<Project> projects = new();
    public ObservableCollection<Project> Projects
    {
        get => projects;
        set => SetProperty(ref projects, value);
    }

    private ObservableCollection<Project> filteredProjects = new();
    public ObservableCollection<Project> FilteredProjects
    {
        get => filteredProjects;
        set => SetProperty(ref filteredProjects, value);
    }

    private Project? selectedProject;
    public Project? SelectedProject
    {
        get => selectedProject;
        set => SetProperty(ref selectedProject, value);
    }

    private Color allButtonColor = Color.FromArgb("#4ADE80");
    public Color AllButtonColor
    {
        get => allButtonColor;
        set => SetProperty(ref allButtonColor, value);
    }

    private Color activeButtonColor = Color.FromArgb("#1E293B");
    public Color ActiveButtonColor
    {
        get => activeButtonColor;
        set => SetProperty(ref activeButtonColor, value);
    }

    private Color atRiskButtonColor = Color.FromArgb("#1E293B");
    public Color AtRiskButtonColor
    {
        get => atRiskButtonColor;
        set => SetProperty(ref atRiskButtonColor, value);
    }

    private Color completedButtonColor = Color.FromArgb("#1E293B");
    public Color CompletedButtonColor
    {
        get => completedButtonColor;
        set => SetProperty(ref completedButtonColor, value);
    }

    private Color favoriteButtonColor = Color.FromArgb("#1E293B");
    public Color FavoriteButtonColor
    {
        get => favoriteButtonColor;
        set => SetProperty(ref favoriteButtonColor, value);
    }



    public MainPageViewModel(FirebaseService firebaseService)
    {
        _firebaseService = firebaseService;
        LoadFavoriteIds();
    }

    protected bool SetProperty<T>(ref T field, T value, [CallerMemberName] string? propertyName = null)
    {
        if (EqualityComparer<T>.Default.Equals(field, value))
            return false;
        field = value;
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        return true;
    }

    private void UpdateButtonColors()
    {
        AllButtonColor = SelectedStatusFilter == "All" ? Color.FromArgb("#4ADE80") : Color.FromArgb("#1E293B");
        ActiveButtonColor = SelectedStatusFilter == "Active" ? Color.FromArgb("#4ADE80") : Color.FromArgb("#1E293B");
        AtRiskButtonColor = SelectedStatusFilter == "At Risk" ? Color.FromArgb("#4ADE80") : Color.FromArgb("#1E293B");
        CompletedButtonColor = SelectedStatusFilter == "Completed" ? Color.FromArgb("#4ADE80") : Color.FromArgb("#1E293B");
        FavoriteButtonColor = SelectedStatusFilter == "Favorites" ? Color.FromArgb("#4ADE80") : Color.FromArgb("#1E293B");
    }

    [RelayCommand]
    private void Search()
    {
        FilterProjects(SearchText);
    }

    /// <summary>
    /// Chuẩn hóa status: lowercase, trim, thay _ và - bằng dấu cách
    /// </summary>
    private string NormalizeStatus(string? status)
    {
        return (status ?? string.Empty)
            .Trim()
            .ToLowerInvariant()
            .Replace("_", " ")
            .Replace("-", " ");
    }

    /// <summary>
    /// Kiểm tra xem project có phải "At Risk" hay không:
    /// - Status chứa từ "risk" (sau normalize)
    /// - HOẶC RiskAssessment chứa "high", "yes", hoặc "risk"
    /// - HOẶC TotalSpent > Budget
    /// </summary>
    private bool IsAtRisk(Project project)
    {
        var normalizedStatus = NormalizeStatus(project.Status);
        var riskAssessment = (project.RiskAssessment ?? string.Empty)
            .Trim()
            .ToLowerInvariant();

        // Điều kiện 1: Status chứa "risk"
        bool statusHasRisk = normalizedStatus.Contains("risk");

        // Điều kiện 2: RiskAssessment chứa "high", "yes", hoặc "risk"
        bool riskAssessmentHasRisk = riskAssessment.Contains("high") 
                                    || riskAssessment.Contains("yes") 
                                    || riskAssessment.Contains("risk");

        // Điều kiện 3: Over budget
        bool isOverBudget = project.TotalSpent > project.Budget;

        return statusHasRisk || riskAssessmentHasRisk || isOverBudget;
    }

    /// <summary>
    /// Kiểm tra xem project có phải "Active" hay không:
    /// Status khớp exact với: "active", "in progress", "on track"
    /// </summary>
    private bool IsActive(Project project)
    {
        var normalizedStatus = NormalizeStatus(project.Status);
        return normalizedStatus == "active"
            || normalizedStatus == "in progress"
            || normalizedStatus == "on track";
    }

    /// <summary>
    /// Kiểm tra xem project có phải "Completed" hay không:
    /// - Status khớp exact với: "completed", "complete", "done", "finished", "closed"
    /// - VÀ KHÔNG phải "At Risk" (không được lố ngân sách)
    /// </summary>
    private bool IsCompleted(Project project)
    {
        // Nếu là "At Risk" thì không coi là "Completed"
        if (IsAtRisk(project))
            return false;

        var normalizedStatus = NormalizeStatus(project.Status);
        return normalizedStatus == "completed"
            || normalizedStatus == "complete"
            || normalizedStatus == "done"
            || normalizedStatus == "finished"
            || normalizedStatus == "closed";
    }

    private void FilterProjects(string value)
    {
        if (Projects == null) return;

        // Fire-and-forget on the thread pool; exceptions are logged internally.
        _ = FilterProjectsAsync(value);
    }

    private async Task FilterProjectsAsync(string value)
    {
        try
        {
            IEnumerable<Project> filtered = Projects!.ToList();

            // LAYER 1: Apply search filter (full-text search across all fields)
            if (!string.IsNullOrWhiteSpace(value))
            {
                var lowered = value.Trim().ToLowerInvariant();
                filtered = filtered.Where(p => 
                    (p.Name ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.Description ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.RiskAssessment ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.Status ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.Manager ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.Date ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.StartDate ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.EndDate ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                    || (p.Expenses?.Any(e => (e.Description ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                        || (e.Type ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)
                        || (e.PaymentStatus ?? string.Empty).Contains(lowered, StringComparison.OrdinalIgnoreCase)) ?? false));
            }

            // LAYER 2: Apply status filter based on strict rules
            if (SelectedStatusFilter != "All")
            {
                filtered = SelectedStatusFilter switch
                {
                    "Favorites" => filtered.Where(p => p.IsFavorite),
                    // ACTIVE: Status khớp exact với "active", "in progress", hoặc "on track"
                    "Active" => filtered.Where(p => IsActive(p)),

                    // AT RISK: Thỏa mãn 1 trong 3 điều kiện:
                    // 1. Status chứa "risk" (sau chuẩn hóa)
                    // 2. RiskAssessment chứa "high", "yes", hoặc "risk"
                    // 3. TotalSpent > Budget (lố ngân sách)
                    "At Risk" => filtered.Where(p => IsAtRisk(p)),

                    // COMPLETED: Status khớp exact với các từ hoàn thành
                    // VÀ phải KHÔNG phải "At Risk" (không lố ngân sách)
                    "Completed" => filtered.Where(p => IsCompleted(p)),

                    // Fallback: Không được định nghĩa, sử dụng exact match
                    _ => filtered.Where(p =>
                        string.Equals(p.Status ?? string.Empty, SelectedStatusFilter, StringComparison.OrdinalIgnoreCase))
                };
            }

            var result = new ObservableCollection<Project>(filtered);

            // Update UI on main thread
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                FilteredProjects = result;
            });
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Filter error: {ex.Message}");
        }
    }

    [RelayCommand]
    public async Task LoadDataAsync()
    {
        if (IsBusy)
            return;
        await FetchDataAsync();
    }

    private async Task FetchDataAsync()
    {
        try
        {
            if (FirebaseProjectsUrl.Contains("<YOUR_PROJECT_ID>", StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidOperationException("Firebase URL is not configured.");
            }

            IsBusy = true;
            StatusMessage = "Loading data...";

            // Fetch entirely on background thread
            var source = await Task.Run(() => _firebaseService.GetProjectsAsync(FirebaseProjectsUrl));

            var sorted = source.OrderByDescending(p =>
            {
                ApplyFavoriteState(p);
                return p.IsFavorite ? 1 : 0;
            }).ThenByDescending(p =>
            {
                var dateStr = p.StartDate ?? p.EndDate ?? p.Date;
                return !string.IsNullOrWhiteSpace(dateStr) ? dateStr : "0000-01-01";
            }).ToList();

            // Switch to UI thread only once for all updates, then re-apply current filter
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                Projects = new ObservableCollection<Project>(sorted);
                StatusMessage = Projects.Any()
                    ? $"{Projects.Count} projects loaded."
                    : "No projects found in Firebase.";

                // Re-apply whatever filter was active before refresh
                await FilterProjectsAsync(SearchText);
            });
        }
        catch (Exception ex)
        {
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                StatusMessage = (ex is HttpRequestException || ex is TaskCanceledException)
                    ? "Offline: Unable to connect. Check your internet."
                    : $"Error: {ex.Message}";
            });
        }
        finally
        {
            // Always reset IsBusy on UI thread
            await MainThread.InvokeOnMainThreadAsync(() => IsBusy = false);
        }
    }

    [RelayCommand]
    private void FilterByStatus(string status)
    {
        SelectedStatusFilter = status;
        UpdateButtonColors();
        FilterProjects(SearchText);
    }

    [RelayCommand]
    private void ToggleFavorite(Project? project)
    {
        if (project == null)
            return;

        if (_favoriteProjectIds.Contains(project.ProjectId))
        {
            _favoriteProjectIds.Remove(project.ProjectId);
            project.IsFavorite = false;
        }
        else
        {
            _favoriteProjectIds.Add(project.ProjectId);
            project.IsFavorite = true;
        }

        SaveFavoriteIds();
        Projects = new ObservableCollection<Project>(Projects
            .OrderByDescending(p => p.IsFavorite)
            .ThenBy(p => p.Name ?? string.Empty));
        FilterProjects(SearchText);
    }

    [RelayCommand]
    private async Task NavigateToDetailsAsync()
    {
        if (SelectedProject != null)
        {
            try
            {
                var project = SelectedProject;
                SelectedProject = null; // Reset selection immediately
                
                var navigationParameter = new Dictionary<string, object>
                {
                    { "Project", project }
                };
                
                await MainThread.InvokeOnMainThreadAsync(async () =>
                {
                    await Shell.Current.GoToAsync("ProjectDetailsPage", true, navigationParameter);
                });
            }
            catch (Exception ex)
            {
                StatusMessage = $"Navigation error: {ex.Message}";
            }
        }
    }

    [RelayCommand]
    public async Task RefreshAsync()
    {
        // Always fetch — do NOT skip when IsBusy because RefreshView
        // binds IsRefreshing to IsBusy; if we return early IsBusy stays
        // true and the spinner freezes forever.
        await FetchDataAsync();
    }

    private void LoadFavoriteIds()
    {
        var raw = Preferences.Default.Get(FavoritesPreferenceKey, string.Empty);
        _favoriteProjectIds = raw.Split(',', StringSplitOptions.RemoveEmptyEntries)
            .Select(value => int.TryParse(value, out var id) ? id : 0)
            .Where(id => id > 0)
            .ToHashSet();
    }

    private void SaveFavoriteIds()
    {
        Preferences.Default.Set(FavoritesPreferenceKey, string.Join(",", _favoriteProjectIds.OrderBy(id => id)));
    }

    private void ApplyFavoriteState(Project project)
    {
        project.IsFavorite = _favoriteProjectIds.Contains(project.ProjectId);
    }
}
