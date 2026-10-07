package com.parkingsystem.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton utility class for obtaining an Oracle DB connection.
 * All DAO classes call DBConnection.getConnection() instead of
 * managing their own driver/URL setup.
 */
public class DBConnection {

    // ----- Connection constants for Docker Oracle 23c Free -----
    public static final String URL  = "jdbc:oracle:thin:@localhost:1522/FREEPDB1";
    public static final String USER = "PARKING";
    public static final String PASS = "ParkingPass123";

    // Private constructor — utility class, not meant to be instantiated
    private DBConnection() {}

    /**
     * Returns a fresh JDBC connection to the Oracle XE database.
     * Caller is responsible for closing the connection.
     *
     * @return active {@link Connection}
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load Oracle JDBC thin driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "Oracle JDBC Driver not found on classpath. " +
                "Add ojdbc8.jar (or ojdbc11.jar) to your build path.", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
