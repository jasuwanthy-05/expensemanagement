package expensemanagement.ui;

import expensemanagement.exception.ValidationException;
import expensemanagement.model.Category;
import expensemanagement.model.Expense;
import expensemanagement.model.RecurringExpense;
import expensemanagement.model.User;
import expensemanagement.util.InputHelper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Search, create, edit, delete, and recurring expense management screen. */
public class ExpenseView extends VBox {
    private final MainApp app;
    private final User user;
    private final TableView<Expense> table = new TableView<>();
    private final TextField keyword = new TextField();
    private final TextField minimum = new TextField();
    private final TextField maximum = new TextField();
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final ComboBox<Category> categoryFilter = new ComboBox<>();

    /** Creates the expense screen and loads the user's data. */
    public ExpenseView(MainApp app, User user) {
        this.app = app;
        this.user = user;
        getStyleClass().add("page-content");
        setPadding(new Insets(30));
        setSpacing(18);
        Label title = new Label("Expenses");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Search, review, and manage every transaction.");
        subtitle.getStyleClass().add("muted-text");
        getChildren().addAll(title, subtitle, buildToolbar(), buildFilters(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        configureTable();
        loadCategories();
        search();
    }

    private HBox buildToolbar() {
        Button add = new Button("+ Add expense");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> openExpenseDialog(null));
        Button recurring = new Button("Recurring");
        recurring.getStyleClass().add("secondary-button");
        recurring.setOnAction(event -> openRecurring());
        Button categories = new Button("Manage categories");
        categories.getStyleClass().add("secondary-button");
        categories.setOnAction(event -> manageCategories());
        HBox toolbar = new HBox(10, add, recurring, categories);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private VBox buildFilters() {
        keyword.setPromptText("Search title or notes");
        minimum.setPromptText("Min amount");
        minimum.setPrefWidth(110);
        maximum.setPromptText("Max amount");
        maximum.setPrefWidth(110);
        from.setPromptText("From date");
        to.setPromptText("To date");
        categoryFilter.setPromptText("All categories");
        categoryFilter.setPrefWidth(160);
        Button search = new Button("Apply filters");
        search.getStyleClass().add("secondary-button");
        search.setOnAction(event -> search());
        Button clear = new Button("Clear");
        clear.getStyleClass().add("link-button");
        clear.setOnAction(event -> {
            keyword.clear();
            minimum.clear();
            maximum.clear();
            from.setValue(null);
            to.setValue(null);
            categoryFilter.setValue(null);
            search();
        });
        return new VBox(10, new HBox(10, keyword, categoryFilter, from, to),
                new HBox(10, minimum, maximum, search, clear));
    }

    private void configureTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Expense, String> title = new TableColumn<>("Expense");
        title.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().title()));
        TableColumn<Expense, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().categoryName()));
        TableColumn<Expense, LocalDate> date = new TableColumn<>("Date");
        date.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().expenseDate()));
        TableColumn<Expense, BigDecimal> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().amount()));
        TableColumn<Expense, String> notes = new TableColumn<>("Notes");
        notes.setCellValueFactory(value -> new SimpleStringProperty(
                value.getValue().notes() == null ? "" : value.getValue().notes()));
        title.setPrefWidth(220);
        category.setPrefWidth(150);
        date.setPrefWidth(130);
        amount.setPrefWidth(120);
        notes.setPrefWidth(240);
        table.getColumns().setAll(title, category, date, amount, notes);
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                openExpenseDialog(table.getSelectionModel().getSelectedItem());
            }
        });
        Button edit = new Button("Edit selected");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(event -> {
            if (table.getSelectionModel().getSelectedItem() != null) {
                openExpenseDialog(table.getSelectionModel().getSelectedItem());
            } else {
                app.showInfo("Select an expense to edit.");
            }
        });
        Button delete = new Button("Delete selected");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteSelected());
        table.setPlaceholder(new Label("No expenses match these filters."));
        getChildren().add(new HBox(10, edit, delete));
    }

    private void loadCategories() {
        app.runAsync(() -> app.categoryService().findAll(user), categories -> {
            categoryFilter.getItems().setAll(categories);
            installCategoryCells(categoryFilter);
        });
    }

    private void search() {
        app.runAsync(() -> app.expenseService().search(user, keyword.getText(),
                categoryFilter.getValue() == null ? null : categoryFilter.getValue().id(),
                from.getValue(), to.getValue(), parseOptional(minimum.getText(), "Minimum amount"),
                parseOptional(maximum.getText(), "Maximum amount")),
                results -> table.setItems(FXCollections.observableArrayList(results)));
    }

    private BigDecimal parseOptional(String text, String label) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(text.trim());
            return InputHelper.positiveMoney(amount, label);
        } catch (NumberFormatException ex) {
            throw new ValidationException(label + " must be a valid number.");
        }
    }

    private void openExpenseDialog(Expense existing) {
        app.runAsync(() -> app.categoryService().findAll(user), categories -> {
            if (categories.isEmpty()) {
                app.showInfo("Create a category before adding an expense.");
                return;
            }
            Dialog<ExpenseForm> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "Add expense" : "Edit expense");
            dialog.getDialogPane().getStyleClass().add("styled-dialog");
            ButtonType save = new ButtonType(existing == null ? "Add expense" : "Save changes",
                    ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
            TextField title = new TextField(existing == null ? "" : existing.title());
            title.setPromptText("What was this expense?");
            TextField amount = new TextField(existing == null ? "" : existing.amount().toPlainString());
            amount.setPromptText("0.00");
            ComboBox<Category> category = new ComboBox<>(FXCollections.observableArrayList(categories));
            installCategoryCells(category);
            category.setValue(existing == null ? categories.get(0) : categories.stream()
                    .filter(item -> item.id() == existing.categoryId()).findFirst().orElse(categories.get(0)));
            DatePicker date = new DatePicker(existing == null ? LocalDate.now() : existing.expenseDate());
            TextArea notes = new TextArea(existing == null || existing.notes() == null ? "" : existing.notes());
            notes.setPromptText("Optional notes");
            notes.setPrefRowCount(3);
            VBox form = new VBox(10, new Label("Title"), title, new Label("Amount"), amount,
                    new Label("Category"), category, new Label("Date"), date, new Label("Notes"), notes);
            form.setPadding(new Insets(12));
            dialog.getDialogPane().setContent(form);
            dialog.setResultConverter(button -> button == save ? new ExpenseForm(title.getText(), amount.getText(),
                    category.getValue(), date.getValue(), notes.getText()) : null);
            Optional<ExpenseForm> result = dialog.showAndWait();
            result.ifPresent(formValue -> app.runAsync(() -> {
                BigDecimal value = new BigDecimal(formValue.amount.trim());
                if (existing == null) {
                    app.expenseService().add(user, formValue.title, value, formValue.category.id(),
                            formValue.date, formValue.notes);
                } else {
                    app.expenseService().update(user, existing.id(), formValue.title, value,
                            formValue.category.id(), formValue.date, formValue.notes);
                }
                return null;
            }, ignored -> {
                search();
                checkBudgetAlerts();
            }));
        });
    }

    private void deleteSelected() {
        Expense selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            app.showInfo("Select an expense to delete.");
            return;
        }

        if (app.confirm("Delete expense", "Delete “" + selected.title() + "”? This cannot be undone.")) {
            app.runAsync(() -> {
                app.expenseService().delete(user, selected.id());
                return null;
            }, ignored -> search());
        }
    }

    private void checkBudgetAlerts() {
        LocalDate now = LocalDate.now();
        app.runAsync(() -> app.budgetService().statuses(user, now.getMonthValue(), now.getYear()), statuses -> {
            statuses.stream().filter(status -> status.alertLevel() != expensemanagement.service.BudgetService.AlertLevel.OK)
                    .forEach(status -> app.showInfo(status.categoryName() + ": " + status.percentage()
                            + "% of your monthly budget has been used."));
        });
    }

    private void openRecurring() {
        app.runAsync(() -> app.expenseService().findRecurring(user), this::showRecurringDialog);
    }

    private void showRecurringDialog(List<RecurringExpense> items) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Monthly recurring expenses");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox rows = new VBox(10);
        for (RecurringExpense item : items) {
            HBox row = new HBox(12, new Label(item.title() + " · ₹" + item.amount() + " · day " + item.dueDay()));
            row.setAlignment(Pos.CENTER_LEFT);
            Button remove = new Button("Remove");
            remove.getStyleClass().add("danger-button");
            remove.setOnAction(event -> {
                if (app.confirm("Remove recurring expense", "Remove “" + item.title() + "”?")) {
                    dialog.close();
                    app.runAsync(() -> {
                        app.expenseService().deleteRecurring(user, item.id());
                        return null;
                    }, ignored -> openRecurring());
                }
            });
            HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
            row.getChildren().add(remove);
            rows.getChildren().add(row);
        }
        if (items.isEmpty()) {
            rows.getChildren().add(new Label("No monthly recurring expenses configured."));
        }
        Button add = new Button("+ Add monthly bill");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> {
            dialog.close();
            openRecurringForm();
        });
        rows.getChildren().add(add);
        dialog.getDialogPane().setContent(rows);
        dialog.showAndWait();
    }

    private void openRecurringForm() {
        app.runAsync(() -> app.categoryService().findAll(user), categories -> {
            if (categories.isEmpty()) {
                app.showInfo("Create a category before adding a recurring expense.");
                return;
            }
            Dialog<RecurringForm> dialog = new Dialog<>();
            dialog.setTitle("Add monthly bill");
            ButtonType save = new ButtonType("Save recurring bill", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
            TextField title = new TextField();
            title.setPromptText("Bill name");
            TextField amount = new TextField();
            amount.setPromptText("Monthly amount");
            ComboBox<Category> category = new ComboBox<>(FXCollections.observableArrayList(categories));
            installCategoryCells(category);
            category.setValue(categories.get(0));
            Spinner<Integer> dueDay = new Spinner<>(1, 31, 1);
            TextArea notes = new TextArea();
            notes.setPromptText("Optional notes");
            VBox form = new VBox(10, new Label("Title"), title, new Label("Amount"), amount,
                    new Label("Category"), category, new Label("Due day"), dueDay, notes);
            form.setPadding(new Insets(12));
            dialog.getDialogPane().setContent(form);
            dialog.setResultConverter(button -> button == save ? new RecurringForm(title.getText(), amount.getText(),
                    category.getValue(), dueDay.getValue(), notes.getText()) : null);
            dialog.showAndWait().ifPresent(value -> app.runAsync(() -> {
                app.expenseService().addRecurring(user, value.title, new BigDecimal(value.amount.trim()),
                        value.category.id(), value.dueDay, value.notes);
                return null;
            }, ignored -> openRecurring()));
        });
    }

    private void manageCategories() {
        app.runAsync(() -> app.categoryService().findAll(user), this::showCategoryDialog);
    }

    private void showCategoryDialog(List<Category> categories) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage categories");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox content = new VBox(10);
        TextField newName = new TextField();
        newName.setPromptText("New category name");
        ColorPicker newColor = new ColorPicker(Color.web("#12B8A6"));
        Button add = new Button("Add category");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> app.runAsync(() -> {
            app.categoryService().add(user, newName.getText(), toHex(newColor.getValue()));
            return app.categoryService().findAll(user);
        }, fresh -> {
            newName.clear();
            content.getChildren().setAll(categoryRows(fresh, dialog));
            content.getChildren().add(0, new HBox(8, newName, newColor, add));
        }));
        content.getChildren().add(new HBox(8, newName, newColor, add));
        content.getChildren().addAll(categoryRows(categories, dialog));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private List<VBox> categoryRows(List<Category> categories, Dialog<Void> dialog) {
        return categories.stream().map(category -> {
            TextField name = new TextField(category.name());
            ColorPicker color = new ColorPicker(Color.web(category.color()));
            Button save = new Button("Save");
            save.getStyleClass().add("secondary-button");
            save.setOnAction(event -> app.runAsync(() -> {
                app.categoryService().update(user, category.id(), name.getText(), toHex(color.getValue()));
                return app.categoryService().findAll(user);
            }, fresh -> refreshCategoryDialog(dialog, fresh)));
            Button delete = new Button("Delete");
            delete.getStyleClass().add("danger-button");
            delete.setOnAction(event -> {
                if (app.confirm("Delete category", "This also deletes expenses and budgets in “"
                        + category.name() + "”. Continue?")) {
                    app.runAsync(() -> {
                        app.categoryService().delete(user, category.id());
                        return app.categoryService().findAll(user);
                    }, fresh -> refreshCategoryDialog(dialog, fresh));
                }
            });
            return new VBox(new HBox(8, name, color, save, delete));
        }).toList();
    }

    private void refreshCategoryDialog(Dialog<Void> dialog, List<Category> fresh) {
        dialog.close();
        showCategoryDialog(fresh);
        loadCategories();
        search();
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255), (int) Math.round(color.getBlue() * 255));
    }

    private void installCategoryCells(ComboBox<Category> combo) {
        combo.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All categories" : item.name());
            }
        });
    }

    private record ExpenseForm(String title, String amount, Category category, LocalDate date, String notes) { }
    private record RecurringForm(String title, String amount, Category category, int dueDay, String notes) { }
}
