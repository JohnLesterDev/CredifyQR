package dev.vertesix.credifyqr.Credentials.adapters.outbound.db;

import dev.vertesix.credifyqr.Credentials.core.domain.DocumentRequest;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentStatus;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentType;
import dev.vertesix.credifyqr.Credentials.core.ports.DocumentRepository;
import dev.vertesix.credifyqr.Identity.adapters.outbound.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link DocumentRepository}.
 *
 * Stores document requests in a local SQLite database and provides
 * lookup operations by id, student id, and status.
 */
public class SqliteDocumentRepository implements DocumentRepository {

    /**
     * Creates the repository and ensures the document_requests table exists.
     */
    public SqliteDocumentRepository() {
        initTable();
    }

    /**
     * Ensures the backing table is created for document requests.
     */
    private void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS document_requests (
                id TEXT PRIMARY KEY,
                student_id TEXT NOT NULL,
                type TEXT NOT NULL,
                status TEXT NOT NULL,
                raw_file_path TEXT,
                stamped_file_path TEXT,
                timestamp INTEGER NOT NULL
            );
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize document_requests table", e);
        }
    }

    /**
     * Saves or updates a document request in the database.
     *
     * @param request the document request to persist
     */
    @Override
    public void save(DocumentRequest request) {
        String sql = """
            INSERT INTO document_requests(id, student_id, type, status, raw_file_path, stamped_file_path, timestamp) 
            VALUES(?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET 
                status=excluded.status,
                raw_file_path=excluded.raw_file_path,
                stamped_file_path=excluded.stamped_file_path;
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, request.getId());
            pstmt.setString(2, request.getStudentId());
            pstmt.setString(3, request.getType().name());
            pstmt.setString(4, request.getStatus().name());
            pstmt.setString(5, request.getRawFilePath());
            pstmt.setString(6, request.getStampedFilePath());
            pstmt.setLong(7, request.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error saving document request", e);
        }
    }

    /**
     * Finds a document request by its unique identifier.
     *
     * @param id the request id
     * @return optional document request if found
     */
    @Override
    public Optional<DocumentRequest> findById(String id) {
        String sql = "SELECT * FROM document_requests WHERE id = ?";
        return querySingle(sql, id);
    }

    /**
     * Finds document requests for a specific student.
     *
     * @param studentId the identifier of the student
     * @return list of matching document requests
     */
    @Override
    public List<DocumentRequest> findByStudentId(String studentId) {
        String sql = "SELECT * FROM document_requests WHERE student_id = ? ORDER BY timestamp DESC";
        return queryList(sql, studentId);
    }

    /**
     * Finds document requests by status.
     *
     * @param status the request status to filter by
     * @return list of matching document requests
     */
    @Override
    public List<DocumentRequest> findByStatus(DocumentStatus status) {
        String sql = "SELECT * FROM document_requests WHERE status = ? ORDER BY timestamp ASC";
        return queryList(sql, status.name());
    }

    /**
     * Executes a query expected to return a single row.
     *
     * @param sql the SQL statement
     * @param param the query parameter
     * @return optional mapped document request
     */
    private Optional<DocumentRequest> querySingle(String sql, String param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("DB error", e);
        }
        return Optional.empty();
    }

    /**
     * Executes a query that may return multiple rows.
     *
     * @param sql the SQL statement
     * @param param the query parameter
     * @return mapped document request list
     */
    private List<DocumentRequest> queryList(String sql, String param) {
        List<DocumentRequest> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, param);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("DB error", e);
        }
        return list;
    }

    /**
     * Maps a result set row to a {@link DocumentRequest}.
     *
     * @param rs the result set row
     * @return document request instance
     * @throws SQLException if reading the row fails
     */
    private DocumentRequest mapRow(ResultSet rs) throws SQLException {
        return new DocumentRequest(
            rs.getString("id"),
            rs.getString("student_id"),
            DocumentType.valueOf(rs.getString("type")),
            DocumentStatus.valueOf(rs.getString("status")),
            rs.getString("raw_file_path"),
            rs.getString("stamped_file_path"),
            rs.getLong("timestamp")
        );
    }
}