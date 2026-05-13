package dev.vertesix.credifyqr.Credentials.core.domain;

/**
 * Represents a document request in the credential issuance workflow.
 *
 * <p>Tracks status, file paths, the requesting student, and the request timestamp.</p>
 */
public class DocumentRequest {
    private final String id;
    private final String studentId;
    private final DocumentType type;
    private DocumentStatus status;
    private String rawFilePath;
    private String stampedFilePath;
    private final long timestamp;

    /**
     * Creates a new document request.
     *
     * @param id unique request identifier
     * @param studentId student identifier requesting the document
     * @param type document type being requested
     * @param status current request status
     * @param rawFilePath filesystem path to the uploaded raw PDF
     * @param stampedFilePath filesystem path to the stamped PDF
     * @param timestamp creation time in milliseconds
     */
    public DocumentRequest(String id, String studentId, DocumentType type, DocumentStatus status, String rawFilePath, String stampedFilePath, long timestamp) {
        this.id = id;
        this.studentId = studentId;
        this.type = type;
        this.status = status;
        this.rawFilePath = rawFilePath;
        this.stampedFilePath = stampedFilePath;
        this.timestamp = timestamp;
    }

    /** @return unique request identifier */
    public String getId() { return id; }
    /** @return student identifier associated with the request */
    public String getStudentId() { return studentId; }
    /** @return type of document requested */
    public DocumentType getType() { return type; }
    /** @return current status of the document request */
    public DocumentStatus getStatus() { return status; }
    /** @param status new status for the document request */
    public void setStatus(DocumentStatus status) { this.status = status; }
    /** @return path to the raw uploaded PDF file */
    public String getRawFilePath() { return rawFilePath; }
    /** @param rawFilePath filesystem path to the raw PDF */
    public void setRawFilePath(String rawFilePath) { this.rawFilePath = rawFilePath; }
    /** @return path to the stamped PDF file */
    public String getStampedFilePath() { return stampedFilePath; }
    /** @param stampedFilePath filesystem path to the stamped PDF */
    public void setStampedFilePath(String stampedFilePath) { this.stampedFilePath = stampedFilePath; }
    /** @return creation timestamp in milliseconds */
    public long getTimestamp() { return timestamp; }
}