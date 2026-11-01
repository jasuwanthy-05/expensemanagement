package expensemanagement.dao;

import expensemanagement.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Loads MySQL connection settings from /config/db.properties and opens JDBC connections. */
public final class DBConnection {
    private static final Properties PROPERTIES = loadProperties();

    private DBConnection() { }

    /** Opens a new MySQL connection using configured properties. */
    public static Connection getConnection() throws SQLException {
        String url = requiredProperty("db.url");
        String username = requiredProperty("db.username");
        String password = PROPERTIES.getProperty("db.password", "");
        return DriverManager.getConnection(url, username, password);
    }

    private static String requiredProperty(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DatabaseException("Missing required setting '" + key + "' in config/db.properties.", null);
        }
        return value.trim();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = DBConnection.class.getResourceAsStream("/config/db.properties")) {
            if (stream == null) {
                throw new DatabaseException("Could not find /config/db.properties on the application classpath.", null);
            }
            properties.load(stream);
            return properties;
        } catch (IOException ex) {
            throw new DatabaseException("Could not read database configuration.", ex);
        }
    }
}
