package dev.vertesix.credifyqr.Identity.adapters.outbound.db;

import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link UserRepository}.
 *
 * <p>Persists user identities and supports lookups by username, id, and email.</p>
 */
public class SqliteUserRepository implements UserRepository {
    
    public SqliteUserRepository() {
        initTable();
    }

    private void initTable() {
        // Changes for rate-limiting implementation: added failed_login_attempts and lockout_until columns
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                birthdate TEXT,
                first_name TEXT,
                last_name TEXT,
                middle_initial TEXT,
                is_claimed INTEGER NOT NULL,
                needs_password_reset INTEGER NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                is_approved INTEGER NOT NULL DEFAULT 1,
                failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                lockout_until INTEGER NOT NULL DEFAULT 0
            );
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize users table", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return queryUser(sql, username);
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return queryUser(sql, id);
    }

    public Optional<User> findByClaimDetails(String username, String email, String birthdate) {
        String sql = "SELECT * FROM users WHERE username = ? AND email = ? AND birthdate = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, birthdate);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(mapToUser(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Database error during claim lookup", e);
        }
        return Optional.empty();
    }

    @Override
    public void save(User user) {
        // Changes for rate-limiting implementation: mapped failed_login_attempts and lockout_until in INSERT and UPDATE
        String sql = """
            INSERT INTO users(id, username, email, password_hash, role, birthdate, first_name, last_name, middle_initial, is_claimed, needs_password_reset, is_active, is_approved, failed_login_attempts, lockout_until) 
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET 
                password_hash=excluded.password_hash, 
                email=excluded.email,
                first_name=excluded.first_name,
                last_name=excluded.last_name,
                middle_initial=excluded.middle_initial,
                is_claimed=excluded.is_claimed, 
                needs_password_reset=excluded.needs_password_reset, 
                is_active=excluded.is_active,
                is_approved=excluded.is_approved,
                failed_login_attempts=excluded.failed_login_attempts,
                lockout_until=excluded.lockout_until;
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPasswordHash());
            pstmt.setString(5, user.getRole().name());
            pstmt.setString(6, user.getBirthdate());
            pstmt.setString(7, user.getFirstName());
            pstmt.setString(8, user.getLastName());
            pstmt.setString(9, user.getMiddleInitial());
            pstmt.setInt(10, user.isClaimed() ? 1 : 0);
            pstmt.setInt(11, user.needsPasswordReset() ? 1 : 0);
            pstmt.setInt(12, user.isActive() ? 1 : 0);
            pstmt.setInt(13, user.isApproved() ? 1 : 0);
            pstmt.setInt(14, user.getFailedLoginAttempts());
            pstmt.setLong(15, user.getLockoutUntil());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    private Optional<User> queryUser(String sql, String param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(mapToUser(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
        return Optional.empty();
    }

    private User mapToUser(ResultSet rs) throws SQLException {
        // Changes for rate-limiting implementation: extracting failed_login_attempts and lockout_until into the constructor
        return new User(
            rs.getString("id"),
            rs.getString("username"),
            rs.getString("email"), 
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")),
            rs.getString("birthdate"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("middle_initial"),
            rs.getInt("is_claimed") == 1,
            rs.getInt("needs_password_reset") == 1,
            rs.getInt("is_active") == 1,
            rs.getInt("is_approved") == 1,
            rs.getInt("failed_login_attempts"),
            rs.getLong("lockout_until")
        );
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(mapToUser(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Database error during findByEmail", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY role, username";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during findAll", e);
        }
        return users;
    }
}