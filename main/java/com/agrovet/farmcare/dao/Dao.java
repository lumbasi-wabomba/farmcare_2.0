package com.agrovet.farmcare.dao;

import java.sql.SQLException;
import java.util.List;

public interface Dao <T>{
    T get(T t) throws SQLException;
    List<T> getAll()throws SQLException;
    T  save(T t) throws SQLException;
    T update(T t, String[] params) throws  SQLException;
    T delete(String t) throws  SQLException;
}
