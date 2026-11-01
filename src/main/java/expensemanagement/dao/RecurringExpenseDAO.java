package expensemanagement.dao;

import expensemanagement.exception.DatabaseException;
import expensemanagement.model.RecurringExpense;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Persists recurring expense templates and generates due transactions atomically. */
public class RecurringExpenseDAO {
    /** Lists recurring templates for a user. */
    public List<RecurringExpense> findAll(long userId) {
        String sql = "SELECT id, user_id, title, amount, category_id, due_day, notes, active "
                + "FROM recurring_expenses WHERE user_id = ? ORDER BY due_day";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<RecurringExpense> items = new ArrayList<>();
                while (result.next()) {
                    items.add(map(result));
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load recurring expenses.", ex);
        }
    }

    /** Saves a recurring template and returns its generated identifier. */
    public long create(RecurringExpense item) {
        String sql = "INSERT INTO recurring_expenses (user_id, title, amount, category_id, due_day, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, item.userId());
            statement.setString(2, item.title());
            statement.setBigDecimal(3, item.amount());
            statement.setLong(4, item.categoryId());
            statement.setInt(5, item.dueDay());
            statement.setString(6, item.notes());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return a recurring expense ID.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not save recurring expense.", ex);
        }
    }

    /** Deletes a recurring template belonging to the user. */
    public void delete(long userId, long id) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM recurring_expenses WHERE id = ? AND user_id = ?")) {
            statement.setLong(1, id);
            statement.setLong(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Recurring expense was not found.");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not delete recurring expense.", ex);
        }
    }

    /** Creates due transactions once per month, in the same transaction as the generation markers. */
    public int generateDue(long userId, LocalDate today) {
        String selectSql = "SELECT id, title, amount, category_id, due_day, notes FROM recurring_expenses "
                + "WHERE user_id = ? AND active = TRUE AND due_day <= ? "
                + "AND (last_generated_year IS NULL OR last_generated_year <> ? OR last_generated_month <> ?) FOR UPDATE";
        String insertSql = "INSERT INTO expenses (user_id, title, amount, category_id, expense_date, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE recurring_expenses SET last_generated_year = ?, last_generated_month = ? "
                + "WHERE id = ? AND user_id = ?";
        YearMonth period = YearMonth.from(today);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(selectSql);
                 PreparedStatement insert = connection.prepareStatement(insertSql);
                 PreparedStatement update = connection.prepareStatement(updateSql)) {
                select.setLong(1, userId);
                select.setInt(2, today.getDayOfMonth());
                select.setInt(3, period.getYear());
                select.setInt(4, period.getMonthValue());
                int generated = 0;
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        int day = Math.min(result.getInt("due_day"), period.lengthOfMonth());
                        insert.setLong(1, userId);
                        insert.setString(2, result.getString("title"));
                        insert.setBigDecimal(3, result.getBigDecimal("amount"));
                        insert.setLong(4, result.getLong("category_id"));
                        insert.setDate(5, Date.valueOf(period.atDay(day)));
                        insert.setString(6, result.getString("notes"));
                        insert.executeUpdate();
                        update.setInt(1, period.getYear());
                        update.setInt(2, period.getMonthValue());
                        update.setLong(3, result.getLong("id"));
                        update.setLong(4, userId);
                        update.executeUpdate();
                        generated++;
                    }
                }
                connection.commit();
                return generated;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not generate due recurring expenses.", ex);
        }
    }

    private RecurringExpense map(ResultSet result) throws SQLException {
        return new RecurringExpense(result.getLong("id"), result.getLong("user_id"), result.getString("title"),
                result.getBigDecimal("amount"), result.getLong("category_id"), result.getInt("due_day"),
                result.getString("notes"), result.getBoolean("active"));
    }
}
