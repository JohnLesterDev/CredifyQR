package dev.vertesix.credifyqr.Identity.adapters.outbound.db;

import dev.vertesix.credifyqr.Identity.core.ports.SettingsRepository;
import java.sql.*;
import java.util.Optional;

/**
 * SQLite implementation of {@link SettingsRepository}.
 */
public class SqliteSettingsRepository implements SettingsRepository {

    public SqliteSettingsRepository() {
        initTable();
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)";
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) { throw new RuntimeException("Failed to init settings table", e); }
    }

    @Override
    public Optional<String> getSetting(String key) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(rs.getString("value"));
        } catch (SQLException e) { throw new RuntimeException("DB error fetching setting", e); }
        return Optional.empty();
    }

    @Override
    public void saveSetting(String key, String value) {
        String sql = "INSERT INTO settings(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value=excluded.value";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("DB error saving setting", e); }
    }
}