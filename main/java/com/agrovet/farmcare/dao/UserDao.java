package com.agrovet.farmcare.dao;

import com.agrovet.farmcare.models.Users;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserDao implements Dao <Users>{
    @Override
    public Users get(Users users) throws SQLException {
        String query = "SELECT * FROM users WHERE username=?";
        try(Connection myConn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myConn.prepareStatement(query);

            stmt.setString(1, users.getUsername());
            ResultSet rs = stmt.executeQuery();
            if  (rs.next()) {
                return new Users(
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("id_no"),
                rs.getString("role")
                );
            }
            return null;

        }catch (SQLException e){
            e.printStackTrace();
            throw new SQLException("Error fetching user");
        }
    }

    @Override
    public List<Users> getAll() throws SQLException {
        String query = "SELECT * FROM users";
        try(Connection myConn = DatabaseConnection.getConnection();
            PreparedStatement stmt = myConn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            List<Users> usersList = new java.util.ArrayList<>();
            while (rs.next()) {
                usersList.add(new Users(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("id_no"),
                        rs.getString("role")
                ));
            }
            return usersList;
        } catch (SQLException e) {
            throw new SQLException("Error fetching all users", e);
        }
    }

    @Override
    public Users save(Users users) throws SQLException {
        String sql = "INSERT INTO users (username, password, email, first_name, last_name, id_no, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try(Connection myConn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myConn.prepareStatement(sql);
            stmt.setString(1, users.getUsername());
            stmt.setString(2, users.getPassword());
            stmt.setString(3, users.getEmail());
            stmt.setString(4, users.getFirstName());
            stmt.setString(5, users.getLastName());
            stmt.setString(6, users.getIDNumber());
            stmt.setString(7, users.getRole());
            stmt.executeUpdate();

            return ("successfully saved user: " + users.getUsername() + " : " +  users.getUsername()) != null ? users : null;
        } catch (SQLException e) {
            throw new SQLException("Error saving user: " + users.getUsername() + " : " +  users.getUsername(), e);
            }
        }

    @Override
    public Users update(Users users, String[] params) throws SQLException {
        String update = "UPDATE users SET password=?, email=?, first_name=?, last_name=?, id_no=?, role=? WHERE username=?";
        try(Connection myConn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myConn.prepareStatement(update);
            if (params == null || params.length < 6) {
                throw new SQLException("Invalid update parameters for user: " + users.getUsername());
            }
            stmt.setString(1, params[0]);
            stmt.setString(2, params[1]);
            stmt.setString(3, params[2]);
            stmt.setString(4, params[3]);
            stmt.setString(5, params[4]);
            stmt.setString(6, params[5]);
            stmt.setString(7, users.getUsername());

            stmt.executeUpdate();
            return users;
        } catch ( SQLException e){
            throw new SQLException("Error updating user: " + users.getUsername(), e);
        }
    }

    @Override
    public Users delete(String username) throws SQLException {
        String delete_query = "DELETE FROM users WHERE username=?";
        try(Connection myConn = DatabaseConnection.getConnection()){
            PreparedStatement stmt = myConn.prepareStatement(delete_query);
            stmt.setString(1, username);
            stmt.executeUpdate();
            return ("successfully deleted user with username: " + username) != null ? new Users() : null;
        } catch (SQLException e) {
            throw new SQLException("Error deleting user with username: " + username, e);
    }
}





}
