package dev.vertesix.credifyqr.Credentials.core.ports;

import dev.vertesix.credifyqr.Credentials.core.domain.DocumentRequest;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface DocumentUseCase {
    DocumentRequest requestDocument(String studentId, DocumentType type, String ipAddress);
    void uploadRawDocument(String registrarId, String documentId, InputStream fileStream, String filename, String ipAddress);
    void approveAndStampDocument(String directorId, String documentId, String appDomain, String ipAddress);
    
    List<Map<String, Object>> getStudentDocuments(String studentId);
    List<Map<String, Object>> getPendingUploads(String registrarId);
    List<Map<String, Object>> getPendingApprovals(String directorId);
    
    java.io.File getDocumentFile(String userId, String documentId);
    
    Map<String, Object> verifyDocument(String documentId);
}