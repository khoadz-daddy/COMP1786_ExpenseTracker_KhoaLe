using System.Net.Http;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace ExpenseTrackerUser;

public class FirebaseService
{
    private readonly HttpClient _httpClient;

    public FirebaseService(HttpClient httpClient)
    {
        _httpClient = httpClient;
        // Set timeout to prevent indefinite hanging
        _httpClient.Timeout = TimeSpan.FromSeconds(30);
    }

    public async Task<IList<Project>> GetProjectsAsync(string firebaseUrl)
    {
        if (!firebaseUrl.EndsWith(".json", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("Firebase URL must end with .json, for example: https://your-db.firebaseio.com/Projects.json");
        }

        using var response = await _httpClient.GetAsync(firebaseUrl);
        response.EnsureSuccessStatusCode();

        var content = await response.Content.ReadAsStringAsync();
        if (string.IsNullOrWhiteSpace(content) || content.Trim().Equals("null", StringComparison.OrdinalIgnoreCase))
        {
            return Array.Empty<Project>();
        }

        var options = new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true,
            ReadCommentHandling = JsonCommentHandling.Skip,
            AllowTrailingCommas = true
        };

        using var document = JsonDocument.Parse(content);
        if (document.RootElement.ValueKind == JsonValueKind.Object)
        {
            // Check if the root has a nested "Projects" key
            if (document.RootElement.TryGetProperty("Projects", out var projectsElement) && projectsElement.ValueKind == JsonValueKind.Object)
            {
                var nested = projectsElement.Deserialize<Dictionary<string, Project>>(options);
                if (nested?.Count > 0)
                {
                    return NormalizeProjects(nested.Values.ToList());
                }
            }

            // Try deserializing the root as a dictionary of projects
            var rootDictionary = document.RootElement.Deserialize<Dictionary<string, Project>>(options);
            if (rootDictionary?.Count > 0)
            {
                return NormalizeProjects(rootDictionary.Values.ToList());
            }
        }

        if (document.RootElement.ValueKind == JsonValueKind.Array)
        {
            var list = document.RootElement.Deserialize<List<Project>>(options);
            if (list?.Count > 0)
            {
                return NormalizeProjects(list);
            }
        }

        return Array.Empty<Project>();
    }

    private static List<Project> NormalizeProjects(List<Project> projects)
    {
        foreach (var project in projects)
        {
            project.Name ??= $"Project {project.ProjectId}";
            project.Description ??= "No description";
            project.Status ??= "Unknown";
        }
        return projects;
    }

