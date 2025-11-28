package org.bits.diamabankwalletf.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class SecurityUtils {

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a secure token for sessions
     */
    public static String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                Long.toHexString(System.currentTimeMillis());
    }

    /**
     * Hash security answer for storage (same method as PIN)
     */
    public static String hashSecurityAnswer(String answer, String salt) {
        try {
            // Normalize the answer
            String normalizedAnswer = answer.trim().toLowerCase().replaceAll("\\s+", " ");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            md.update(normalizedAnswer.getBytes());

            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            log.error("Error hashing security answer", e);
            throw new RuntimeException("Error hashing security answer", e);
        }
    }

    /**
     * Verify security answer
     */
    public static boolean verifySecurityAnswer(String providedAnswer, String storedHash, String salt) {
        try {
            String hashedProvided = hashSecurityAnswer(providedAnswer, salt);
            return hashedProvided.equals(storedHash);
        } catch (Exception e) {
            log.error("Error verifying security answer", e);
            return false;
        }
    }

    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(LocalDateTime expiry) {
        return expiry != null && LocalDateTime.now().isAfter(expiry);
    }

    /**
     * Mask phone number for logging
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }

        int length = phoneNumber.length();
        StringBuilder masked = new StringBuilder();

        // Show first 2 digits
        masked.append(phoneNumber, 0, 2);

        // Add asterisks for middle digits
        for (int i = 2; i < length - 2; i++) {
            masked.append("*");
        }

        // Show last 2 digits
        masked.append(phoneNumber.substring(length - 2));

        return masked.toString();
    }

    /**
     * Validate PIN format and strength
     */
    public static boolean isValidPin(String pin) {
        if (pin == null || !pin.matches("^\\d{4,6}$")) {
            return false;
        }

        // Check for sequential numbers (1234, 4321)
        boolean isSequential = true;
        boolean isReverseSequential = true;

        for (int i = 1; i < pin.length(); i++) {
            int current = Character.getNumericValue(pin.charAt(i));
            int previous = Character.getNumericValue(pin.charAt(i - 1));

            if (current != previous + 1) {
                isSequential = false;
            }
            if (current != previous - 1) {
                isReverseSequential = false;
            }
        }

        if (isSequential || isReverseSequential) {
            return false;
        }

        // Check for repeated digits (1111, 2222)
        char firstDigit = pin.charAt(0);
        boolean allSame = true;
        for (int i = 1; i < pin.length(); i++) {
            if (pin.charAt(i) != firstDigit) {
                allSame = false;
                break;
            }
        }

        return !allSame;
    }

    public static String generateRequestId() {
        SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
        String datePrefix = idFormat.format(new Date());
        int randomNum = new Random().nextInt(1_000_000); // 6 digits
        String randomDigits = String.format("%06d", randomNum);
        return datePrefix + randomDigits;
    }

    public static String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }


}
