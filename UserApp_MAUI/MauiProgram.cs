using Microsoft.Extensions.Logging;
using CommunityToolkit.Maui;

namespace ExpenseTrackerUser;

public static class MauiProgram
{
	public static MauiApp CreateMauiApp()
	{
		var builder = MauiApp.CreateBuilder();
		builder
			.UseMauiApp<App>()
			.UseMauiCommunityToolkit()
			.ConfigureFonts(fonts =>
			{
				fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
				fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
			});

#if DEBUG
		builder.Logging.AddDebug();
#endif

		builder.Services.AddSingleton<HttpClient>();
		builder.Services.AddSingleton<FirebaseService>();
		
		builder.Services.AddTransient<MainPageViewModel>();
		builder.Services.AddTransient<MainPage>();

		builder.Services.AddTransient<ProjectDetailsViewModel>();
		builder.Services.AddTransient<ProjectDetailsPage>();

		builder.Services.AddTransient<ExpenseEditViewModel>();
		builder.Services.AddTransient<ExpenseEditPage>();

		return builder.Build();
	}
}
