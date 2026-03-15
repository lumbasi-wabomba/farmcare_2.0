package com.agrovet.farmcare.dao;

import com.agrovet.farmcare.models.Expense;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class ExpenseDao implements Dao<Expense> {

    @Override
    public Expense get(Expense expense) throws SQLException {
        String sql = "select * from expenses where exp_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, expense.getExpenseID());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Expense(
                        rs.getString("username"),
                        rs.getInt("exp_id"),
                        rs.getDate("exp_date"),
                        rs.getString("exp_type"),
                        rs.getString("exp_desc"),
                        rs.getDouble("total")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new SQLException("Error fetching expense: " + expense.getExpenseID(), e);
        }
    }

    @Override
    public java.util.List<Expense> getAll() {
        String sql = "SELECT * FROM expenses";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            java.util.List<Expense> expenseList = new java.util.ArrayList<>();
            while (rs.next()) {
                expenseList.add(new Expense(
                        rs.getString("username"),
                        rs.getInt("exp_id"),
                        rs.getDate("exp_date"),
                        rs.getString("exp_type"),
                        rs.getString("exp_desc"),
                        rs.getDouble("total")
                ));
            }
            return expenseList;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all expenses", e);
        }
    }

    @Override
    public Expense save(Expense expense) {
        String sql = "INSERT INTO expenses (username, exp_date, exp_type, exp_desc, total) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, expense.getUsername());
            stmt.setDate(2, new Date(expense.getExpenseDate().getTime()));
            stmt.setString(3, expense.getExpenseType());
            stmt.setString(4, expense.getExpenseDescription());
            stmt.setDouble(5, expense.getTotal());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating expense failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    expense.setExpenseID(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating expense failed, no ID obtained.");
                }
            }
            return expense;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving expense: " + expense.getExpenseID(), e);
        }
    }

    @Override
    public Expense update(Expense expense, String[] params) {
        String sql = "UPDATE expenses SET username = ?, exp_date = ?, exp_type = ?, exp_desc = ?, total = ? WHERE exp_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, params[0]);
            stmt.setDate(2, Date.valueOf(params[1]));
            stmt.setString(3, params[2]);
            stmt.setString(4, params[3]);
            stmt.setDouble(5, Double.parseDouble(params[4]));
            stmt.setInt(6, expense.getExpenseID());
            stmt.executeUpdate();
            return expense;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating expense: " + expense.getExpenseID(), e);
        }
    }

    @Override
    public Expense delete(String id) {
        String sql = "DELETE FROM expenses WHERE exp_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(id));
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting expense failed, no rows affected.");
            }
            return null; // Return null to indicate successful deletion
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting expense with ID: " + id, e);
        }
    }
}