    public async Task<List<Expense>> ModifyExpenseAsync(string firebaseUrl, int projectId, Expense? expenseToUpdate, int? expenseIdToDelete)
    {
        using var response = await _httpClient.GetAsync(firebaseUrl);
        response.EnsureSuccessStatusCode();
        var content = await response.Content.ReadAsStringAsync();
        
        using var document = JsonDocument.Parse(content);
        string? projectKey = null;
        string? prefix = "";
        JsonElement projectElement = default;

        if (document.RootElement.ValueKind == JsonValueKind.Object)
        {
            if (document.RootElement.TryGetProperty("Projects", out var projectsElement) && projectsElement.ValueKind == JsonValueKind.Object)
            {
                foreach (var prop in projectsElement.EnumerateObject())
                {
                    if (prop.Value.TryGetProperty("projectId", out var idProp) && 
                        ((idProp.ValueKind == JsonValueKind.Number && idProp.GetInt32() == projectId) || 
                         (idProp.ValueKind == JsonValueKind.String && int.TryParse(idProp.GetString(), out int id) && id == projectId)))
                    {
                        projectKey = prop.Name;
                        prefix = "Projects/";
                        projectElement = prop.Value;
                        break;
                    }
                }
            }
            
            if (projectKey == null)
            {
                foreach (var prop in document.RootElement.EnumerateObject())
                {
                    if (prop.Value.TryGetProperty("projectId", out var idProp) && 
                        ((idProp.ValueKind == JsonValueKind.Number && idProp.GetInt32() == projectId) || 
                         (idProp.ValueKind == JsonValueKind.String && int.TryParse(idProp.GetString(), out int id) && id == projectId)))
                    {
                        projectKey = prop.Name;
                        prefix = "";
                        projectElement = prop.Value;
                        break;
                    }
                }
            }
        }
        else if (document.RootElement.ValueKind == JsonValueKind.Array)
        {
            int index = 0;
            foreach (var item in document.RootElement.EnumerateArray())
            {
                if (item.TryGetProperty("projectId", out var idProp) && 
                    ((idProp.ValueKind == JsonValueKind.Number && idProp.GetInt32() == projectId) || 
                     (idProp.ValueKind == JsonValueKind.String && int.TryParse(idProp.GetString(), out int id) && id == projectId)))
                {
                    projectKey = index.ToString();
                    prefix = "";
                    projectElement = item;
                    break;
                }
                index++;
            }
        }

        if (projectKey == null) throw new Exception($"Project with ID {projectId} not found in Firebase.");

        // Construct the URL for the specific project
        var baseUrl = firebaseUrl.Substring(0, firebaseUrl.LastIndexOf(".json"));
        var projectUrl = $"{baseUrl}/{prefix}{projectKey}.json";

        // Implement Firebase Transaction using ETag (Optimistic Concurrency Control)
        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++)
        {
            // 1. GET current project with ETag
            using var getRequest = new HttpRequestMessage(HttpMethod.Get, projectUrl);
            getRequest.Headers.Add("X-Firebase-ETag", "true");
            
            using var getResponse = await _httpClient.SendAsync(getRequest);
            getResponse.EnsureSuccessStatusCode();
            
            string? eTag = null;
            if (getResponse.Headers.TryGetValues("ETag", out var eTags))
            {
                eTag = eTags.FirstOrDefault();
            }
            
            var projectContent = await getResponse.Content.ReadAsStringAsync();
            var projectNode = JsonNode.Parse(projectContent) as JsonObject;
            if (projectNode == null) throw new Exception("Invalid project data");
            
            List<Expense> currentExpenses = new List<Expense>();
            
            if (projectNode.TryGetPropertyValue("expenses", out var expensesNode) && expensesNode is JsonArray)
            {
                var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
                currentExpenses = expensesNode.Deserialize<List<Expense>>(options) ?? new List<Expense>();
            }

            // 2. Apply mutation
            if (expenseIdToDelete.HasValue)
            {
                currentExpenses.RemoveAll(e => e.ExpenseId == expenseIdToDelete.Value);
            }
            
            if (expenseToUpdate != null)
            {
                var existing = currentExpenses.FirstOrDefault(e => e.ExpenseId == expenseToUpdate.ExpenseId);
                if (existing != null)
                {
                    existing.Description = expenseToUpdate.Description;
                    existing.Amount = expenseToUpdate.Amount;
                    existing.Type = expenseToUpdate.Type;
                    existing.Date = expenseToUpdate.Date;
                    existing.PaymentStatus = expenseToUpdate.PaymentStatus;
                    existing.Currency = expenseToUpdate.Currency;
                    existing.PaymentMethod = expenseToUpdate.PaymentMethod;
                    existing.Claimant = expenseToUpdate.Claimant;
                    existing.Location = expenseToUpdate.Location;
                }
                else
                {
                    if (expenseToUpdate.ExpenseId <= 0)
                    {
                        expenseToUpdate.ExpenseId = currentExpenses.Any() ? currentExpenses.Max(e => e.ExpenseId) + 1 : 1;
                    }
                    currentExpenses.Add(expenseToUpdate);
                }
            }

            // Calculate TotalSpent and UsagePercentage
            double newTotalSpent = currentExpenses.Sum(e => e.Amount);
            double budget = 0;
            if (projectNode.TryGetPropertyValue("budget", out var budgetNode) && budgetNode != null)
            {
                budget = budgetNode.GetValue<double>();
            }
            
            double newUsagePercentage = budget > 0 ? (newTotalSpent / budget) * 100 : 0;
            long roundedUsage = (long)Math.Round(newUsagePercentage);

            // Update the JSON node directly
            var optionsOut = new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase };
            projectNode["expenses"] = JsonSerializer.SerializeToNode(currentExpenses, optionsOut);
            projectNode["totalSpent"] = newTotalSpent;
            projectNode["usagePercentage"] = roundedUsage;

            // 3. PUT with if-match ETag
            var json = projectNode.ToJsonString();
            
            using var putRequest = new HttpRequestMessage(HttpMethod.Put, projectUrl);
            putRequest.Content = new StringContent(json, System.Text.Encoding.UTF8, "application/json");
            if (!string.IsNullOrEmpty(eTag))
            {
                // Firebase REST API requires the exact raw ETag WITHOUT quotes. 
                eTag = eTag.Replace("\"", "");
                putRequest.Headers.TryAddWithoutValidation("if-match", eTag);
            }

            using var putResponse = await _httpClient.SendAsync(putRequest);
            
            if (putResponse.IsSuccessStatusCode)
            {
                return currentExpenses; // Transaction successful
            }
            else if (putResponse.StatusCode == System.Net.HttpStatusCode.PreconditionFailed)
            {
                // ETag mismatched, retry the loop
                continue;
            }
            else
            {
                putResponse.EnsureSuccessStatusCode();
            }
        }

        throw new Exception("Thất bại khi lưu chi tiêu sau nhiều lần thử lại do có người khác đang cùng chỉnh sửa (Xung đột dữ liệu).");
    }
}
