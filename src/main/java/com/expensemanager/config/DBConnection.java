package com.expensemanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Simple DB connection factory that reads settings from classpath file db.properties.
 * Environment variables DB_URL, DB_USER, DB_PASSWORD override file values.
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                PROPS.load(in);
            } else {
                System.err.println("WARNING: db.properties not found on classpath.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = get("db.url", "DB_URL");
        String user = get("db.user", "DB_USER");
        String password = get("db.password", "DB_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }

    private static String get(String propName, String envName) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return PROPS.getProperty(propName);
    }

    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {
            }
        }
    }
}
