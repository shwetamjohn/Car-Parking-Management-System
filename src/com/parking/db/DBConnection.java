package com.parking.db;

import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection - Database connection manager for Dockerized Oracle Database.
 * Connects to Oracle 23c Free container on localhost:1522/FREEPDB1.
 */
public class DBConnection {

    // Oracle JDBC Connection Parameters
    private static final String URL = "jdbc:oracle:thin:@localhost:1522/FREEPDB1";
    private static final String USER = "PARKING";
    private static final String PASS = "ParkingPass123";

    static {
        try {
            // Load Oracle JDBC Driver
            Class.forName("oracle.jdbc.OracleDriver");
            System.out.println("[DBConnection] Oracle JDBC Driver Registered successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] Error: Oracle JDBC Driver not found!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "Oracle JDBC Driver (ojdbc11) not found on classpath!\nPlease check your project dependencies.",
                "Database Driver Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Private constructor for utility class
    private DBConnection() {}

    /**
     * Establishes and returns a fresh Connection to the Oracle Database.
     * 
     * @return active Connection object
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Failed to connect to Oracle Database at: " + URL);
            System.err.println("SQL State: " + e.getSQLState() + ", Error Code: " + e.getErrorCode());
            e.printStackTrace();
            
            JOptionPane.showMessageDialog(
                null,
                "Cannot connect to Oracle Database!\n\n" +
                "URL: " + URL + "\n" +
                "User: " + USER + "\n" +
                "Error: " + e.getMessage() + "\n\n" +
                "Please verify that Docker Oracle container is running on port 1522.",
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
            throw e;
        }
    }

    /**
     * Utility method to test database connectivity.
     * 
     * @return true if connected successfully, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
