package com.dticnat.controleimpressao.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class FileValidationService {

    private static final List<String> ALLOWED_MEDIA_TYPES = List.of("application/pdf");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    public boolean validateAndProcessFile(MultipartFile file) {
        // 1. Basic Checks
        if (file.isEmpty()) {
            System.err.println("File is empty: " + file.getOriginalFilename());
            return false;
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            System.err.println("File exceeds max size: " + file.getOriginalFilename());
            return false;
        }

        // 2. File Type Detection (Magic Numbers)
        Tika tika = new Tika();
        String detectedMediaType;
        try (InputStream inputStream = file.getInputStream()) {
            // It's good to pass a fresh InputStream to Tika if you plan to reuse it later
            // Or read into a byte array first if files are small enough to hold in memory for multiple checks
            // For large files, always stream and save to temp file first.
            byte[] fileBytes = file.getBytes(); // Read once if small, or handle via temp file
            try (InputStream tikaStream = new java.io.ByteArrayInputStream(fileBytes)) {
                detectedMediaType = tika.detect(tikaStream, file.getOriginalFilename());
            }

            System.out.println("Detected media type for " + file.getOriginalFilename() + ": " + detectedMediaType);

            if (!ALLOWED_MEDIA_TYPES.contains(detectedMediaType)) {
                System.err.println("Disallowed media type: " + detectedMediaType);
                return false;
            }

            // 3. "Corrupted" and "Encrypted" Checks (based on detected type)
            try (InputStream contentStream = new java.io.ByteArrayInputStream(fileBytes)) {
                if ("application/pdf".equals(detectedMediaType)) {
                    if (!isValidPdf(contentStream)) {
                        System.err.println("Invalid or encrypted PDF: " + file.getOriginalFilename());
                        return false;
                    }
                } else if (detectedMediaType.startsWith("image/")) {
                    if (!isValidImage(contentStream)) {
                        System.err.println("Invalid or corrupted image: " + file.getOriginalFilename());
                        return false;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getOriginalFilename() + " - " + e.getMessage());
            return false; // Could be I/O error, or fundamental unreadability
        }

        // All checks pass
        System.out.println("File " + file.getOriginalFilename() + " validated successfully.");
        return true;
    }

    private boolean isValidPdf(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(RandomAccessReadBuffer.createBufferFromStream(inputStream))) {
            // Basic check: if it loads, it's structurally a PDF.
            // It will throw InvalidPasswordException if password protected.
            return document.getNumberOfPages() > 0; // Example: ensure it has pages
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            System.err.println("PDF is password protected (encrypted).");
            return false; // Treat encrypted PDF as invalid for this example
        } catch (IOException e) {
            System.err.println("Failed to parse PDF, likely corrupted: " + e.getMessage());
            return false; // Corrupted
        }
    }

    private boolean isValidImage(InputStream inputStream) {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (IOException e) {
            System.err.println("Failed to read image, likely corrupted: " + e.getMessage());
            return false;
        }
    }
}