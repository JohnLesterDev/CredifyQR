package dev.vertesix.credifyqr.Credentials.core.ports;

import dev.vertesix.credifyqr.Credentials.core.domain.DocumentRequest;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentStatus;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {
    void save(DocumentRequest request);
    Optional<DocumentRequest> findById(String id);
    List<DocumentRequest> findByStudentId(String studentId);
    List<DocumentRequest> findByStatus(DocumentStatus status);
}