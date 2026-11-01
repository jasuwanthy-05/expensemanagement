package expensemanagement.ui;

import expensemanagement.model.User;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Account profile, password, and appearance settings. */
public class SettingsView extends VBox {
    private final MainApp app;
    private final User user;

    /** Creates the settings screen for an authenticated account. */
    public SettingsView(MainApp app, User user) {
        this.app = app;
        this.user = user;
        getStyleClass().add("page-content");
        setPadding(new Insets(30));
        setSpacing(18);
        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage your account and appearance.");
        subtitle.getStyleClass().add("muted-text");
        getChildren().addAll(title, subtitle, profileCard(), passwordCard(), appearanceCard());
    }

    private VBox profileCard() {
        Label heading = new Label("Profile");
        heading.getStyleClass().add("section-title");
        TextField name = new TextField(user.name());
        name.setPromptText("Your display name");
        Label email = new Label("Signed in as " + user.email());
        email.getStyleClass().add("muted-text");
        Button save = new Button("Save name");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> app.runAsync(() -> app.authService().changeName(user, name.getText()),
                app::updateCurrentUser));
        VBox card = new VBox(12, heading, name, email, save);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22));
        return card;
    }

    private VBox passwordCard() {
        Label heading = new Label("Change password");
        heading.getStyleClass().add("section-title");
        PasswordField current = new PasswordField();
        current.setPromptText("Current password");
        PasswordField next = new PasswordField();
        next.setPromptText("New password (at least 8 characters)");
        PasswordField confirm = new PasswordField();
        confirm.setPromptText("Confirm new password");
        Button change = new Button("Update password");
        change.getStyleClass().add("secondary-button");
        change.setOnAction(event -> {
            if (!next.getText().equals(confirm.getText())) {
                app.showError(new IllegalArgumentException("The new password and confirmation do not match."));
                return;
            }
            app.runAsync(() -> app.authService().changePassword(user, current.getText(), next.getText()),
                    app::updateCurrentUser);
        });
        VBox card = new VBox(12, heading, current, next, confirm, change);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22));
        return card;
    }

    private VBox appearanceCard() {
        Label heading = new Label("Appearance");
        heading.getStyleClass().add("section-title");
        CheckBox dark = new CheckBox("Use dark theme");
        dark.setOnAction(event -> app.setDarkMode(dark.isSelected()));
        VBox card = new VBox(12, heading, dark);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(22));
        return card;
    }
}
