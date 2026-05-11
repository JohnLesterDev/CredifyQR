package dev.vertesix.credifyqr.Identity.adapters.outbound.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static String dbUrl;

    /**
     * Initializes the DB URL from the App bootstrap.
     */
    public static void init(String url) {
        dbUrl = url;
        try (Connection conn = getConnection()) {
            if (conn != null) {
                logger.info("Database connection established: {}", dbUrl);
            }
        } catch (SQLException e) {
            logger.error("Failed to connect to database during init", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dbUrl == null) {
            throw new IllegalStateException("DatabaseConnection not initialized with a URL.");
        }
        return DriverManager.getConnection(dbUrl);
    }
}