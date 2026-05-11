package dev.vertesix.credifyqr.Identity.adapters.outbound.db;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;

import java.sql.*;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {
    private final String dbUrl;

    public SqliteUserRepository(String dbUrl) {
        this.dbUrl = dbUrl;
        initTable();
    }

    private void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                birthdate TEXT,
                is_claimed INTEGER NOT NULL,
                needs_password_reset INTEGER NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1
            );
            """;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize users table", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during findByUsername", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during findById", e);
        }
        return Optional.empty();
    }

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users(id, username, password_hash, role, birthdate, is_claimed, needs_password_reset, is_active) VALUES(?,?,?,?,?,?,?,?) " +
                     "ON CONFLICT(id) DO UPDATE SET password_hash=excluded.password_hash, role=excluded.role, is_claimed=excluded.is_claimed, needs_password_reset=excluded.needs_password_reset, is_active=excluded.is_active";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole().name());
            pstmt.setString(5, user.getBirthdate());
            pstmt.setInt(6, user.isClaimed() ? 1 : 0);
            pstmt.setInt(7, user.needsPasswordReset() ? 1 : 0);
            pstmt.setInt(8, user.isActive() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    private User mapToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getString("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")),
            rs.getString("birthdate"),
            rs.getInt("is_claimed") == 1,
            rs.getInt("needs_password_reset") == 1,
            rs.getInt("is_active") == 1
        );
    }
}