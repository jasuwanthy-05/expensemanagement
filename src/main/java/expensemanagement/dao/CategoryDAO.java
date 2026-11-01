package expensemanagement.dao;

import expensemanagement.exception.DatabaseException;
import expensemanagement.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persists user-owned expense categories. */
public class CategoryDAO {
    private static final String[] DEFAULTS = {
            "Food", "Transport", "Shopping", "Bills", "Health", "Entertainment", "Education", "Other"
    };

    /** Returns all categories for a user, alphabetically. */
    public List<Category> findAll(long userId) {
        String sql = "SELECT id, name, user_id, color FROM categories WHERE user_id = ? ORDER BY name";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<Category> categories = new ArrayList<>();
                while (result.next()) {
                    categories.add(map(result));
                }
                return categories;
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not load categories.", ex);
        }
    }

    /** Adds the default categories if this account does not yet have any. */
    public void seedDefaults(long userId) {
        String existsSql = "SELECT COUNT(*) FROM categories WHERE user_id = ?";
        String insertSql = "INSERT INTO categories (name, user_id, color) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement exists = connection.prepareStatement(existsSql)) {
            exists.setLong(1, userId);
            try (ResultSet result = exists.executeQuery()) {
                result.next();
                if (result.getInt(1) > 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                String[] colors = {"#12B8A6", "#5B8DEF", "#A855F7", "#F59E0B",
                        "#EC4899", "#8B5CF6", "#14B8A6", "#64748B"};
                for (int i = 0; i < DEFAULTS.length; i++) {
                    insert.setString(1, DEFAULTS[i]);
                    insert.setLong(2, userId);
                    insert.setString(3, colors[i]);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not create default categories.", ex);
        }
    }

    /** Creates a category and returns its generated database identifier. */
    public Category create(long userId, String name, String color) {
        String sql = "INSERT INTO categories (name, user_id, color) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setLong(2, userId);
            statement.setString(3, color);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return a category ID.");
                }
                return new Category(keys.getLong(1), name, userId, color);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not create category. Category names must be unique.", ex);
        }
    }

    /** Renames and recolors a category belonging to the given user. */
    public void update(long userId, long categoryId, String name, String color) {
        String sql = "UPDATE categories SET name = ?, color = ? WHERE id = ? AND user_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, color);
            statement.setLong(3, categoryId);
            statement.setLong(4, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Category was not found for this account.");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not update category. Category names must be unique.", ex);
        }
    }

    /** Deletes a category and its dependent expenses and budgets. */
    public void delete(long userId, long categoryId) {
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            statement.setLong(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Category was not found for this account.");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Could not delete category.", ex);
        }
    }

    private Category map(ResultSet result) throws SQLException {
        return new Category(result.getLong("id"), result.getString("name"),
                result.getLong("user_id"), result.getString("color"));
    }
}
