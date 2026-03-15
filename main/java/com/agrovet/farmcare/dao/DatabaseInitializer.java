package com.agrovet.farmcare.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean isSqlite = false;
            try {
                String url = conn.getMetaData().getURL();
                isSqlite = url != null && url.startsWith("jdbc:sqlite:");
            } catch (Exception ignored) {
                // If we can't detect, fall back to the legacy/MySQL-ish DDL.
            }

            exec(conn, """
                    CREATE TABLE IF NOT EXISTS users (
                        username VARCHAR(64) PRIMARY KEY,
                        password VARCHAR(255) NOT NULL,
                        email VARCHAR(255),
                        first_name VARCHAR(128),
                        last_name VARCHAR(128),
                        id_no VARCHAR(64),
                        role VARCHAR(32)
                    )
                    """);

            if (isSqlite) {
                exec(conn, """
                        CREATE TABLE IF NOT EXISTS stocks (
                            product_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            product_name VARCHAR(255) NOT NULL,
                            product_desc VARCHAR(1000),
                            category VARCHAR(255),
                            unit_bp DOUBLE,
                            unit_sp DOUBLE,
                            quantity INT,
                            reorder_lvl INT
                        )
                        """);

                exec(conn, """
                        CREATE TABLE IF NOT EXISTS sales (
                            sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username VARCHAR(64),
                            cust_name VARCHAR(255),
                            prod_name VARCHAR(255),
                            unit_price DOUBLE,
                            quantity INT,
                            total DOUBLE,
                            sale_date DATE
                        )
                        """);

                // Best-effort migration for existing installs.
                try {
                    exec(conn, "ALTER TABLE sales ADD COLUMN sale_date DATE");
                } catch (Exception ignored) {
                    // Column already exists or ALTER is not supported; ignore.
                }

                exec(conn, """
                        CREATE TABLE IF NOT EXISTS expenses (
                            exp_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username VARCHAR(64),
                            exp_date DATE,
                            exp_type VARCHAR(255),
                            exp_desc VARCHAR(1000),
                            total DOUBLE
                        )
                        """);

                exec(conn, """
                        CREATE TABLE IF NOT EXISTS purchases (
                            purchase_id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username VARCHAR(64),
                            prod_name VARCHAR(255),
                            quantity INT,
                            unit_bp DOUBLE,
                            supplier_name VARCHAR(255),
                            purchase_date DATE
                        )
                        """);

                // Best-effort migration for existing installs.
                try {
                    exec(conn, "ALTER TABLE purchases ADD COLUMN purchase_date DATE");
                } catch (Exception ignored) {
                    // Column already exists or ALTER is not supported; ignore.
                }
            } else {
                exec(conn, """
                        CREATE TABLE IF NOT EXISTS stocks (
                            product_id INT AUTO_INCREMENT PRIMARY KEY,
                            product_name VARCHAR(255) NOT NULL,
                            product_desc VARCHAR(1000),
                            category VARCHAR(255),
                            unit_bp DOUBLE,
                            unit_sp DOUBLE,
                            quantity INT,
                            reorder_lvl INT
                        )
                        """);

                exec(conn, """
                        CREATE TABLE IF NOT EXISTS sales (
                            sale_id INT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(64),
                            cust_name VARCHAR(255),
                            prod_name VARCHAR(255),
                            unit_price DOUBLE,
                            quantity INT,
                            total DOUBLE,
                            sale_date DATE
                        )
                        """);
                // Best-effort migration for existing installs.
                try {
                    exec(conn, "ALTER TABLE sales ADD sale_date DATE");
                } catch (Exception ignored) {
                    // Column already exists or ALTER is not supported; ignore.
                }

                exec(conn, """
                        CREATE TABLE IF NOT EXISTS expenses (
                            exp_id INT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(64),
                            exp_date DATE,
                            exp_type VARCHAR(255),
                            exp_desc VARCHAR(1000),
                            total DOUBLE
                        )
                        """);

                exec(conn, """
                        CREATE TABLE IF NOT EXISTS purchases (
                            purchase_id INT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(64),
                            prod_name VARCHAR(255),
                            quantity INT,
                            unit_bp DOUBLE,
                            supplier_name VARCHAR(255),
                            purchase_date DATE
                        )
                        """);

                // Best-effort migration for existing installs.
                try {
                    exec(conn, "ALTER TABLE purchases ADD purchase_date DATE");
                } catch (Exception ignored) {
                    // Column already exists or ALTER is not supported; ignore.
                }
            }
        } catch (Exception e) {
            // Don't fail app startup; missing DB is common in dev installs. Surface in logs.
            log.error("Database init failed: {}", rootMessage(e));
        }
    }

    private static void exec(Connection conn, String sql) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        Throwable next;
        while ((next = cur.getCause()) != null && next != cur) cur = next;
        String msg = cur.getMessage();
        return msg == null || msg.isBlank() ? cur.getClass().getSimpleName() : msg;
    }
}
