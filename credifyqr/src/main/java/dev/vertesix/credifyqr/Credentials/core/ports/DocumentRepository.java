package dev.vertesix.credifyqr.Credentials.core.ports;

import dev.vertesix.credifyqr.Credentials.core.domain.DocumentRequest;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for persisting document requests.
 */
public interface DocumentRepository {
    /**
     * Saves or updates a document request.
     *
     * @param request the document request to persist
     */
    void save(DocumentRequest request);
    /**
     * Finds a document request by id.
     *
     * @param id the request identifier
     * @return optional document request if found
     */
    Optional<DocumentRequest> findById(String id);
    /**
     * Finds document requests for a specific student.
     *
     * @param studentId the student identifier
     * @return list of matching document requests
     */
    List<DocumentRequest> findByStudentId(String studentId);
    /**
     * Finds document requests by status.
     *
     * @param status the request status to filter by
     * @return list of matching document requests
     */
    List<DocumentRequest> findByStatus(DocumentStatus status);
}