package expensemanagement.ui;

import expensemanagement.dao.BudgetDAO;
import expensemanagement.dao.CategoryDAO;
import expensemanagement.dao.ExpenseDAO;
import expensemanagement.dao.RecurringExpenseDAO;
import expensemanagement.dao.UserDAO;
import expensemanagement.model.User;
import expensemanagement.service.AuthService;
import expensemanagement.service.BudgetService;
import expensemanagement.service.CategoryService;
import expensemanagement.service.ExpenseService;
import expensemanagement.service.ReportService;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** JavaFX application entry point and application-wide navigation/service owner. */
public class MainApp extends Application {
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();
    private final RecurringExpenseDAO recurringDAO = new RecurringExpenseDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AuthService authService = new AuthService(userDAO, categoryDAO);
    private final CategoryService categoryService = new CategoryService(categoryDAO);
    private final ExpenseService expenseService = new ExpenseService(expenseDAO, categoryDAO, recurringDAO);
    private final BudgetService budgetService = new BudgetService(budgetDAO, expenseDAO, categoryDAO);
    private final ReportService reportService = new ReportService(expenseDAO);
    private Stage stage;
    private StackPane screenHost;
    private BorderPane shell;
    private User currentUser;
    private boolean darkMode;
    private String activeNavigation = "Dashboard";

    @Override
    /** Initializes the main window and displays authentication. */
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Pennywise | Expense Manager");
        stage.getIcons().add(createAppIcon());
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setWidth(1200);
        stage.setHeight(750);
        showLogin();
        stage.show();
    }

    private Image createAppIcon() {
        WritableImage icon = new WritableImage(32, 32);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                boolean mark = (x > 7 && x < 24 && y > 6 && y < 26)
                        && ((x < 15 && y < 12) || (x > 15 && y > 18) || (y > 12 && y < 20));
                icon.getPixelWriter().setArgb(x, y, mark ? 0xFF12B8A6 : 0xFF172554);
            }
        }
        return icon;
    }

    /** Starts a JavaFX background task and reports failures through the application's alert UX. */
    public <T> void runAsync(Callable<T> work, Consumer<T> onSuccess) {
        runAsync(work, onSuccess, this::showError);
    }

    /** Starts a task with a screen-specific failure callback. */
    public <T> void runAsync(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));
        Thread thread = new Thread(task, "expense-manager-database");
        thread.setDaemon(true);
        thread.start();
    }

    /** Displays a user-friendly error message. */
    public void showError(Throwable error) {
        String message = error == null || error.getMessage() == null
                ? "The operation could not be completed." : error.getMessage();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong");
        alert.setHeaderText("We couldn't complete that action");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Displays an informational message. */
    public void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Expense Manager");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Asks for confirmation before a destructive action. */
    public boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    /** Shows the authentication screen. */
    public void showLogin() {
        currentUser = null;
        screenHost = new StackPane();
        Scene scene = new Scene(screenHost, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        switchContent(new LoginView(this));
    }

    /** Shows the authenticated application shell and default dashboard. */
    public void showApplication(User user) {
        currentUser = user;
        runAsync(() -> expenseService.generateDueRecurring(user, java.time.LocalDate.now()), generated -> {
            if (generated > 0) {
                showInfo(generated + " recurring expense(s) were added for this month.");
            }
            buildShell();
            navigate("Dashboard");
        });
    }

    private void buildShell() {
        shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        applyTheme();
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(28, 16, 20, 16));
        Label brand = new Label("◈  Pennywise");
        brand.getStyleClass().add("brand");
        Label subtitle = new Label("PERSONAL FINANCE");
        subtitle.getStyleClass().add("sidebar-caption");
        VBox identity = new VBox(4, brand, subtitle);
        identity.setPadding(new Insets(0, 8, 24, 8));
        sidebar.getChildren().add(identity);
        for (String item : new String[]{"Dashboard", "Expenses", "Budgets", "Reports", "Settings"}) {
            Button button = navButton(item);
            sidebar.getChildren().add(button);
        }
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label account = new Label("●  " + currentUser.name());
        account.getStyleClass().add("account-name");
        Button logout = navButton("⇥  Logout");
        logout.setOnAction(event -> showLogin());
        sidebar.getChildren().addAll(spacer, account, logout);
        shell.setLeft(sidebar);
        screenHost = new StackPane();
        screenHost.getStyleClass().add("content-host");
        shell.setCenter(screenHost);
        stage.getScene().setRoot(shell);
    }

    private Button navButton(String item) {
        String label = item.equals("Logout") ? "⇥  Logout" : switch (item) {
            case "Dashboard" -> "⌂  Dashboard";
            case "Expenses" -> "↗  Expenses";
            case "Budgets" -> "▤  Budgets";
            case "Reports" -> "▥  Reports";
            case "Settings" -> "⚙  Settings";
            default -> item;
        };
        Button button = new Button(label);
        button.getStyleClass().add("nav-button");
        if (item.equals(activeNavigation)) {
            button.getStyleClass().add("nav-active");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> {
            if (item.equals("Logout")) {
                showLogin();
            } else {
                navigate(item);
            }
        });
        return button;
    }

    /** Opens the requested top-level view. */
    public void navigate(String destination) {
        if (currentUser == null) {
            showLogin();
            return;
        }
        activeNavigation = destination;
        Node view = switch (destination) {
            case "Expenses" -> new ExpenseView(this, currentUser);
            case "Budgets" -> new BudgetView(this, currentUser);
            case "Reports" -> new ReportView(this, currentUser);
            case "Settings" -> new SettingsView(this, currentUser);
            default -> new DashboardView(this, currentUser);
        };
        buildShell();
        switchContent(view);
    }

    private void switchContent(Node node) {
        if (screenHost == null) {
            screenHost = new StackPane();
        }
        node.setOpacity(0);
        screenHost.getChildren().setAll(node);
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /** Updates the current signed-in user after a profile name change. */
    public void updateCurrentUser(User user) {
        currentUser = user;
        buildShell();
        navigate("Settings");
    }

    /** Applies the requested light or dark theme to the application shell. */
    public void setDarkMode(boolean enabled) {
        darkMode = enabled;
        applyTheme();
    }

    private void applyTheme() {
        if (shell == null) {
            return;
        }
        shell.getStyleClass().remove("dark-theme");
        if (darkMode) {
            shell.getStyleClass().add("dark-theme");
        }
    }

    /** Provides the authenticated user to screen controllers. */
    public User currentUser() { return currentUser; }

    /** Provides account operations to the authentication and settings screens. */
    public AuthService authService() { return authService; }

    /** Provides expense operations to application screens. */
    public ExpenseService expenseService() { return expenseService; }

    /** Provides budget operations to application screens. */
    public BudgetService budgetService() { return budgetService; }

    /** Provides report operations to application screens. */
    public ReportService reportService() { return reportService; }

    /** Provides category operations to application screens. */
    public CategoryDAO categoryDAO() { return categoryDAO; }

    /** Provides category operations to application screens. */
    public CategoryService categoryService() { return categoryService; }

    /** Launches the desktop application. */
    public static void main(String[] args) {
        launch(args);
    }
}
