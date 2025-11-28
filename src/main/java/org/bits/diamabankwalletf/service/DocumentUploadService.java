package org.bits.diamabankwalletf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Service
@Slf4j
public class DocumentUploadService {

    @Value("${wallet.upload.face.directory:/home/wallet/ScannedFiles/FACE_IMAGE}")
    private String faceUploadDirectory;

    @Value("${wallet.upload.doc.directory:/home/wallet/ScannedFiles/DOC_IMAGE}")
    private String docUploadDirectory;

    /**
     * Upload face image from base64 string
     */
    public int uploadFaceImage(String customerId, String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            log.warn("Face image is null or empty for customer: {}", customerId);
            return 0; // No image to upload
        }

        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(faceUploadDirectory);
            Files.createDirectories(uploadPath);

            // Decode base64 image
            byte[] imageBytes = decodeBase64Image(base64Image);
            if (imageBytes == null) {
                return -1;
            }

            // Create BufferedImage from bytes
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(bis);
            bis.close();

            if (originalImage == null) {
                log.error("Failed to read image data for customer: {}", customerId);
                return -1;
            }

            // Resize image to 600x600
            BufferedImage resizedImage = createResizedCopy(originalImage, 600, 600, true);

            // Save to file
            File savedFile = new File(uploadPath.toFile(), "FACE__" + customerId + ".png");
            boolean success = ImageIO.write(resizedImage, "png", savedFile);

