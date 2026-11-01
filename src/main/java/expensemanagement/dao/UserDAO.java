package expensemanagement.dao;

import expensemanagement.exception.DatabaseException;
import expensemanagement.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/** Persists user accounts and authentication details. */
public class UserDAO {
    /** Finds an account by normalized email. */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, name, email, password_hash, created_at FROM users WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not look up the user account.", ex);
        }
    }

    /** Creates a user and returns the persisted account. */
    public User create(String name, String email, String passwordHash) {
        String sql = "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return a user ID.");
                }
                return new User(keys.getLong(1), name, email, passwordHash, LocalDateTime.now());
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not create the account. The email may already be registered.", ex);
        }
    }

    /** Updates the account display name. */
    public void updateName(long userId, String name) {
        executeUpdate("UPDATE users SET name = ? WHERE id = ?", name, userId, "Could not update your name.");
    }

    /** Updates the account password hash. */
    public void updatePassword(long userId, String passwordHash) {
        executeUpdate("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId,
                "Could not update your password.");
    }

    private void executeUpdate(String sql, String value, long userId, String message) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setLong(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("User account no longer exists.");
            }
        } catch (SQLException ex) {
            throw new DatabaseException(message, ex);
        }
    }

    private User map(ResultSet result) throws SQLException {
        Timestamp created = result.getTimestamp("created_at");
        return new User(result.getLong("id"), result.getString("name"), result.getString("email"),
                result.getString("password_hash"), created == null ? null : created.toLocalDateTime());
    }
}
