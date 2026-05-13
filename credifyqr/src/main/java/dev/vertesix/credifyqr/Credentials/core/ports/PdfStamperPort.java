package dev.vertesix.credifyqr.Credentials.core.ports;

import java.io.File;

/**
 * Port for stamping PDF documents with verification metadata.
 */
public interface PdfStamperPort {
    /**
     * Writes verification content into a raw PDF document.
     */
    void stampPdf(File rawFile, File outputFile, String verificationUrl) throws Exception;
}