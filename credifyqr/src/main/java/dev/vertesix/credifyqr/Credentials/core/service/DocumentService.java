package dev.vertesix.credifyqr.Credentials.core.service;

import dev.vertesix.credifyqr.Credentials.core.domain.DocumentRequest;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentStatus;
import dev.vertesix.credifyqr.Credentials.core.domain.DocumentType;
import dev.vertesix.credifyqr.Credentials.core.ports.DocumentRepository;
import dev.vertesix.credifyqr.Credentials.core.ports.DocumentUseCase;
import dev.vertesix.credifyqr.Credentials.core.ports.PdfStamperPort;
import dev.vertesix.credifyqr.Identity.core.domain.ActionType;
import dev.vertesix.credifyqr.Identity.core.domain.AuditLog;
import dev.vertesix.credifyqr.Identity.core.domain.Role;
import dev.vertesix.credifyqr.Identity.core.domain.User;
import dev.vertesix.credifyqr.Identity.core.ports.AuditRepository;
import dev.vertesix.credifyqr.Identity.core.ports.UserRepository;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DocumentService implements DocumentUseCase {

    private final DocumentRepository documentRepository;
    private final PdfStamperPort pdfStamperPort;
    private final UserRepository userRepository;
    private final AuditRepository auditRepository;

    private static final String STORAGE_RAW = "./storage/raw";
    private static final String STORAGE_STAMPED = "./storage/stamped";

    public DocumentService(DocumentRepository documentRepository, PdfStamperPort pdfStamperPort, UserRepository userRepository, AuditRepository auditRepository) {
        this.documentRepository = documentRepository;
        this.pdfStamperPort = pdfStamperPort;
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        initStorage();
    }

    private void initStorage() {
        try {
            Files.createDirectories(Paths.get(STORAGE_RAW));
            Files.createDirectories(Paths.get(STORAGE_STAMPED));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize storage directories", e);
        }
    }

    private void logAudit(String actorId, ActionType type, String targetId, String ip) {
        auditRepository.save(new AuditLog(UUID.randomUUID().toString(), actorId, type, targetId, ip, System.currentTimeMillis()));
    }

    @Override
    public DocumentRequest requestDocument(String studentId, DocumentType type, String ipAddress) {
        User student = userRepository.findById(studentId).orElseThrow(() -> new SecurityException("User not found"));
        if (student.getRole() != Role.STUDENT) throw new SecurityException("Only students can request documents");

        DocumentRequest request = new DocumentRequest(
            UUID.randomUUID().toString(),
            student.getUsername(), // Using username (Student ID) for easier tracking
            type,
            DocumentStatus.REQUESTED,
            null, null,
            System.currentTimeMillis()
        );

        documentRepository.save(request);
        logAudit(studentId, ActionType.USER_PROVISIONED, request.getId(), ipAddress); // Repurposing ActionType for audit trace
        return request;
    }

    @Override
    public void uploadRawDocument(String registrarId, String documentId, InputStream fileStream, String filename, String ipAddress) {
        User registrar = userRepository.findById(registrarId).orElseThrow();
        if (registrar.getRole() != Role.REGISTRAR_STAFF) throw new SecurityException("Unauthorized upload attempt");

        DocumentRequest request = documentRepository.findById(documentId).orElseThrow(() -> new IllegalArgumentException("Document request not found"));
        if (request.getStatus() != DocumentStatus.REQUESTED) throw new IllegalStateException("Document is not in REQUESTED state");

        try (InputStream is = fileStream) {
            String cleanName = UUID.randomUUID().toString() + "_" + filename.replaceAll("[^a-zA-Z0-9.-]", "_");
            Path targetPath = Paths.get(STORAGE_RAW, cleanName);
            
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            request.setRawFilePath(targetPath.toString());
            request.setStatus(DocumentStatus.UPLOADED);
            documentRepository.save(request);
            
            logAudit(registrarId, ActionType.ROLE_MODIFIED, documentId, ipAddress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save uploaded file to disk", e);
        }
    }

    @Override
    public void approveAndStampDocument(String directorId, String documentId, String appDomain, String ipAddress) {
        User director = userRepository.findById(directorId).orElseThrow();
        if (director.getRole() != Role.CAMPUS_DIRECTOR) throw new SecurityException("Unauthorized approval attempt");

        DocumentRequest request = documentRepository.findById(documentId).orElseThrow(() -> new IllegalArgumentException("Document request not found"));
        if (request.getStatus() != DocumentStatus.UPLOADED) throw new IllegalStateException("Document is not awaiting approval");

        try {
            File rawFile = new File(request.getRawFilePath());
            if (!rawFile.exists()) throw new IllegalStateException("Raw file missing from disk");

            String stampedFilename = "STAMPED_" + rawFile.getName();
            File stampedFile = new File(STORAGE_STAMPED, stampedFilename);

            // Constructing verification URL
            String verifyUrl = "https://" + appDomain + "/verify/" + request.getId();

            // Trigger Phase 2 Engine
            pdfStamperPort.stampPdf(rawFile, stampedFile, verifyUrl);

            request.setStampedFilePath(stampedFile.getPath());
            request.setStatus(DocumentStatus.APPROVED);
            documentRepository.save(request);

            logAudit(directorId, ActionType.ROLE_MODIFIED, documentId, ipAddress);
        } catch (Exception e) {
            throw new RuntimeException("PDF Stamping engine failed", e);
        }
    }

    @Override
    public List<Map<String, Object>> getStudentDocuments(String studentId) {
        User student = userRepository.findById(studentId).orElseThrow();
        return documentRepository.findByStudentId(student.getUsername()).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getPendingUploads(String registrarId) {
        return documentRepository.findByStatus(DocumentStatus.REQUESTED).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getPendingApprovals(String directorId) {
        return documentRepository.findByStatus(DocumentStatus.UPLOADED).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public File getDocumentFile(String userId, String documentId) {
        DocumentRequest request = documentRepository.findById(documentId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        if (user.getRole() == Role.STUDENT && !user.getUsername().equals(request.getStudentId())) {
            throw new SecurityException("Unauthorized document access");
        }

        if (request.getStatus() != DocumentStatus.APPROVED || request.getStampedFilePath() == null) {
            throw new IllegalStateException("Document is not yet approved and stamped");
        }

        File file = new File(request.getStampedFilePath());
        if (!file.exists()) throw new IllegalStateException("File missing from disk");
        
        return file;
    }

    private Map<String, Object> mapToDto(DocumentRequest req) {
        return Map.of(
            "id", req.getId(),
            "studentId", req.getStudentId(),
            "type", req.getType().name(),
            "status", req.getStatus().name(),
            "timestamp", req.getTimestamp()
        );
    }

    @Override
    public Map<String, Object> verifyDocument(String documentId) {
        DocumentRequest request = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or forged document ID"));

        if (request.getStatus() != DocumentStatus.APPROVED) {
            throw new IllegalStateException("Document is pending or suspended");
        }

        User student = userRepository.findByUsername(request.getStudentId())
            .orElseThrow(() -> new IllegalStateException("Associated student identity missing"));

        return Map.of(
            "studentName", student.getFullName(),
            "documentType", request.getType().name(),
            "issueDate", request.getTimestamp(),
            "status", request.getStatus().name()
        );
    }
}