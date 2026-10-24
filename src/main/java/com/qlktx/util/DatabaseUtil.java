package com.qlktx.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** JDBC configuration outside source control. Each DAO closes its own connection. */
public final class DatabaseUtil {
    private static final Properties SETTINGS = loadSettings();

    private DatabaseUtil() { }

    public static Connection getConnection() throws SQLException {
        String url = setting("KTX_DB_URL", "db.url",
                "jdbc:sqlserver://localhost:1433;databaseName=QuanLyKyTucXa;encrypt=true;trustServerCertificate=true;loginTimeout=5;");
        String user = setting("KTX_DB_USER", "db.user", "sa");
        String password = setting("KTX_DB_PASSWORD", "db.password", null);
        if (password == null || password.isBlank()) {
            throw new SQLException("Set KTX_DB_PASSWORD or an external KTX_DB_CONFIG before starting the application.");
        }
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Microsoft SQL Server JDBC driver is missing.", e);
        }
        Properties connection = new Properties();
        connection.setProperty("user", user);
        connection.setProperty("password", password);
        connection.setProperty("applicationName", "QuanLyKyTucXa");
        return DriverManager.getConnection(url, connection);
    }

    private static String setting(String environment, String property, String fallback) {
        String value = System.getenv(environment);
        if (value != null && !value.isBlank()) return value;
        return SETTINGS.getProperty(property, fallback);
    }

    private static Properties loadSettings() {
        Properties values = new Properties();
        String file = System.getenv("KTX_DB_CONFIG");
        if (file == null || file.isBlank()) return values;
        try (InputStream input = Files.newInputStream(Path.of(file))) {
            values.load(input);
            return values;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read external database configuration.", e);
        }
    }
}
