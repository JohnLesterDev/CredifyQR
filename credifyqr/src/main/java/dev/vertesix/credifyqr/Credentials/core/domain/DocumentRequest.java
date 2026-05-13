package dev.vertesix.credifyqr.Credentials.core.domain;

public class DocumentRequest {
    private final String id;
    private final String studentId;
    private final DocumentType type;
    private DocumentStatus status;
    private String rawFilePath;
    private String stampedFilePath;
    private final long timestamp;

    public DocumentRequest(String id, String studentId, DocumentType type, DocumentStatus status, String rawFilePath, String stampedFilePath, long timestamp) {
        this.id = id;
        this.studentId = studentId;
        this.type = type;
        this.status = status;
        this.rawFilePath = rawFilePath;
        this.stampedFilePath = stampedFilePath;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public DocumentType getType() { return type; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public String getRawFilePath() { return rawFilePath; }
    public void setRawFilePath(String rawFilePath) { this.rawFilePath = rawFilePath; }
    public String getStampedFilePath() { return stampedFilePath; }
    public void setStampedFilePath(String stampedFilePath) { this.stampedFilePath = stampedFilePath; }
    public long getTimestamp() { return timestamp; }
}