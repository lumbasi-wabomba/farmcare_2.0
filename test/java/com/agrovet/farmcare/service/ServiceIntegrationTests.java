package com.agrovet.farmcare.service;

import com.agrovet.farmcare.dao.DatabaseConnection;
import com.agrovet.farmcare.models.Purchases;
import com.agrovet.farmcare.models.Sales;
import com.agrovet.farmcare.models.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ServiceIntegrationTests {

    static {
        System.setProperty("farmcare.db.url", "jdbc:h2:mem:farmcare_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        System.setProperty("farmcare.db.user", "sa");
        System.setProperty("farmcare.db.password", "");
    }

    @Autowired
    private StockService stockService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private SalesService salesService;

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

    private static int count(Connection conn, String table) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = stmt.executeQuery()) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    @Test
    void recordingPurchaseUpdatesStock() throws Exception {
        Stock stock = new Stock(null, "Dairy Meal", "Desc", "Feeds", 100.0, 120.0, 10, 5);
        Stock saved = stockService.addStock(stock);
        assertNotNull(saved.getProductID());
        int productId = saved.getProductID();

        Purchases p = new Purchases();
        p.setUsername("alice");
        p.setProductName("Dairy Meal");
        p.setQuantity(7);
        p.setBuyingPrice(90.0);
        p.setSupplierName("Supplier");

        purchaseService.recordPurchase(p, false);

        Stock updated = stockService.getStockById(productId);
        assertNotNull(updated);
        assertEquals(17, updated.getQuantity());
        assertEquals(90.0, updated.getUnitBp(), 0.0001);
    }

    @Test
    void purchaseCannotCreateNewStockUnlessAllowed_andRollsBackPurchaseRow() throws Exception {
        Purchases p = new Purchases();
        p.setUsername("alice");
        p.setProductName("New Product");
        p.setQuantity(3);
        p.setBuyingPrice(50.0);
        p.setSupplierName("Supplier");

        SQLException ex = assertThrows(SQLException.class, () -> purchaseService.recordPurchase(p, false));
        assertTrue(ex.getMessage().toLowerCase().contains("product not found"));

        try (Connection conn = DatabaseConnection.getConnection()) {
            assertEquals(0, count(conn, "purchases"));
            assertEquals(0, count(conn, "stocks"));
        }

        Purchases recorded = purchaseService.recordPurchase(p, true);
        assertNotNull(recorded.getPurchaseID());
        Stock created = stockService.getStockByNameExact("New Product");
        assertNotNull(created);
        assertEquals(3, created.getQuantity());
        assertEquals(50.0, created.getUnitBp(), 0.0001);
    }

    @Test
    void recordingSaleReducesStockAndCreatesSale() throws Exception {
        Stock stock = new Stock(null, "Dairy Meal", "Desc", "Feeds", 100.0, 120.0, 10, 5);
        Stock saved = stockService.addStock(stock);
        assertNotNull(saved.getProductID());
        int productId = saved.getProductID();

        SalesService.Bill bill = salesService.processCustomerBill(
                "alice",
                "Customer",
                List.of(Map.of("productName", "Dairy Meal", "quantity", 2))
        );
        assertEquals(240.0, bill.getTotalAmount(), 0.0001);

        Stock updated = stockService.getStockById(productId);
        assertNotNull(updated);
        assertEquals(8, updated.getQuantity());

        List<Sales> sales = salesService.getAllSales();
        assertEquals(1, sales.size());
        assertEquals("Dairy Meal", sales.get(0).getProductName());
        assertEquals(120.0, sales.get(0).getUnitPrice(), 0.0001);
        assertEquals(240.0, sales.get(0).getTotal(), 0.0001);
    }
}
