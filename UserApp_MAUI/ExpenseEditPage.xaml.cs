namespace ExpenseTrackerUser;

public partial class ExpenseEditPage : ContentPage
{
    public ExpenseEditPage(ExpenseEditViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = viewModel;
    }
}
