package expensemanagement.ui;

import expensemanagement.model.Expense;
import expensemanagement.model.User;
import expensemanagement.service.BudgetService;
import expensemanagement.service.ReportService;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/** Overview dashboard with current-month metrics, charts, and recent transactions. */
public class DashboardView extends VBox {
    private final MainApp app;
    private final User user;

    /** Creates the dashboard and loads its data asynchronously. */
    public DashboardView(MainApp app, User user) {
        this.app = app;
        this.user = user;
        getStyleClass().add("page-content");
        setPadding(new Insets(30));
        setSpacing(20);
        Label title = new Label("Good day, " + user.name());
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Here’s your financial snapshot for " + YearMonth.now().getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ".");
        subtitle.getStyleClass().add("muted-text");
        getChildren().addAll(title, subtitle);
        loadData();
    }

    private void loadData() {
        app.runAsync(() -> {
            YearMonth month = YearMonth.now();
            var expenses = app.reportService().monthlyExpenses(user, month);
            return new DashboardData(expenses, app.reportService().categoryTotals(expenses),
                    app.reportService().summary(expenses), app.reportService().lastSixMonths(user),
                    app.budgetService().remainingBudget(user, month.getMonthValue(), month.getYear()));
        }, data -> render(data));
    }

    private void render(DashboardData data) {
        HBox metrics = new HBox(16,
                metric("Spent this month", money(data.summary.total()), "Current month"),
                metric("Budget remaining", money(data.remaining), "Across planned budgets"),
                metric("Top category", data.summary.topCategory(), "Highest spending"),
                metric("Transactions", Integer.toString(data.summary.transactionCount()), "This month"));
        getChildren().add(metrics);
        PieChart pie = new PieChart();
        pie.setTitle("Spending by category");
        pie.setLegendVisible(true);
        pie.setPrefHeight(300);
        data.categories.forEach((name, amount) -> pie.getData().add(new PieChart.Data(name, amount.doubleValue())));
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount");
        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setTitle("Last 6 months");
        bar.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.months.forEach((month, amount) -> series.getData().add(new XYChart.Data<>(
                month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), amount.doubleValue())));
        bar.getData().add(series);
        GridPane charts = new GridPane();
        charts.setHgap(16);
        charts.add(card(pie), 0, 0);
        charts.add(card(bar), 1, 0);
        GridPane.setHgrow(charts.getChildren().get(0), Priority.ALWAYS);
        GridPane.setHgrow(charts.getChildren().get(1), Priority.ALWAYS);
        getChildren().add(charts);
        TableView<Expense> table = new TableView<>();
        TableColumn<Expense, String> expenseTitle = new TableColumn<>("Expense");
        expenseTitle.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().title()));
        TableColumn<Expense, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().categoryName()));
        TableColumn<Expense, java.time.LocalDate> date = new TableColumn<>("Date");
        date.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().expenseDate()));
        TableColumn<Expense, BigDecimal> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().amount()));
        table.getColumns().addAll(expenseTitle, category, date, amount);
        table.setItems(FXCollections.observableArrayList(data.expenses.stream().limit(6).toList()));
        VBox recent = card(new VBox(12, new Label("Recent expenses"), table));
        VBox.setVgrow(recent, Priority.ALWAYS);
        getChildren().add(recent);
    }

    private VBox metric(String heading, String value, String caption) {
        Label label = new Label(heading);
        label.getStyleClass().add("muted-text");
        Label amount = new Label(value);
        amount.getStyleClass().add("metric-value");
        Label detail = new Label(caption);
        detail.getStyleClass().add("small-muted");
        return card(new VBox(8, label, amount, detail));
    }

    private VBox card(javafx.scene.Node content) {
        VBox box = new VBox(content);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(20));
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String money(BigDecimal amount) {
        return "₹" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private record DashboardData(java.util.List<Expense> expenses, Map<String, BigDecimal> categories,
                                 ReportService.Summary summary, Map<YearMonth, BigDecimal> months,
                                 BigDecimal remaining) { }
}