            if (success) {
                log.info("Face image uploaded successfully for customer: {} to {}",
                        customerId, savedFile.getAbsolutePath());
                return 0;
            } else {
                log.error("Failed to write face image for customer: {}", customerId);
                return -1;
            }

        } catch (Exception e) {
            log.error("Error uploading face image for customer: " + customerId, e);
            return -1;
        }
    }

    /**
     * Upload document front image from base64 string
     */
    public int uploadDocumentFront(String customerId, String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            log.warn("Document front image is null or empty for customer: {}", customerId);
            return 0;
        }

        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(docUploadDirectory);
            Files.createDirectories(uploadPath);

            // Decode base64 image
            byte[] imageBytes = decodeBase64Image(base64Image);
            if (imageBytes == null) {
                return -1;
            }

            // Create BufferedImage from bytes
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(bis);
            bis.close();

            if (originalImage == null) {
                log.error("Failed to read document front image data for customer: {}", customerId);
                return -1;
            }

            // Resize image to 640x480
            BufferedImage resizedImage = createResizedCopy(originalImage, 640, 480, true);

            // Save to file
            File savedFile = new File(uploadPath.toFile(), "DOC_FRONT__" + customerId + ".png");
            boolean success = ImageIO.write(resizedImage, "png", savedFile);

            if (success) {
                log.info("Document front image uploaded successfully for customer: {} to {}",
                        customerId, savedFile.getAbsolutePath());
                return 0;
            } else {
                log.error("Failed to write document front image for customer: {}", customerId);
                return -1;
            }

        } catch (Exception e) {
            log.error("Error uploading document front image for customer: " + customerId, e);
            return -1;
        }
    }

    /**
     * Upload document back image from base64 string
     */
    public int uploadDocumentBack(String customerId, String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            log.warn("Document back image is null or empty for customer: {}", customerId);
            return 0;
        }

        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(docUploadDirectory);
            Files.createDirectories(uploadPath);

            // Decode base64 image
            byte[] imageBytes = decodeBase64Image(base64Image);
            if (imageBytes == null) {
                return -1;
            }

            // Create BufferedImage from bytes
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(bis);
            bis.close();

            if (originalImage == null) {
                log.error("Failed to read document back image data for customer: {}", customerId);
                return -1;
            }

            // Resize image to 640x480
            BufferedImage resizedImage = createResizedCopy(originalImage, 640, 480, true);

            // Save to file
            File savedFile = new File(uploadPath.toFile(), "DOC_BACK__" + customerId + ".png");
            boolean success = ImageIO.write(resizedImage, "png", savedFile);

            if (success) {
                log.info("Document back image uploaded successfully for customer: {} to {}",
                        customerId, savedFile.getAbsolutePath());
                return 0;
            } else {
                log.error("Failed to write document back image for customer: {}", customerId);
                return -1;
            }

        } catch (Exception e) {
            log.error("Error uploading document back image for customer: " + customerId, e);
            return -1;
        }
    }

    /**
     * Upload all documents for a customer
     */
    public int uploadAllDocuments(String customerId, String faceImage, String docFront, String docBack) {
        int result = 0;

        // Upload face image
        int faceResult = uploadFaceImage(customerId, faceImage);
        if (faceResult == -1) {
            result = -1;
            log.error("Failed to upload face image for customer: {}", customerId);
        }

        // Upload document front
        int frontResult = uploadDocumentFront(customerId, docFront);
        if (frontResult == -1) {
            result = -1;
            log.error("Failed to upload document front for customer: {}", customerId);
        }

        // Upload document back
        int backResult = uploadDocumentBack(customerId, docBack);
        if (backResult == -1) {
            result = -1;
            log.error("Failed to upload document back for customer: {}", customerId);
        }

        if (result == 0) {
            log.info("All documents uploaded successfully for customer: {}", customerId);
        }

        return result;
    }

    /**
     * Decode base64 image string to byte array with multiple fallback strategies
     */
    public byte[] decodeBase64Image(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            log.warn("Base64 image string is null or empty");
            return null;
        }

        // Strategy 1: Try direct decoding (in case it's already clean)
        try {
            return Base64.getDecoder().decode(base64Image);
        } catch (Exception e1) {
            log.debug("Strategy 1 failed (direct decode): {}", e1.getMessage());
        }

        // Strategy 2: Remove data URL prefix and try again
        try {
            String base64Data = base64Image.trim();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            return Base64.getDecoder().decode(base64Data);
        } catch (Exception e2) {
            log.debug("Strategy 2 failed (remove prefix): {}", e2.getMessage());
        }

        // Strategy 3: Clean whitespace and newlines
        try {
            String base64Data = base64Image.trim();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            base64Data = base64Data.replaceAll("\\s+", "").replaceAll("\\r", "").replaceAll("\\n", "");
            return Base64.getDecoder().decode(base64Data);
        } catch (Exception e3) {
            log.debug("Strategy 3 failed (clean whitespace): {}", e3.getMessage());
        }

        // Strategy 4: Try MIME decoder (handles line breaks and other formatting)
        try {
            String base64Data = base64Image.trim();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            return Base64.getMimeDecoder().decode(base64Data);
        } catch (Exception e4) {
            log.debug("Strategy 4 failed (MIME decoder): {}", e4.getMessage());
        }

        // Strategy 5: Try URL-safe decoder
        try {
            String base64Data = base64Image.trim();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            base64Data = base64Data.replaceAll("\\s+", "");
            return Base64.getUrlDecoder().decode(base64Data);
        } catch (Exception e5) {
            log.debug("Strategy 5 failed (URL decoder): {}", e5.getMessage());
        }

        // Strategy 6: Remove all non-base64 characters and try again
        try {
            String base64Data = base64Image.trim();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            // Keep only valid base64 characters
            base64Data = base64Data.replaceAll("[^A-Za-z0-9+/=]", "");

            // Add padding if needed
            while (base64Data.length() % 4 != 0) {
                base64Data += "=";
            }

            return Base64.getDecoder().decode(base64Data);
        } catch (Exception e6) {
            log.debug("Strategy 6 failed (clean and pad): {}", e6.getMessage());
        }

        // Strategy 7: Last resort - try to convert from hex if it's actually hex encoded
        try {
            String cleanData = base64Image.trim();
            if (cleanData.contains(",")) {
                cleanData = cleanData.substring(cleanData.indexOf(",") + 1);
            }

            // Check if it looks like hex (even length, only hex chars)
            if (cleanData.length() % 2 == 0 && cleanData.matches("[0-9A-Fa-f]+")) {
                return hexStringToByteArray(cleanData);
            }
        } catch (Exception e7) {
            log.debug("Strategy 7 failed (hex conversion): {}", e7.getMessage());
        }

        // All strategies failed
        log.error("All base64 decoding strategies failed for image data");
        log.error("Original string length: {}", base64Image.length());
        log.error("Sample (first 200 chars): {}",
                base64Image.length() > 200 ? base64Image.substring(0, 200) + "..." : base64Image);

        // Log some diagnostics
        logBase64Diagnostics(base64Image);

        return null;
    }

    /**
     * Convert hex string to byte array
     */
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Log diagnostics about the base64 string to help with debugging
     */
    private void logBase64Diagnostics(String base64Image) {
        if (base64Image == null) {
            log.error("Base64 diagnostics: String is null");
            return;
        }

        String sample = base64Image.length() > 100 ? base64Image.substring(0, 100) : base64Image;

        log.error("Base64 diagnostics:");
        log.error("- Length: {}", base64Image.length());
        log.error("- Contains comma: {}", base64Image.contains(","));
        log.error("- Contains whitespace: {}", base64Image.matches(".*\\s.*"));
        log.error("- Contains newlines: {}", base64Image.contains("\n") || base64Image.contains("\r"));
        log.error("- First 100 chars: {}", sample);

        // Check character distribution
        long validBase64Chars = sample.chars().filter(c ->
                (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
                        (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '='
        ).count();

        log.error("- Valid base64 chars in sample: {}/{}", validBase64Chars, sample.length());

        // Show some invalid characters if any
        String invalidChars = sample.chars()
                .filter(c -> !((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
                        (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=' ||
                        c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == ','))
                .mapToObj(c -> String.valueOf((char)c))
                .distinct()
                .limit(10)
                .reduce("", (a, b) -> a + b);

        if (!invalidChars.isEmpty()) {
            log.error("- Some invalid chars found: {}", invalidChars);
        }
    }

    /**
     * Create a resized copy of the image
     */
    private BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();

        if (preserveAlpha) {
            g.setComposite(AlphaComposite.Src);
        }

        // Use high-quality rendering hints
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        return scaledBI;
    }

    /**
     * Check if upload directories exist and are writable
     */
    public boolean checkUploadDirectories() {
        boolean faceDirectoryOk = checkDirectory(faceUploadDirectory);
        boolean docDirectoryOk = checkDirectory(docUploadDirectory);

        return faceDirectoryOk && docDirectoryOk;
    }

    private boolean checkDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created upload directory: {}", directoryPath);
            }

            if (!Files.isWritable(path)) {
                log.error("Upload directory is not writable: {}", directoryPath);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking upload directory: " + directoryPath, e);
            return false;
        }
    }
}
