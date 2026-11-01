package expensemanagement.ui;

import expensemanagement.model.Expense;
import expensemanagement.model.User;
import expensemanagement.service.ReportService;
import expensemanagement.util.CSVExporter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Monthly and yearly report summaries with CSV export. */
public class ReportView extends javafx.scene.layout.VBox {
    private final MainApp app;
    private final User user;
    private final TableView<CategoryTotal> table = new TableView<>();
    private final Label total = new Label("₹0.00");
    private final Label count = new Label("0");
    private final Label top = new Label("—");
    private final TextField yearField = new TextField(Integer.toString(java.time.Year.now().getValue()));
    private final ComboBox<String> month = new ComboBox<>();
    private final ToggleButton monthly = new ToggleButton("Monthly");
    private final ToggleButton yearly = new ToggleButton("Yearly");
    private List<Expense> currentExpenses = List.of();

    /** Creates the reports screen and loads the current month. */
    public ReportView(MainApp app, User user) {
        this.app = app;
        this.user = user;
        getStyleClass().add("page-content");
        setPadding(new Insets(30));
        setSpacing(18);
        Label title = new Label("Reports");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Understand where your money goes.");
        subtitle.getStyleClass().add("muted-text");
        ToggleGroup period = new ToggleGroup();
        monthly.setToggleGroup(period);
        yearly.setToggleGroup(period);
        monthly.setSelected(true);
        month.getItems().setAll("January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December");
        month.getSelectionModel().select(java.time.LocalDate.now().getMonthValue() - 1);
        yearField.setPrefWidth(100);
        Button run = new Button("Generate report");
        run.getStyleClass().add("primary-button");
        run.setOnAction(event -> load());
        Button export = new Button("Export CSV");
        export.getStyleClass().add("secondary-button");
        export.setOnAction(event -> export());
        HBox controls = new HBox(10, monthly, yearly, month, yearField, run, export);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox metrics = new HBox(20, metric("Total spent", total), metric("Transactions", count),
                metric("Top category", top));
        configureTable();
        getChildren().addAll(title, subtitle, controls, metrics, table);
        load();
    }

    private javafx.scene.layout.VBox metric(String title, Label value) {
        Label heading = new Label(title);
        heading.getStyleClass().add("muted-text");
        value.getStyleClass().add("metric-value");
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(8, heading, value);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        return card;
    }

    private void configureTable() {
        table.getStyleClass().add("data-table");
        TableColumn<CategoryTotal, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(value -> new javafx.beans.property.SimpleStringProperty(value.getValue().category));
        TableColumn<CategoryTotal, BigDecimal> amount = new TableColumn<>("Total");
        amount.setCellValueFactory(value -> new javafx.beans.property.SimpleObjectProperty<>(value.getValue().amount));
        table.getColumns().setAll(category, amount);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No report data for this period."));
    }

    private void load() {
        app.runAsync(() -> {
            int year = parseYear();
            List<Expense> expenses;
            if (yearly.isSelected()) {
                expenses = app.reportService().yearlyExpenses(user, year);
            } else {
                expenses = app.reportService().monthlyExpenses(user,
                        YearMonth.of(year, month.getSelectionModel().getSelectedIndex() + 1));
            }
            return new ReportData(expenses, app.reportService().summary(expenses),
                    app.reportService().categoryTotals(expenses));
        }, data -> {
            currentExpenses = data.expenses;
            total.setText("₹" + data.summary.total().toPlainString());
            count.setText(Integer.toString(data.summary.transactionCount()));
            top.setText(data.summary.topCategory());
            table.setItems(FXCollections.observableArrayList(data.categories.entrySet().stream()
                    .map(entry -> new CategoryTotal(entry.getKey(), entry.getValue())).toList()));
        });
    }

    private int parseYear() {
        try {
            int value = Integer.parseInt(yearField.getText().trim());
            if (value < 2000 || value > 9999) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a year between 2000 and 9999.");
        }
    }

    private void export() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export expense report");
        chooser.setInitialFileName("expense-report.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        java.io.File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            app.runAsync(() -> {
                CSVExporter.exportExpenses(file.toPath(), currentExpenses);
                return null;
            }, ignored -> app.showInfo("Report exported to " + file.getAbsolutePath()));
        }
    }

    private record CategoryTotal(String category, BigDecimal amount) { }
    private record ReportData(List<Expense> expenses, ReportService.Summary summary,
                              Map<String, BigDecimal> categories) { }
}
