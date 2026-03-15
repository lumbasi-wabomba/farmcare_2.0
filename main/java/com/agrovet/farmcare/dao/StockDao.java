package com.agrovet.farmcare.dao;

import com.agrovet.farmcare.models.Stock;
import com.agrovet.farmcare.models.StockSort;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StockDao  implements Dao<Stock> {

    @Override
    public Stock get(Stock stock) throws SQLException {
        String query = "SELECT * FROM stocks WHERE product_id = ?";
        try (Connection myconn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myconn.prepareStatement(query);
            if (stock.getProductID() == null) {
                throw new SQLException("Product ID is required");
            }
            stmt.setInt(1, stock.getProductID());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Stock(
                 rs.getObject("product_id", Integer.class),
                 rs.getString("product_name"),
                 rs.getString("product_desc"),
                 rs.getString("category"),
                 rs.getDouble("unit_bp"),
                 rs.getDouble("unit_sp"),
                 rs.getInt("quantity"),
                 rs.getInt("reorder_lvl")
                );
            }
            return  null;
        } catch ( SQLException e){
            throw new SQLException("Error fetching stock", e);
        }
    }

    @Override
    public List<Stock> getAll() {
        String query = "SELECT * FROM stocks";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            List<Stock> stockList = new ArrayList<>();
            while (rs.next()) {
                stockList.add(new Stock(
                        rs.getObject("product_id", Integer.class),
                        rs.getString("product_name"),
                        rs.getString("product_desc"),
                        rs.getString("category"),
                        rs.getDouble("unit_bp"),
                        rs.getDouble("unit_sp"),
                        rs.getInt("quantity"),
                        rs.getInt("reorder_lvl")
                ));
            }
            return stockList;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all stock", e);
        }
    }

    @Override
    public Stock save(Stock stock) {
        String query = "INSERT INTO stocks (product_name, product_desc, category, unit_bp, unit_sp, quantity, reorder_lvl) VALUES (?,?,?,?,?,?,?)";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, stock.getProductName());
            stmt.setString(2, stock.getProductDescription());
            stmt.setString(3, stock.getCategory());
            stmt.setDouble(4, stock.getUnitBp());
            stmt.setDouble(5, stock.getUnitSp());
            stmt.setInt(6, stock.getQuantity());
            stmt.setInt(7, stock.getReorderLevel());
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating stock failed, no rows affected.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    stock.setProductID(generatedKeys.getInt(1));
                }
            }
            return stock;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving stock", e);
        }
    }

    @Override
    public Stock update(Stock stock, String[] params) {
        String query = "UPDATE stocks SET product_name = ?, product_desc = ?, category = ?, unit_bp = ?, unit_sp = ?, quantity = ?, reorder_lvl = ? WHERE product_id = ?";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(query)) {
            stmt.setString(1, params[0]); // name
            stmt.setString(2, params[1]); // desc
            stmt.setString(3, params[2]); // cat
            stmt.setDouble(4, Double.parseDouble(params[3])); // bp
            stmt.setDouble(5, Double.parseDouble(params[4])); // sp
            stmt.setInt(6, Integer.parseInt(params[5])); // qty
            stmt.setInt(7, Integer.parseInt(params[6])); // rlvl
            if (stock.getProductID() == null) {
                throw new SQLException("Product ID is required");
            }
            stmt.setInt(8, stock.getProductID());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No stock updated for product_id: " + stock.getProductID());
            }
            return stock;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating stock", e);
        }
    }

    @Override
    public Stock delete(String productID) throws SQLException {
        String query = "DELETE FROM stocks WHERE product_id = ?";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(query)) {
            stmt.setInt(1, Integer.parseInt(productID));
            stmt.executeUpdate();
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting stock", e);
        }
    }

    public int countAll() throws SQLException {
        String query = "SELECT COUNT(*) AS cnt FROM stocks";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("cnt") : 0;
        } catch (SQLException e) {
            throw new SQLException("Error counting stock items", e);
        }
    }

    public List<Stock> getPage(int limit, int offset, StockSort sort) throws SQLException {
        String orderBy = switch (sort == null ? StockSort.QUANTITY_DESC : sort) {
            case QUANTITY_DESC -> "quantity DESC, product_name ASC";
            case NAME_ASC -> "product_name ASC";
        };
        String query = "SELECT * FROM stocks ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(query)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Stock> stockList = new ArrayList<>();
                while (rs.next()) {
                    stockList.add(new Stock(
                            rs.getObject("product_id", Integer.class),
                            rs.getString("product_name"),
                            rs.getString("product_desc"),
                            rs.getString("category"),
                            rs.getDouble("unit_bp"),
                            rs.getDouble("unit_sp"),
                            rs.getInt("quantity"),
                            rs.getInt("reorder_lvl")
                    ));
                }
                return stockList;
            }
        } catch (SQLException e) {
            throw new SQLException("Error fetching stock page", e);
        }
    }

    public Stock findFirstByNameExact(String productName) throws SQLException {
        String query = "SELECT * FROM stocks WHERE LOWER(product_name) = LOWER(?) LIMIT 1";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(query)) {
            stmt.setString(1, productName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Stock(
                            rs.getObject("product_id", Integer.class),
                            rs.getString("product_name"),
                            rs.getString("product_desc"),
                            rs.getString("category"),
                            rs.getDouble("unit_bp"),
                            rs.getDouble("unit_sp"),
                            rs.getInt("quantity"),
                            rs.getInt("reorder_lvl")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new SQLException("Error fetching stock by product name", e);
        }
    }

}
