package dev.vertesix.credifyqr.Identity.adapters.outbound.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the application database connection URL and provides JDBC connections.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static String dbUrl;

    /**
     * Initializes the DB URL from the App bootstrap.
     *
     * @param url JDBC connection URL
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

    /**
     * Opens a JDBC connection to the configured database.
     *
     * @return JDBC connection instance
     * @throws SQLException when a connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        if (dbUrl == null) {
            throw new IllegalStateException("DatabaseConnection not initialized with a URL.");
        }
        return DriverManager.getConnection(dbUrl);
    }
}