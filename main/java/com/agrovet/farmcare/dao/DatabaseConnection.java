package com.agrovet.farmcare.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLDataException;
import java.util.Properties;

public class DatabaseConnection {
    private static final String DEFAULT_URL = "jdbc:sqlite:data/farmcare.db";
    private static final String DEFAULT_USER = "";
    private static final String DEFAULT_PASSWORD = "";

    private static final Properties CLASSPATH_PROPS = loadClasspathProperties();

    private static String envOrProp(String propKey, String envKey, String defaultValue) {
        String fromProp = System.getProperty(propKey);
        if (fromProp != null && !fromProp.isBlank()) return fromProp;
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        return defaultValue;
    }

    private static Properties loadClasspathProperties() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
            // Best-effort: the app can still run with env/system properties.
        }
        return props;
    }

    private static String getFromClasspath(String key) {
        String v = CLASSPATH_PROPS.getProperty(key);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    private static String resolveUrl() {
        return firstNonBlank(
                envOrProp("farmcare.db.url", "FARMCARE_DB_URL", ""),
                getFromClasspath("farmcare.db.url"),
                envOrProp("spring.datasource.url", "SPRING_DATASOURCE_URL", ""),
                getFromClasspath("spring.datasource.url"),
                DEFAULT_URL
        );
    }

    private static String resolveUser() {
        return firstNonBlank(
                envOrProp("farmcare.db.user", "FARMCARE_DB_USER", ""),
                getFromClasspath("farmcare.db.user"),
                envOrProp("spring.datasource.username", "SPRING_DATASOURCE_USERNAME", ""),
                getFromClasspath("spring.datasource.username"),
                DEFAULT_USER
        );
    }

    private static String resolvePassword() {
        return firstNonBlank(
                envOrProp("farmcare.db.password", "FARMCARE_DB_PASSWORD", ""),
                getFromClasspath("farmcare.db.password"),
                envOrProp("spring.datasource.password", "SPRING_DATASOURCE_PASSWORD", ""),
                getFromClasspath("spring.datasource.password"),
                DEFAULT_PASSWORD
        );
    }

    private static void ensureSqliteParentDirExists(String jdbcUrl) {
        if (jdbcUrl == null) return;
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) return;

        String spec = jdbcUrl.substring("jdbc:sqlite:".length());
        if (spec.startsWith(":memory:")) return;

        // Strip query parameters if present.
        int qIdx = spec.indexOf('?');
        if (qIdx >= 0) spec = spec.substring(0, qIdx);

        // Support basic file: URLs (best-effort).
        if (spec.startsWith("file:")) spec = spec.substring("file:".length());

        java.io.File f = new java.io.File(spec);
        java.io.File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
    }

    public static Connection getConnection() throws SQLDataException {
        try {
            String url = resolveUrl();
            String user = resolveUser();
            String password = resolvePassword();

            ensureSqliteParentDirExists(url);
            if (url.startsWith("jdbc:sqlite:")) {
                return DriverManager.getConnection(url);
            }
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            throw new SQLDataException("Unable to connect to database", e);
        }
    }
}
