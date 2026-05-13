package dev.vertesix.credifyqr.Credentials.adapters.inbound.web;

import dev.vertesix.credifyqr.Credentials.core.domain.DocumentType;
import dev.vertesix.credifyqr.Credentials.core.ports.DocumentUseCase;
import dev.vertesix.credifyqr.Identity.core.ports.IdentityUseCase;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Controller for handling document-related operations in the CredifyQR system.
 * Manages requests, uploads, approvals, downloads, and verifications of documents.
 */
public class DocumentController {

    private final DocumentUseCase documentUseCase;
    private final IdentityUseCase identityUseCase; // To fetch current domain for QR

    /**
     * Constructs a new DocumentController with the given use cases.
     * @param documentUseCase the document use case
     * @param identityUseCase the identity use case
     */
    public DocumentController(DocumentUseCase documentUseCase, IdentityUseCase identityUseCase) {
        this.documentUseCase = documentUseCase;
        this.identityUseCase = identityUseCase;
    }

    /**
     * Registers the routes for document operations with the Javalin app.
     * @param app the Javalin application instance
     */
    public void registerRoutes(Javalin app) {
        app.post("/api/docs/request", this::requestDocument);
        app.get("/api/docs/student", this::getStudentDocs);
        
        app.get("/api/docs/registrar/pending", this::getRegistrarPending);
        app.post("/api/docs/{id}/upload", this::uploadDocument);
        
        app.get("/api/docs/director/pending", this::getDirectorPending);
        app.post("/api/docs/{id}/approve", this::approveDocument);
        
        app.get("/api/docs/{id}/download", this::downloadDocument);
        
        // NEW: Public Verification Endpoint
        app.get("/api/public/verify/{id}", this::verifyDocument);

        app.get("/api/docs/{id}/preview", this::previewDocument);
    }

    /**
     * Handles the request for a new document.
     * @param ctx the Javalin context
     */
    private void requestDocument(Context ctx) {
        String userId = ctx.attribute("userId");
        String typeStr = ctx.formParam("type");
        
        try {
            DocumentType type = DocumentType.valueOf(typeStr);
            documentUseCase.requestDocument(userId, type, ctx.ip());
            ctx.status(201).result("Request submitted successfully.");
        } catch (Exception e) {
            ctx.status(400).result(e.getMessage());
        }
    }

    /**
     * Handles the upload of a document.
     * @param ctx the Javalin context
     */
    private void uploadDocument(Context ctx) {
        String registrarId = ctx.attribute("userId");
        String docId = ctx.pathParam("id");
        UploadedFile file = ctx.uploadedFile("pdf");

        if (file == null) {
            ctx.status(400).result("No file was uploaded.");
            return;
        }

        // Strict PDF Guard: Checks both extension and MIME payload
        if (!file.filename().toLowerCase().endsWith(".pdf") || 
            file.contentType() == null || 
            !file.contentType().equalsIgnoreCase("application/pdf")) {
            ctx.status(400).result("Security Guard: Only strict PDF files are permitted.");
            return;
        }

        // try-with-resources forces the InputStream to close. 
        // This releases the Windows file lock, allowing Jetty to delete the temp multipart file.
        try (java.io.InputStream contentStream = file.content()) {
            documentUseCase.uploadRawDocument(registrarId, docId, contentStream, file.filename(), ctx.ip());
            ctx.status(200).result("Document uploaded and routed to Campus Director.");
        } catch (Exception e) {
            ctx.status(500).result(e.getMessage());
        }
    }

    /**
     * Handles the approval of a document.
     * @param ctx the Javalin context
     */
    private void approveDocument(Context ctx) {
        String directorId = ctx.attribute("userId");
        String docId = ctx.pathParam("id");
        
        try {
            // Generate base URL from the incoming HTTP request context
            String baseUrl = ctx.scheme() + "://" + ctx.host();
            
            documentUseCase.approveAndStampDocument(directorId, docId, baseUrl, ctx.ip());
            ctx.status(200).result("Document Approved & Cryptographically Stamped.");
        } catch (Exception e) {
            ctx.status(500).result(e.getMessage());
        }
    }

    /**
     * Retrieves the documents for a student.
     * @param ctx the Javalin context
     */
    private void getStudentDocs(Context ctx) {
        ctx.json(documentUseCase.getStudentDocuments(ctx.attribute("userId")));
    }

    /**
     * Retrieves pending uploads for the registrar.
     * @param ctx the Javalin context
     */
    private void getRegistrarPending(Context ctx) {
        ctx.json(documentUseCase.getPendingUploads(ctx.attribute("userId")));
    }

    /**
     * Retrieves pending approvals for the director.
     * @param ctx the Javalin context
     */
    private void getDirectorPending(Context ctx) {
        ctx.json(documentUseCase.getPendingApprovals(ctx.attribute("userId")));
    }

    /**
     * Handles the download of a document.
     * @param ctx the Javalin context
     */
    private void downloadDocument(Context ctx) {
        String userId = ctx.attribute("userId");
        String docId = ctx.pathParam("id");

        try {
            File file = documentUseCase.getDocumentFile(userId, docId);
            ctx.contentType("application/pdf");
            ctx.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            ctx.result(new FileInputStream(file));
        } catch (Exception e) {
            ctx.status(403).result(e.getMessage());
        }
    }

    /**
     * Verifies a document publicly.
     * @param ctx the Javalin context
     */
    private void verifyDocument(Context ctx) {
        String docId = ctx.pathParam("id");
        try {
            Map<String, Object> data = documentUseCase.verifyDocument(docId);
            ctx.status(200).json(data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.status(404).result(e.getMessage());
        } catch (Exception e) {
            ctx.status(500).result("Internal verification engine fault.");
        }
    }

    /**
     * Handles the preview of a raw document before approval.
     * @param ctx the Javalin context
     */
    private void previewDocument(Context ctx) {
        String userId = ctx.attribute("userId");
        String docId = ctx.pathParam("id");

        try {
            File file = documentUseCase.getRawDocumentFile(userId, docId);
            ctx.contentType("application/pdf");
            // 'inline' instructs the browser to open it in the tab
            ctx.header("Content-Disposition", "inline; filename=\"PREVIEW_" + file.getName() + "\"");
            ctx.result(new FileInputStream(file));
        } catch (Exception e) {
            ctx.status(403).result(e.getMessage());
        }
    }
}