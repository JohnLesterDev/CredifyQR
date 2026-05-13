package dev.vertesix.credifyqr.Credentials.core.ports;

import java.io.File;

public interface PdfStamperPort {
    void stampPdf(File rawFile, File outputFile, String verificationUrl) throws Exception;
}