package expensemanagement.dao;

import expensemanagement.exception.DatabaseException;
import expensemanagement.model.Budget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persists monthly category budgets. */
public class BudgetDAO {
    /** Returns the budget for a user, category, and month if it exists. */
    public Optional<Budget> find(long userId, long categoryId, int month, int year) {
        String sql = "SELECT id, user_id, category_id, monthly_limit, month, year FROM budgets "
                + "WHERE user_id = ? AND category_id = ? AND month = ? AND year = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPeriod(statement, userId, categoryId, month, year);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load budget.", ex);
        }
    }

    /** Returns all budgets for a selected month. */
    public List<Budget> findAll(long userId, int month, int year) {
        String sql = "SELECT id, user_id, category_id, monthly_limit, month, year FROM budgets "
                + "WHERE user_id = ? AND month = ? AND year = ? ORDER BY category_id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, month);
            statement.setInt(3, year);
            try (ResultSet result = statement.executeQuery()) {
                List<Budget> budgets = new ArrayList<>();
                while (result.next()) {
                    budgets.add(map(result));
                }
                return budgets;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load monthly budgets.", ex);
        }
    }

    /** Creates or replaces a user's category budget for a month. */
    public void save(long userId, long categoryId, BigDecimal limit, int month, int year) {
        String sql = "INSERT INTO budgets (user_id, category_id, monthly_limit, month, year) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE monthly_limit = VALUES(monthly_limit)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, categoryId);
            statement.setBigDecimal(3, limit);
            statement.setInt(4, month);
            statement.setInt(5, year);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Could not save monthly budget.", ex);
        }
    }

    /** Deletes a budget for a category and period. */
    public void delete(long userId, long categoryId, int month, int year) {
        String sql = "DELETE FROM budgets WHERE user_id = ? AND category_id = ? AND month = ? AND year = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPeriod(statement, userId, categoryId, month, year);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Could not delete monthly budget.", ex);
        }
    }

    private void bindPeriod(PreparedStatement statement, long userId, long categoryId,
                            int month, int year) throws SQLException {
        statement.setLong(1, userId);
        statement.setLong(2, categoryId);
        statement.setInt(3, month);
        statement.setInt(4, year);
    }

    private Budget map(ResultSet result) throws SQLException {
        return new Budget(result.getLong("id"), result.getLong("user_id"), result.getLong("category_id"),
                result.getBigDecimal("monthly_limit"), result.getInt("month"), result.getInt("year"));
    }
}
