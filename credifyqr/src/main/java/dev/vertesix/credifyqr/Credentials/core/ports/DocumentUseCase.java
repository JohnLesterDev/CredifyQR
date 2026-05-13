package dev.vertesix.credifyqr.Credentials.core.ports;

import dev.vertesix.credifyqr.Credentials.core.domain.DocumentRequest;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Use case boundary for document management within the CredifyQR application.
 */
public interface DocumentUseCase {
    /**
     * Creates a document request for a student.
     *
     * @param studentId the requesting student's id
     * @param type the requested document type
     * @param ipAddress the source IP for audit logging
     * @return created document request
     */
    DocumentRequest requestDocument(String studentId, DocumentType type, String ipAddress);
    /**
     * Uploads a raw PDF for a requested document.
     *
     * @param registrarId id of the uploading registrar
     * @param documentId id of the document request
     * @param fileStream PDF input stream
     * @param filename original uploaded filename
     * @param ipAddress the source IP for audit logging
     */
    void uploadRawDocument(String registrarId, String documentId, InputStream fileStream, String filename, String ipAddress);
    /**
     * Approves a raw document and stamps the PDF with verification metadata.
     *
     * @param directorId id of the approving campus director
     * @param documentId id of the document request
     * @param baseUrl application base url used to build the verification URL
     * @param ipAddress the source IP for audit logging
     */
    void approveAndStampDocument(String directorId, String documentId, String baseUrl, String ipAddress);
    /**
     * Returns documents belonging to a student.
     *
     * @param studentId the student id
     * @return student document DTO list
     */
    List<Map<String, Object>> getStudentDocuments(String studentId);
    /**
     * Returns documents ready for registrar upload.
     *
     * @param registrarId the registrar id
     * @return pending upload DTO list
     */
    List<Map<String, Object>> getPendingUploads(String registrarId);
    /**
     * Returns documents waiting for campus director approval.
     *
     * @param directorId the director id
     * @return pending approval DTO list
     */
    List<Map<String, Object>> getPendingApprovals(String directorId);
    /**
     * Retrieves the actual stamped file for download.
     *
     * @param userId the requesting user's id
     * @param documentId the document request id
     * @return the stamped PDF file
     */
    java.io.File getDocumentFile(String userId, String documentId);
    /**
     * Verifies a document by its identifier and returns metadata.
     *
     * @param documentId the document request id
     * @return document verification metadata
     */
    Map<String, Object> verifyDocument(String documentId);

    /**
     * Retrieves the raw, unstamped document for administrative review.
     *
     * @param userId the requesting admin's id
     * @param documentId the document request id
     * @return the raw PDF file
     */
    java.io.File getRawDocumentFile(String userId, String documentId);
}