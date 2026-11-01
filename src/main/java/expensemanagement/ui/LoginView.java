package expensemanagement.ui;

import expensemanagement.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** Login and account registration screen. */
public class LoginView extends BorderPane {
    private final MainApp app;
    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label message = new Label();
    private boolean registering;

    /** Creates the login screen and initializes its form. */
    public LoginView(MainApp app) {
        this.app = app;
        getStyleClass().add("login-page");
        VBox card = new VBox(16);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(430);
        card.setPadding(new Insets(42));
        Label brand = new Label("◈  Pennywise");
        brand.getStyleClass().add("login-brand");
        Label title = new Label("Welcome back");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Your spending, clear and in control.");
        subtitle.getStyleClass().add("muted-text");
        nameField.setPromptText("Your name");
        emailField.setPromptText("Email address");
        passwordField.setPromptText("Password (at least 8 characters)");
        Button submit = new Button("Sign in");
        submit.getStyleClass().add("primary-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(event -> authenticate());
        Button toggle = new Button("Create an account");
        toggle.getStyleClass().add("link-button");
        toggle.setOnAction(event -> {
            registering = !registering;
            nameField.setManaged(registering);
            nameField.setVisible(registering);
            submit.setText(registering ? "Create account" : "Sign in");
            toggle.setText(registering ? "I already have an account" : "Create an account");
            title.setText(registering ? "Create your account" : "Welcome back");
            message.setText("");
        });
        nameField.setManaged(false);
        nameField.setVisible(false);
        message.getStyleClass().add("error-text");
        card.getChildren().addAll(brand, title, subtitle, nameField, emailField, passwordField, message, submit, toggle);
        setCenter(card);
        setAlignment(card, Pos.CENTER);
    }

    private void authenticate() {
        message.setText("");
        app.runAsync(() -> {
            if (registering) {
                return app.authService().register(nameField.getText(), emailField.getText(), passwordField.getText());
            }
            return app.authService().login(emailField.getText(), passwordField.getText())
                    .orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect."));
        }, user -> {
            app.showApplication(user);
        }, error -> app.showError(error));
    }
}
