package com.agrovet.farmcare.dao;

import com.agrovet.farmcare.models.Sales;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class SalesDao  implements  Dao<Sales> {

    @Override
    public Sales get(Sales sales) throws SQLException {
        String sql = "SELECT * FROM sales WHERE sale_id = ?";
        try (Connection myconn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myconn.prepareStatement(sql);
            stmt.setInt(1, sales.getSalesID());
            ResultSet rs = stmt.executeQuery();

            if  (rs.next()) {
                return  new Sales(
                        rs.getInt("sale_id"),
                        rs.getString("username"),
                        rs.getString("cust_name"),
                        rs.getString("prod_name"),
                        rs.getDouble("unit_price"),
                        rs.getInt("quantity"),
                        rs.getDouble("total"),
                        rs.getDate("sale_date")
                );
            }
            return null;
        }
    }

    @Override
    public List<Sales> getAll() throws SQLException {
        String sql = "SELECT * FROM sales";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Sales> salesList = new java.util.ArrayList<>();
            while (rs.next()) {
                salesList.add(new Sales(
                        rs.getInt("sale_id"),
                        rs.getString("username"),
                        rs.getString("cust_name"),
                        rs.getString("prod_name"),
                        rs.getDouble("unit_price"),
                        rs.getInt("quantity"),
                        rs.getDouble("total"),
                        rs.getDate("sale_date")
                ));
            }
            return salesList;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all sales", e);
        }
    }

    @Override
    public Sales save(Sales sales) throws SQLException {
        String sql = "INSERT INTO sales (username, cust_name, prod_name, unit_price, quantity, total, sale_date) VALUES (?,?,?,?,?,?,?)";
        try (Connection myconn = DatabaseConnection.getConnection();
             PreparedStatement stmt = myconn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, sales.getUsername());
            stmt.setString(2, sales.getCustomerName());
            stmt.setString(3, sales.getProductName());
            stmt.setDouble(4, sales.getUnitPrice());
            stmt.setInt(5, sales.getQuantity());
            stmt.setDouble(6, sales.getTotal());
            java.util.Date date = sales.getSaleDate();
            if (date == null) date = new java.util.Date();
            stmt.setDate(7, new java.sql.Date(date.getTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating sale failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    sales.setSalesID(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating sale failed, no ID obtained.");
                }
            }
            return sales;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error saving sale", e);
        }
    }

    @Override
    public Sales update(Sales sales, String[] params) throws SQLException {
        final String updateWithDate = "UPDATE sales SET username = ?, cust_name = ?, prod_name = ?, unit_price = ?, quantity = ?, total = ?, sale_date = ? WHERE sale_id = ?";
        final String updateNoDate = "UPDATE sales SET username = ?, cust_name = ?, prod_name = ?, unit_price = ?, quantity = ?, total = ? WHERE sale_id = ?";
        try (Connection myconn = DatabaseConnection.getConnection()){
            if (params == null || params.length < 6) {
                throw new SQLException("Invalid update parameters for sale: " + sales.getSalesID());
            }
            boolean hasDate = params.length >= 7 && params[6] != null && !params[6].trim().isEmpty();
            PreparedStatement stmt = myconn.prepareStatement(hasDate ? updateWithDate : updateNoDate);
            stmt.setString(1, params[0]);
            stmt.setString(2, params[1]);
            stmt.setString(3, params[2]);
            stmt.setDouble(4, Double.parseDouble(params[3]));
            stmt.setInt(5, Integer.parseInt(params[4]));
            stmt.setDouble(6, Double.parseDouble(params[5]));
            if (hasDate) {
                stmt.setDate(7, java.sql.Date.valueOf(params[6].trim()));
                stmt.setInt(8, sales.getSalesID());
            } else {
                stmt.setInt(7, sales.getSalesID());
            }

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No sale updated for id: " + sales.getSalesID());
            }
            return sales;
        } catch (SQLException e) {
            throw new SQLException("Error updating sale: " + sales.getSalesID(), e);
        }
    }

    @Override
    public Sales delete(String saleID) throws SQLException {
        String delete = "DELETE FROM sales WHERE sale_id = ?";
        try (Connection myconn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myconn.prepareStatement(delete);

            stmt.setInt(1, Integer.parseInt(saleID));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException( "Error while deleting the sale" +e);
        }
        return null;
    }
}
