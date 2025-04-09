package com.klasurapp.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the connection to the PostgreSQL database.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    
    private static final String URL = "jdbc:postgresql://localhost:5432/klasurapp";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1375"; // Fix the password string - no line breaks
    
    // Use a connection timeout to avoid hanging
    private static final int CONNECTION_TIMEOUT = 5;

    private static Connection connection;

    public static synchronized Connection getConnection() throws SQLException {
        boolean needsNewConnection = (connection == null);
        
        if (!needsNewConnection) {
            // Check if connection is closed or invalid
            try {
                needsNewConnection = connection.isClosed() || !connection.isValid(CONNECTION_TIMEOUT);
            } catch (SQLException e) {
                logger.warn("Error checking connection status, will create a new one", e);
                needsNewConnection = true;
            }
        }
        
        if (needsNewConnection) {
            try {
                // Close the old connection if it exists but is invalid
                if (connection != null) {
                    try { 
                        connection.close(); 
                    } catch (SQLException e) {
                        // Ignore errors when closing an already problematic connection
                    }
                }
                
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                connection.setAutoCommit(true); // Ensure autocommit is enabled by default
                logger.info("Connected to PostgreSQL database");
            } catch (ClassNotFoundException e) {
                logger.error("PostgreSQL JDBC driver not found", e);
                throw new SQLException("PostgreSQL JDBC driver not found", e);
            } catch (SQLException e) {
                logger.error("Failed to connect to database", e);
                throw e;
            }
        }
        
        return connection;
    }

    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;  // Set to null to force a new connection next time
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection", e);
        }
    }
}