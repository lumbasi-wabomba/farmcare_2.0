package com.agrovet.farmcare.dao;

import com.agrovet.farmcare.models.Expense;
import com.agrovet.farmcare.models.Purchases;
import com.agrovet.farmcare.models.Sales;
import com.agrovet.farmcare.models.Stock;
import com.agrovet.farmcare.models.StockSort;
import com.agrovet.farmcare.models.Users;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrudDaoTests {

    private final UserDao userDao = new UserDao();
    private final StockDao stockDao = new StockDao();
    private final SalesDao salesDao = new SalesDao();
    private final ExpenseDao expenseDao = new ExpenseDao();
    private final PurchasesDao purchasesDao = new PurchasesDao();

    @BeforeAll
    static void configureInMemoryDb() {
        System.setProperty("farmcare.db.url", "jdbc:h2:mem:farmcare_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        System.setProperty("farmcare.db.user", "sa");
        System.setProperty("farmcare.db.password", "");
    }

    @BeforeEach
    void resetSchema() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            exec(conn, "DROP TABLE IF EXISTS sales");
            exec(conn, "DROP TABLE IF EXISTS purchases");
            exec(conn, "DROP TABLE IF EXISTS expenses");
            exec(conn, "DROP TABLE IF EXISTS Stock");
            exec(conn, "DROP TABLE IF EXISTS stocks");
            exec(conn, "DROP TABLE IF EXISTS users");

            exec(conn, """
                    CREATE TABLE users (
                        username VARCHAR(64) PRIMARY KEY,
                        password VARCHAR(255) NOT NULL,
                        email VARCHAR(255),
                        first_name VARCHAR(128),
                        last_name VARCHAR(128),
                        id_no VARCHAR(64),
                        role VARCHAR(32)
                    )
                    """);

            exec(conn, """
                    CREATE TABLE stocks (
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
                    CREATE TABLE sales (
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

            exec(conn, """
                    CREATE TABLE expenses (
                        exp_id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(64),
                        exp_date DATE,
                        exp_type VARCHAR(255),
                        exp_desc VARCHAR(1000),
                        total DOUBLE
                    )
                    """);

            exec(conn, """
                    CREATE TABLE purchases (
                        purchase_id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(64),
                        prod_name VARCHAR(255),
                        quantity INT,
                        unit_bp DOUBLE,
                        supplier_name VARCHAR(255),
                        purchase_date DATE
                    )
                    """);
        }
    }

    private static void exec(Connection conn, String sql) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    @Test
    void usersCrudWorks() throws Exception {
        Users u = new Users("alice", "pw", "a@example.com", "Alice", "Doe", "123", "admin");
        assertEquals("alice", userDao.save(u).getUsername());

        Users got = new Users();
        got.setUsername("alice");
        Users loaded = userDao.get(got);
        assertNotNull(loaded);
        assertEquals("Alice", loaded.getFirstName());

        List<Users> all = userDao.getAll();
        assertEquals(1, all.size());

        String[] params = {"pw2", "b@example.com", "Alicia", "Doe", "123", "superuser"};
        u.setUsername("alice");
        userDao.update(u, params);

        Users updated = userDao.get(got);
        assertNotNull(updated);
        assertEquals("b@example.com", updated.getEmail());
        assertEquals("superuser", updated.getRole());

        userDao.delete("alice");
        assertNull(userDao.get(got));
    }

    @Test
    void stockCrudWorks() throws Exception {
        Stock s = new Stock(null, "Dairy Meal", "Desc", "Feeds", 100.0, 120.0, 50, 10);
        stockDao.save(s);
        assertNotNull(s.getProductID());

        Stock key = new Stock();
        key.setProductID(s.getProductID());
        Stock loaded = stockDao.get(key);
        assertNotNull(loaded);
        assertEquals("Dairy Meal", loaded.getProductName());

        assertEquals(1, stockDao.countAll());
        assertEquals(1, stockDao.getAll().size());
        assertEquals(1, stockDao.getPage(25, 0, StockSort.QUANTITY_DESC).size());

        String[] params = {"Dairy Meal", "Desc2", "Feeds", "110.0", "130.0", "40", "12"};
        stockDao.update(key, params);
        Stock updated = stockDao.get(key);
        assertNotNull(updated);
        assertEquals(40, updated.getQuantity());
        assertEquals(12, updated.getReorderLevel());

        stockDao.delete(String.valueOf(s.getProductID()));
        assertNull(stockDao.get(key));
    }

    @Test
    void salesCrudWorks() throws Exception {
        Sales sale = new Sales();
        sale.setUsername("alice");
        sale.setCustomerName("Customer");
        sale.setProductName("Dairy Meal");
        sale.setUnitPrice(120.0);
        sale.setQuantity(2);
        sale.setTotal(240.0);
        sale.setSaleDate(java.sql.Date.valueOf(LocalDate.of(2026, 3, 15)));

        Sales saved = salesDao.save(sale);
        assertTrue(saved.getSalesID() > 0);

        Sales key = new Sales();
        key.setSalesID(saved.getSalesID());
        Sales loaded = salesDao.get(key);
        assertNotNull(loaded);
        assertEquals(240.0, loaded.getTotal(), 0.0001);
        assertNotNull(loaded.getSaleDate());

        assertEquals(1, salesDao.getAll().size());

        String[] params = {"alice", "Customer2", "Dairy Meal", "120.0", "3", "360.0", LocalDate.of(2026, 3, 16).toString()};
        salesDao.update(key, params);
        Sales updated = salesDao.get(key);
        assertNotNull(updated);
        assertEquals("Customer2", updated.getCustomerName());
        assertEquals(3, updated.getQuantity());
        assertEquals(360.0, updated.getTotal(), 0.0001);
        assertEquals(LocalDate.of(2026, 3, 16), new java.sql.Date(updated.getSaleDate().getTime()).toLocalDate());

        salesDao.delete(String.valueOf(saved.getSalesID()));
        assertNull(salesDao.get(key));
    }

    @Test
    void expensesCrudWorks() throws Exception {
        Expense e = new Expense();
        e.setUsername("alice");
        e.setExpenseDate(new Date());
        e.setExpenseType("Transport");
        e.setExpenseDescription("Taxi");
        e.setTotal(500.0);
        expenseDao.save(e);
        assertTrue(e.getExpenseID() > 0);

        Expense key = new Expense();
        key.setExpenseID(e.getExpenseID());
        Expense loaded = expenseDao.get(key);
        assertNotNull(loaded);
        assertEquals("Transport", loaded.getExpenseType());

        assertEquals(1, expenseDao.getAll().size());

        String isoDate = LocalDate.of(2026, 3, 15).toString();
        String[] params = {"alice", isoDate, "Utilities", "Electricity", "800.50"};
        expenseDao.update(key, params);
        Expense updated = expenseDao.get(key);
        assertNotNull(updated);
        assertEquals("Utilities", updated.getExpenseType());
        assertEquals(800.50, updated.getTotal(), 0.0001);

        expenseDao.delete(String.valueOf(e.getExpenseID()));
        assertNull(expenseDao.get(key));
    }

    @Test
    void purchasesCrudWorks() throws Exception {
        Purchases p = new Purchases();
        p.setUsername("alice");
        p.setProductName("Dairy Meal");
        p.setQuantity(10);
        p.setBuyingPrice(95.0);
        p.setSupplierName("Supplier");
        p.setPurchaseDate(java.sql.Date.valueOf(LocalDate.of(2026, 3, 15)));
        purchasesDao.save(p);
        assertTrue(p.getPurchaseID() > 0);

        Purchases key = new Purchases();
        key.setPurchaseID(p.getPurchaseID());
        Purchases loaded = purchasesDao.get(key);
        assertNotNull(loaded);
        assertEquals("Supplier", loaded.getSupplierName());
        assertNotNull(loaded.getPurchaseDate());

        assertEquals(1, purchasesDao.getAll().size());

        String[] params = {"alice", "Dairy Meal", "12", "97.5", "Supplier2", LocalDate.of(2026, 3, 16).toString()};
        purchasesDao.update(key, params);
        Purchases updated = purchasesDao.get(key);
        assertNotNull(updated);
        assertEquals(12, updated.getQuantity());
        assertEquals(97.5, updated.getBuyingPrice(), 0.0001);
        assertEquals(LocalDate.of(2026, 3, 16), new java.sql.Date(updated.getPurchaseDate().getTime()).toLocalDate());

        purchasesDao.delete(String.valueOf(p.getPurchaseID()));
        assertNull(purchasesDao.get(key));
    }
}
