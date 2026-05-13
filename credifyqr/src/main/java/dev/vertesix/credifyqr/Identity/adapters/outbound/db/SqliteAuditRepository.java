package dev.vertesix.credifyqr.Identity.adapters.outbound.db;

import dev.vertesix.credifyqr.Identity.core.domain.AuditLog;
import dev.vertesix.credifyqr.Identity.core.ports.AuditRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite implementation of {@link AuditRepository}.
 *
 * <p>Persists audit log entries for security and compliance tracing.</p>
 */
public class SqliteAuditRepository implements AuditRepository {

    public SqliteAuditRepository() {
        initTable();
    }

    private void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id TEXT PRIMARY KEY,
                actor_id TEXT,
                action_type TEXT NOT NULL,
                target_id TEXT,
                ip_address TEXT,
                timestamp INTEGER NOT NULL
            );
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize audit_logs table", e);
        }
    }

    @Override
    public void save(AuditLog log) {
        String sql = "INSERT INTO audit_logs(id, actor_id, action_type, target_id, ip_address, timestamp) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getId());
            pstmt.setString(2, log.getActorId());
            pstmt.setString(3, log.getActionType().name());
            pstmt.setString(4, log.getTargetId());
            pstmt.setString(5, log.getIpAddress());
            pstmt.setLong(6, log.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving audit log", e);
        }
    }
}