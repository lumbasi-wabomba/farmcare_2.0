package com.agrovet.farmcare.service;

import com.agrovet.farmcare.dao.UserDao;
import com.agrovet.farmcare.models.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    public Users authenticateUser(String username, String password) throws SQLException {
        Users user = new Users();
        user.setUsername(username);
        Users foundUser = userDao.get(user);
        if (foundUser != null && foundUser.getPassword().equals(password)) {
            return foundUser;
        }
        return null;
    }

    public Users registerUser(Users user) throws SQLException {
        // Check if user already exists
        Users existingUser = new Users();
        existingUser.setUsername(user.getUsername());
        if (userDao.get(existingUser) != null) {
            throw new SQLException("User already exists");
        }
        return userDao.save(user);
    }

    public Users getUserByUsername(String username) throws SQLException {
        Users user = new Users();
        user.setUsername(username);
        return userDao.get(user);
    }

    public List<Users> getAllUsers() throws SQLException {
        return userDao.getAll();
    }

    public Users updateUser(Users user, String[] params) throws SQLException {
        return userDao.update(user, params);
    }

    public Users deleteUser(String username) throws SQLException {
        return userDao.delete(username);
    }
}
