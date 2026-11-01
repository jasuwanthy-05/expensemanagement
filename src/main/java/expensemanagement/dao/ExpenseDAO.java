package expensemanagement.dao;

import expensemanagement.exception.DatabaseException;
import expensemanagement.model.Expense;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persists expenses and provides user-scoped search operations. */
public class ExpenseDAO {
    private static final String SELECT = "SELECT e.id, e.user_id, e.title, e.amount, e.category_id, "
            + "c.name AS category_name, e.expense_date, e.notes FROM expenses e "
            + "JOIN categories c ON c.id = e.category_id ";

    /** Loads the user's expenses, newest first. */
    public List<Expense> findAll(long userId) {
        return findFiltered(userId, null, null, null, null, null, null);
    }

    /** Filters by optional dates and amount bounds, plus a title/notes keyword and category. */
    public List<Expense> findFiltered(long userId, String keyword, Long categoryId, LocalDate from, LocalDate to,
                                      BigDecimal minAmount, BigDecimal maxAmount) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE e.user_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(e.title) LIKE ? OR LOWER(COALESCE(e.notes, '')) LIKE ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
        }
        appendCondition(sql, parameters, " AND e.category_id = ?", categoryId);
        appendCondition(sql, parameters, " AND e.expense_date >= ?", from == null ? null : Date.valueOf(from));
        appendCondition(sql, parameters, " AND e.expense_date <= ?", to == null ? null : Date.valueOf(to));
        appendCondition(sql, parameters, " AND e.amount >= ?", minAmount);
        appendCondition(sql, parameters, " AND e.amount <= ?", maxAmount);
        sql.append(" ORDER BY e.expense_date DESC, e.id DESC");
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                List<Expense> expenses = new ArrayList<>();
                while (result.next()) {
                    expenses.add(map(result));
                }
                return expenses;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load expenses.", ex);
        }
    }

    /** Finds one expense owned by a user. */
    public Optional<Expense> findById(long userId, long expenseId) {
        String sql = SELECT + "WHERE e.user_id = ? AND e.id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, expenseId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load the selected expense.", ex);
        }
    }

    /** Inserts an expense and returns its generated identifier. */
    public long create(Expense expense) {
        String sql = "INSERT INTO expenses (user_id, title, amount, category_id, expense_date, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindExpense(statement, expense, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return an expense ID.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not save expense.", ex);
        }
    }

    /** Updates an expense only when it belongs to its owner. */
    public void update(Expense expense) {
        String sql = "UPDATE expenses SET title = ?, amount = ?, category_id = ?, expense_date = ?, notes = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expense.title());
            statement.setBigDecimal(2, expense.amount());
            statement.setLong(3, expense.categoryId());
            statement.setDate(4, Date.valueOf(expense.expenseDate()));
            statement.setString(5, expense.notes());
            statement.setLong(6, expense.id());
            statement.setLong(7, expense.userId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expense was not found for this account.");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not update expense.", ex);
        }
    }

    /** Deletes an expense only when it belongs to its owner. */
    public void delete(long userId, long expenseId) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM expenses WHERE id = ? AND user_id = ?")) {
            statement.setLong(1, expenseId);
            statement.setLong(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Expense was not found for this account.");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not delete expense.", ex);
        }
    }

    private void bindExpense(PreparedStatement statement, Expense expense, boolean includeId) throws SQLException {
        statement.setLong(1, expense.userId());
        statement.setString(2, expense.title());
        statement.setBigDecimal(3, expense.amount());
        statement.setLong(4, expense.categoryId());
        statement.setDate(5, Date.valueOf(expense.expenseDate()));
        statement.setString(6, expense.notes());
        if (includeId) {
            statement.setLong(7, expense.id());
        }
    }

    private void appendCondition(StringBuilder sql, List<Object> parameters, String clause, Object value) {
        if (value != null) {
            sql.append(clause);
            parameters.add(value);
        }
    }

    private Expense map(ResultSet result) throws SQLException {
        return new Expense(result.getLong("id"), result.getLong("user_id"), result.getString("title"),
                result.getBigDecimal("amount"), result.getLong("category_id"),
                result.getString("category_name"), result.getDate("expense_date").toLocalDate(),
                result.getString("notes"));
    }
}
