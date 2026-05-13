package dev.vertesix.credifyqr.Credentials.adapters.outbound.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.vertesix.credifyqr.Credentials.core.ports.PdfStamperPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

/**
 * A PDF stamper adapter that uses PDFBox to add a QR code and optional logo to each page.
 * <p>
 * The adapter loads a raw PDF, writes a verification QR code and a small logo image
 * to each page, and then saves the stamped result to an output file.
 */
public class PdfBoxStamperAdapter implements PdfStamperPort {

    private static final int QR_SIZE = 80;

    /**
     * Stamps the provided PDF file with a verification QR code and optional logo.
     *
     * @param rawFile the input PDF file to stamp
     * @param outputFile the output PDF file to write the stamped document to
     * @param verificationUrl the URL encoded in the QR code for verification
     * @throws Exception if an error occurs while loading, stamping, or saving the PDF
     */
    @Override
    public void stampPdf(File rawFile, File outputFile, String verificationUrl) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(verificationUrl, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        try (PDDocument document = Loader.loadPDF(rawFile)) {
            PDImageXObject pdQrImage = LosslessFactory.createFromImage(document, qrImage);
            
            PDImageXObject pdLogoImage = null;
            try (InputStream logoStream = getClass().getResourceAsStream("/public/img/credifyqr-logo-resized.png")) {
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    pdLogoImage = PDImageXObject.createFromByteArray(document, logoBytes, "logo");
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not load logo for PDF stamp.");
            }

            for (PDPage page : document.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                float startY = 30; 
                float qrStartX = mediaBox.getWidth() - QR_SIZE - 20;

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.drawImage(pdQrImage, qrStartX, startY, QR_SIZE, QR_SIZE);
                    
                    if (pdLogoImage != null) {
                        float scale = 60f / pdLogoImage.getHeight();
                        float logoW = pdLogoImage.getWidth() * scale;
                        float logoH = 60f;
                        float logoStartX = qrStartX - logoW - 10; 
                        float logoStartY = startY + ((QR_SIZE - logoH) / 2); 
                        contentStream.drawImage(pdLogoImage, logoStartX, logoStartY, logoW, logoH);
                    }
                }
            }
            document.save(outputFile);
        }
    }
}