package dev.vertesix.credifyqr.Identity.adapters.outbound.db;

import dev.vertesix.credifyqr.Identity.core.ports.TokenBlacklistRepository;
import java.sql.*;

public class SqliteBlacklistRepository implements TokenBlacklistRepository {
    private final String dbUrl;

    public SqliteBlacklistRepository(String dbUrl) {
        this.dbUrl = dbUrl;
        initTable();
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS token_blacklist (jti TEXT PRIMARY KEY, expires_at INTEGER)";
        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void blacklist(String jti, long expiresAt) {
        String sql = "INSERT OR IGNORE INTO token_blacklist(jti, expires_at) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jti);
            pstmt.setLong(2, expiresAt);
            pstmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        String sql = "SELECT 1 FROM token_blacklist WHERE jti = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jti);
            return pstmt.executeQuery().next();
        } catch (SQLException e) { return true; }
    }
}