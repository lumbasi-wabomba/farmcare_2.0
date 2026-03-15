package com.agrovet.farmcare.dao;

import com.agrovet.farmcare.models.Purchases;
import org.springframework.stereotype.Repository;

import java.sql.*;


@Repository
public class PurchasesDao implements Dao<Purchases> {
    @Override
    public Purchases get(Purchases purchases) throws SQLException {
        String sql = "SELECT * FROM purchases WHERE purchase_id = ?";
        try(Connection myconn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myconn.prepareStatement(sql);
            stmt.setInt(1, purchases.getPurchaseID());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Purchases(
                        rs.getInt("purchase_id"),
                        rs.getString("username"),
                        rs.getString("prod_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_bp"),
                        rs.getString("supplier_name"),
                        rs.getDate("purchase_date")
                );
            }
            return null;
        }
    }

    @Override
    public java.util.List<Purchases> getAll() {
        String sql = "SELECT * FROM purchases";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            java.util.List<Purchases> purchaseList = new java.util.ArrayList<>();
            while (rs.next()) {
                purchaseList.add(new Purchases(
                        rs.getInt("purchase_id"),
                        rs.getString("username"),
                        rs.getString("prod_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_bp"),
                        rs.getString("supplier_name"),
                        rs.getDate("purchase_date")
                ));
            }
            return purchaseList;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all purchases", e);
        }
    }

    @Override
    public Purchases save(Purchases purchases) {
        String sql = "INSERT INTO purchases (username, prod_name, quantity, unit_bp, supplier_name, purchase_date) VALUES (?,?,?,?,?,?)";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, purchases.getUsername());
            stmt.setString(2, purchases.getProductName());
            stmt.setInt(3, purchases.getQuantity());
            stmt.setDouble(4, purchases.getBuyingPrice());
            stmt.setString(5, purchases.getSupplierName());
            java.util.Date date = purchases.getPurchaseDate();
            if (date == null) date = new java.util.Date();
            stmt.setDate(6, new java.sql.Date(date.getTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating purchase failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    purchases.setPurchaseID(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating purchase failed, no ID obtained.");
                }
            }
            return purchases;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving purchase", e);
        }
    }

    @Override
    public Purchases update(Purchases purchases, String[] params) throws SQLException {
        String sql = "UPDATE purchases SET username=?, prod_name=?, quantity=?, unit_bp=?, supplier_name=?, purchase_date=? WHERE purchase_id=?";
        try (Connection myConn = DatabaseConnection.getConnection()){

            PreparedStatement stmt = myConn.prepareStatement(sql);
            stmt.setString(1, params[0]);
            stmt.setString(2, params[1]);
            stmt.setInt(3, Integer.parseInt(params[2]));
            stmt.setDouble(4, Double.parseDouble(params[3]));
            stmt.setString(5, params[4]);
            stmt.setDate(6, java.sql.Date.valueOf(params[5]));
            stmt.setInt(7, purchases.getPurchaseID());
            stmt.executeUpdate();
            return purchases;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating purchase", e);
        }
    }

    @Override
    public Purchases delete(String id)  throws SQLException {
        String sql = "DELETE FROM purchases WHERE purchase_id = ?";
        try(Connection conn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();
        }catch (SQLException e){
            throw new RuntimeException("Error deleting purchase", e);
        }
        return null;
    }
}
