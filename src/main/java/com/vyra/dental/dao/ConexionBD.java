package com.vyra.dental.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL = "jdbc:sqlite:data/vyra.db";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}