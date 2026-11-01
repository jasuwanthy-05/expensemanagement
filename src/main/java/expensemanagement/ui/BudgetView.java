package expensemanagement.ui;

import expensemanagement.model.Category;
import expensemanagement.model.User;
import expensemanagement.service.BudgetService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Monthly budget creation and category usage screen. */
public class BudgetView extends VBox {
    private final MainApp app;
    private final User user;
    private final VBox rows = new VBox(14);
    private final Spinner<Integer> month = new Spinner<>(1, 12, LocalDate.now().getMonthValue());
    private final Spinner<Integer> year = new Spinner<>(2000, 9999, LocalDate.now().getYear());

    /** Creates the budget screen and loads current-month budgets. */
    public BudgetView(MainApp app, User user) {
        this.app = app;
        this.user = user;
        getStyleClass().add("page-content");
        setPadding(new Insets(30));
        setSpacing(18);
        Label title = new Label("Budgets");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Plan category limits and keep spending on track.");
        subtitle.getStyleClass().add("muted-text");
        Button set = new Button("+ Set category budget");
        set.getStyleClass().add("primary-button");
        set.setOnAction(event -> openBudgetDialog());
        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(event -> load());
        HBox controls = new HBox(10, new Label("Month"), month, new Label("Year"), year, refresh, set);
        controls.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(title, subtitle, controls, rows);
        load();
    }

    private void load() {
        int selectedMonth = month.getValue();
        int selectedYear = year.getValue();
        app.runAsync(() -> app.budgetService().statuses(user, selectedMonth, selectedYear),
                statuses -> render(statuses));
    }

    private void render(List<BudgetService.BudgetStatus> statuses) {
        rows.getChildren().clear();
        if (statuses.isEmpty()) {
            Label empty = new Label("No budgets set for this month. Add a category limit to get started.");
            empty.getStyleClass().add("muted-text");
            rows.getChildren().add(empty);
            return;
        }
        for (BudgetService.BudgetStatus status : statuses) {
            Label name = new Label(status.categoryName());
            name.getStyleClass().add("section-title");
            Label amounts = new Label("₹" + status.spent().toPlainString() + " spent of ₹"
                    + status.budget().monthlyLimit().toPlainString());
            Label percent = new Label(status.percentage().toPlainString() + "%");
            percent.getStyleClass().add("small-muted");
            ProgressBar progress = new ProgressBar(Math.min(status.percentage().doubleValue() / 100, 1));
            progress.setMaxWidth(Double.MAX_VALUE);
            progress.getStyleClass().add("budget-progress");
            switch (status.alertLevel()) {
                case OK -> progress.getStyleClass().add("progress-ok");
                case WARNING -> progress.getStyleClass().add("progress-warning");
                case OVER -> progress.getStyleClass().add("progress-over");
            }
            Label alert = new Label(switch (status.alertLevel()) {
                case OK -> "On track";
                case WARNING -> "You've reached 80% of this budget.";
                case OVER -> "Budget limit reached or exceeded.";
            });
            alert.getStyleClass().add(switch (status.alertLevel()) {
                case OK -> "status-good";
                case WARNING -> "status-warning";
                case OVER -> "status-danger";
            });
            Button delete = new Button("Remove");
            delete.getStyleClass().add("danger-button");
            delete.setOnAction(event -> {
                if (app.confirm("Remove budget", "Remove the " + status.categoryName() + " budget?")) {
                    app.runAsync(() -> {
                        app.budgetService().deleteBudget(user, status.budget().categoryId(),
                                selectedMonth(), selectedYear());
                        return null;
                    }, ignored -> load());
                }
            });
            HBox heading = new HBox(12, name, amounts, percent);
            heading.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(amounts, Priority.ALWAYS);
            HBox bottom = new HBox(12, alert, delete);
            bottom.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(alert, Priority.ALWAYS);
            VBox card = new VBox(12, heading, progress, bottom);
            card.getStyleClass().add("card");
            card.setPadding(new Insets(20));
            rows.getChildren().add(card);
        }
    }

    private int selectedMonth() { return month.getValue(); }
    private int selectedYear() { return year.getValue(); }

    private void openBudgetDialog() {
        app.runAsync(() -> app.categoryService().findAll(user), categories -> {
            if (categories.isEmpty()) {
                app.showInfo("Create a category before setting a budget.");
                return;
            }
            Dialog<BudgetForm> dialog = new Dialog<>();
            dialog.setTitle("Set monthly budget");
            ButtonType save = new ButtonType("Save budget", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
            ComboBox<Category> category = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(categories));
            category.setValue(categories.get(0));
            category.setCellFactory(view -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(Category item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.name());
                }
            });
            category.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(Category item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.name());
                }
            });
            TextField limit = new TextField();
            limit.setPromptText("Monthly limit, e.g. 5000.00");
            Spinner<Integer> budgetMonth = new Spinner<>(1, 12, month.getValue());
            Spinner<Integer> budgetYear = new Spinner<>(2000, 9999, year.getValue());
            VBox form = new VBox(10, new Label("Category"), category, new Label("Monthly limit"), limit,
                    new Label("Month number"), budgetMonth, new Label("Year"), budgetYear);
            form.setPadding(new Insets(12));
            dialog.getDialogPane().setContent(form);
            dialog.setResultConverter(button -> button == save ? new BudgetForm(category.getValue(), limit.getText(),
                    budgetMonth.getValue(), budgetYear.getValue()) : null);
            dialog.showAndWait().ifPresent(value -> app.runAsync(() -> {
                app.budgetService().setBudget(user, value.category.id(), new BigDecimal(value.limit.trim()),
                        value.month, value.year);
                return null;
            }, ignored -> {
                month.getValueFactory().setValue(value.month);
                year.getValueFactory().setValue(value.year);
                load();
            }));
        });
    }

    private record BudgetForm(Category category, String limit, int month, int year) { }
}
