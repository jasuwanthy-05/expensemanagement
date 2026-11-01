package expensemanagement.service;

import expensemanagement.dao.CategoryDAO;
import expensemanagement.dao.UserDAO;
import expensemanagement.exception.ValidationException;
import expensemanagement.model.User;
import expensemanagement.util.InputHelper;
import expensemanagement.util.PasswordUtil;

import java.util.Optional;

/** Implements registration, authentication, and account settings. */
public class AuthService {
    private final UserDAO userDAO;
    private final CategoryDAO categoryDAO;

    /** Creates the service using its persistence dependencies. */
    public AuthService(UserDAO userDAO, CategoryDAO categoryDAO) {
        this.userDAO = userDAO;
        this.categoryDAO = categoryDAO;
    }

    /** Registers an account and seeds its standard categories. */
    public User register(String name, String email, String password) {
        String cleanName = InputHelper.requiredText(name, "Name", 100);
        String cleanEmail = InputHelper.email(email);
        validatePassword(password);
        if (userDAO.findByEmail(cleanEmail).isPresent()) {
            throw new ValidationException("An account with that email already exists.");
        }
        User user = userDAO.create(cleanName, cleanEmail, PasswordUtil.hash(password));
        categoryDAO.seedDefaults(user.id());
        return user;
    }

    /** Authenticates credentials without revealing whether the email is registered. */
    public Optional<User> login(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        return userDAO.findByEmail(InputHelper.email(email))
                .filter(user -> PasswordUtil.verify(password, user.passwordHash()));
    }

    /** Updates the name for the current account and returns the refreshed user value. */
    public User changeName(User user, String name) {
        String cleanName = InputHelper.requiredText(name, "Name", 100);
        userDAO.updateName(user.id(), cleanName);
        return new User(user.id(), cleanName, user.email(), user.passwordHash(), user.createdAt());
    }

    /** Verifies the current password and replaces it with a new salted hash. */
    public User changePassword(User user, String currentPassword, String newPassword) {
        if (!PasswordUtil.verify(currentPassword, user.passwordHash())) {
            throw new ValidationException("Current password is incorrect.");
        }
        validatePassword(newPassword);
        String hash = PasswordUtil.hash(newPassword);
        userDAO.updatePassword(user.id(), hash);
        return new User(user.id(), user.name(), user.email(), hash, user.createdAt());
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 100) {
            throw new ValidationException("Password must be between 8 and 100 characters.");
        }
    }
}
