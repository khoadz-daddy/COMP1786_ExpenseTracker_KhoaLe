namespace ExpenseTrackerUser;

public partial class AppShell : Shell
{
	public AppShell()
	{
		InitializeComponent();
		Routing.RegisterRoute("ProjectDetailsPage", typeof(ProjectDetailsPage));
		Routing.RegisterRoute("ExpenseEditPage", typeof(ExpenseEditPage));
	}
}
