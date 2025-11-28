package org.bits.diamabankwalletf.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.JwtToken;
import org.bits.diamabankwalletf.repository.JwtTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class JwtTokenService {

    private final JwtTokenRepository jwtTokenRepository;

    @Value("${jwt.secret:561c87dde9b58bd659ea665a25e8ea8be72fec90}")
    private String jwtSecret;

    @Value("${jwt.expiration:1440}") // 24 hours in minutes
    private int jwtExpirationMinutes;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Validate EMV QR Code - with proper CRC validation
     */
    public JwtValidationResult validateQRCode(String emvQRCode) {
        try {
            log.info("Validating EMV QR code, length: {}", emvQRCode != null ? emvQRCode.length() : "null");

            if (!StringUtils.hasText(emvQRCode)) {
                return JwtValidationResult.error("EMV QR code is required");
            }

            // Basic format validation
            if (emvQRCode.length() < 30) {
                return JwtValidationResult.error("EMV QR code too short");
            }

            if (emvQRCode.length() > 600) {
                return JwtValidationResult.error("EMV QR code too long");
            }

            // Parse EMV QR code to extract token ID
            String tokenId;
            try {
                tokenId = parseEMVQRCodeForTokenId(emvQRCode);
            } catch (Exception parseException) {
                log.error("EMV parsing failed: {}", parseException.getMessage());
                return JwtValidationResult.error("EMV QR code format error: " + parseException.getMessage());
            }

            if (!StringUtils.hasText(tokenId)) {
                return JwtValidationResult.error("Could not extract token ID from EMV QR code");
            }

            log.info("Successfully extracted token ID from EMV QR: {}", tokenId);

            // Validate token in database
            return validateTokenFromDatabase(tokenId);

        } catch (Exception e) {
            log.error("Error validating EMV QR code", e);
            return JwtValidationResult.error("Failed to validate EMV QR code: " + e.getMessage());
        }
    }

    /**
     * Parse EMV QR Code to extract token ID - with fixed CRC validation
     */
    private String parseEMVQRCodeForTokenId(String qrCode) {
        try {
            log.debug("Parsing EMV QR code for token ID, length: {}", qrCode.length());

            if (!qrCode.startsWith("000201")) {
                log.error("EMV QR code missing format indicator");
                throw new IllegalArgumentException("EMV QR code missing format indicator");
            }

            // Verify CRC with correct algorithm
            if (!verifyCRC(qrCode)) {
                log.error("EMV QR code CRC validation failed");
                throw new IllegalArgumentException("EMV QR code CRC validation failed");
            }
            log.debug("CRC validation passed");

            Map<String, String> dataObjects = parseQRCodeData(qrCode);
            log.debug("Parsed {} data objects from EMV QR", dataObjects.size());

            // Extract merchant account information (ID "26")
            String merchantAccountInfo = dataObjects.get("26");
            if (!StringUtils.hasText(merchantAccountInfo)) {
                log.error("Missing merchant account information (ID 26) in EMV QR");
                throw new IllegalArgumentException("Missing merchant account information in EMV QR");
            }

            // Parse merchant account template
            Map<String, String> merchantData = parseQRCodeData(merchantAccountInfo);

            String tokenId = merchantData.get("03"); // Token reference is in ID "03"

            if (!StringUtils.hasText(tokenId)) {
                log.error("Token ID not found in merchant data (ID 03)");
                throw new IllegalArgumentException("Token ID not found in EMV QR code");
            }

            log.debug("Successfully extracted token ID: {}", tokenId);
            return tokenId;

        } catch (Exception e) {
            log.error("Failed to parse EMV QR code: {}", e.getMessage(), e);
            throw new RuntimeException("EMV QR code parsing error: " + e.getMessage(), e);
        }
    }

    /**
     * Verify CRC-16 checksum - FIXED to match agent backend exactly
     */
    private boolean verifyCRC(String qrCode) {
        try {
            log.debug("=== CRC VERIFICATION DEBUG START ===");
            log.debug("QR Code length: {}", qrCode.length());

            // Find the CRC field position
            int crcFieldStart = qrCode.lastIndexOf("6304");
            if (crcFieldStart == -1) {
                log.error("CRC field '6304' not found in EMV QR code");
                return false;
            }

            log.debug("CRC field starts at position: {}", crcFieldStart);

            // Extract expected CRC (4 characters after "6304")
            if (crcFieldStart + 8 > qrCode.length()) {
                log.error("QR code too short to contain complete CRC field");
                return false;
            }

            String expectedCrc = qrCode.substring(crcFieldStart + 4, crcFieldStart + 8);
            log.debug("Expected CRC: {}", expectedCrc);

            // CRITICAL FIX: Calculate CRC over data INCLUDING "6304" but EXCLUDING the actual CRC value
            // This matches exactly how the agent backend generates it
            String dataForCrc = qrCode.substring(0, crcFieldStart + 4); // Include "6304"
            log.debug("Data for CRC calculation length: {}", dataForCrc.length());

            String calculatedCrc = calculateCRC16(dataForCrc);
            log.debug("Calculated CRC: {}", calculatedCrc);

            boolean isValid = expectedCrc.equalsIgnoreCase(calculatedCrc);
            log.debug("CRC validation result: {}", isValid);
            log.debug("=== CRC VERIFICATION DEBUG END ===");

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying CRC", e);
            return false;
        }
    }

    /**
     * Parse EMV QR code data into map - matches agent backend exactly
     */
    private Map<String, String> parseQRCodeData(String qrCode) {
        Map<String, String> dataObjects = new HashMap<>();
        int position = 0;

        while (position < qrCode.length() - 4) {
            if (position + 4 > qrCode.length()) break;

            try {
                String id = qrCode.substring(position, position + 2);
                String lengthStr = qrCode.substring(position + 2, position + 4);

                int length = Integer.parseInt(lengthStr);
                if (position + 4 + length > qrCode.length()) {
                    log.warn("Invalid length at position {}, expected: {}, remaining: {}",
                            position, length, qrCode.length() - position - 4);
                    break;
                }

                String value = qrCode.substring(position + 4, position + 4 + length);
                dataObjects.put(id, value);

                position += 4 + length;
            } catch (NumberFormatException e) {
                log.error("Invalid length format at position {}", position);
                break;
            }
        }

        return dataObjects;
    }

    /**
     * Calculate CRC-16 checksum - EXACTLY matches agent backend implementation
     */
    private String calculateCRC16(String data) {
        if (!StringUtils.hasText(data)) {
            throw new IllegalArgumentException("Data for CRC calculation cannot be null or empty");
        }

        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        int crc = 0xFFFF; // Initial value
        int polynomial = 0x1021; // Polynomial

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ polynomial;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }

        return String.format("%04X", crc);
    }

    /**
     * Validate token from database - UPDATED to handle shortened token IDs
     */
    private JwtValidationResult validateTokenFromDatabase(String tokenId) {
        try {
            log.info("Looking up token in database: {}", tokenId);

            // Find token in database - handle both full and shortened token IDs
            Optional<JwtToken> tokenOpt;

            if (tokenId.length() == 16) {
                // Shortened token ID - search by prefix
                log.debug("Using shortened token ID (16 chars), searching by prefix");
                tokenOpt = jwtTokenRepository.findByTokenIdStartingWith(tokenId);
            } else {
                // Full token ID
                log.debug("Using full token ID, direct lookup");
                tokenOpt = jwtTokenRepository.findByTokenId(tokenId);
            }

            if (!tokenOpt.isPresent()) {
                log.error("Token not found in database: {}", tokenId);
                return JwtValidationResult.error("Invalid EMV QR code token");
            }

            JwtToken tokenEntity = tokenOpt.get();

            // Check token status
            if (!tokenEntity.isActive()) {
                log.error("Token is not active. Status: {}, Expired: {}",
                        tokenEntity.getStatus(), tokenEntity.isExpired());

                if (tokenEntity.isExpired()) {
                    return JwtValidationResult.error("EMV QR code has expired");
                } else if ("USED".equals(tokenEntity.getStatus())) {
                    return JwtValidationResult.error("EMV QR code has already been used");
                } else {
                    return JwtValidationResult.error("EMV QR code is no longer valid");
                }
            }

            // Create payment data
            PaymentData paymentData = new PaymentData();
            paymentData.setTokenId(tokenEntity.getTokenId()); // Use FULL token ID from database
            paymentData.setMerchantWallet(tokenEntity.getMerchantWallet());
            paymentData.setMerchantNumber(tokenEntity.getMerchantNumber());
            paymentData.setMerchantName(tokenEntity.getMerchantName());
            paymentData.setBankCode(tokenEntity.getBankCode());
            paymentData.setAmount(tokenEntity.getAmount());
            paymentData.setCurrency(tokenEntity.getCurrency());
            paymentData.setExpiresAt(tokenEntity.getExpiresAt());

            log.info("EMV QR token validated successfully. Full Token ID: {}", tokenEntity.getTokenId());

            return JwtValidationResult.success(paymentData);

        } catch (Exception e) {
            log.error("Error validating token from database", e);
            return JwtValidationResult.error("Database validation error: " + e.getMessage());
        }
    }
    /**
     * Mark EMV QR token as used
     */
    public boolean markTokenAsUsed(String emvQRCodeOrTokenId, String customerWallet, String transactionRef) {
        try {
            String tokenId;

            // Check if input looks like a token ID or EMV QR code
            if (emvQRCodeOrTokenId.length() > 50 && emvQRCodeOrTokenId.startsWith("000201")) {
                // Looks like EMV QR code - extract token ID
                tokenId = parseEMVQRCodeForTokenId(emvQRCodeOrTokenId);
                log.info("Extracted tokenId from EMV QR for marking: {}", tokenId);
            } else {
                // Assume it's already a token ID
                tokenId = emvQRCodeOrTokenId;
                log.info("Using direct tokenId for marking: {}", tokenId);
            }

            if (!StringUtils.hasText(tokenId)) {
                log.error("Could not extract valid tokenId for marking as used");
                return false;
            }

            Optional<JwtToken> tokenOpt = jwtTokenRepository.findByTokenId(tokenId);
            if (tokenOpt.isPresent()) {
                JwtToken token = tokenOpt.get();
                token.markAsUsed(customerWallet, transactionRef);
                jwtTokenRepository.save(token);

                log.info("EMV QR token marked as used. Token ID: {}, Transaction: {}",
                        tokenId, transactionRef);
                return true;
            } else {
                log.error("EMV QR token not found for marking as used: {}", tokenId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error marking EMV QR token as used", e);
            return false;
        }
    }

    /**
     * Revoke EMV QR token
     */
    public boolean revokeToken(String emvQRCodeOrTokenId) {
        try {
            String tokenId;

            // Check if input looks like a token ID or EMV QR code
            if (emvQRCodeOrTokenId.length() > 50 && emvQRCodeOrTokenId.startsWith("000201")) {
                // Looks like EMV QR code - extract token ID
                tokenId = parseEMVQRCodeForTokenId(emvQRCodeOrTokenId);
            } else {
                // Assume it's already a token ID
                tokenId = emvQRCodeOrTokenId;
            }

            if (!StringUtils.hasText(tokenId)) {
                log.error("Could not extract valid tokenId for revocation");
                return false;
            }

            Optional<JwtToken> tokenOpt = jwtTokenRepository.findByTokenId(tokenId);
            if (tokenOpt.isPresent()) {
                JwtToken token = tokenOpt.get();
                token.revoke();
                jwtTokenRepository.save(token);

                log.info("EMV QR token revoked. Token ID: {}", tokenId);
                return true;
            } else {
                log.error("EMV QR token not found for revocation: {}", tokenId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error revoking EMV QR token", e);
            return false;
        }
    }

    // Legacy JWT validation method (kept for backward compatibility)
    public JwtValidationResult validateJwtToken(String jwtToken) {
        try {
            log.info("Validating JWT token directly");

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody();

            String tokenId = claims.get("tokenId", String.class);
            if (!StringUtils.hasText(tokenId)) {
                return JwtValidationResult.error("Invalid token: missing tokenId");
            }

            return validateTokenFromDatabase(tokenId);

        } catch (Exception e) {
            log.error("Error validating JWT token directly", e);
            return JwtValidationResult.error("Invalid or expired token");
        }
    }

    // Result classes
    public static class JwtValidationResult {
        private final boolean success;
        private final PaymentData paymentData;
        private final String errorMessage;

        private JwtValidationResult(boolean success, PaymentData paymentData, String errorMessage) {
            this.success = success;
            this.paymentData = paymentData;
            this.errorMessage = errorMessage;
        }

        public static JwtValidationResult success(PaymentData paymentData) {
            return new JwtValidationResult(true, paymentData, null);
        }

        public static JwtValidationResult error(String errorMessage) {
            return new JwtValidationResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public PaymentData getPaymentData() { return paymentData; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class PaymentData {
        private String tokenId;
        private String merchantWallet;
        private String merchantNumber;
        private String merchantName;
        private String bankCode;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime expiresAt;

        // Getters and Setters
        public String getTokenId() { return tokenId; }
        public void setTokenId(String tokenId) { this.tokenId = tokenId; }

        public String getMerchantWallet() { return merchantWallet; }
        public void setMerchantWallet(String merchantWallet) { this.merchantWallet = merchantWallet; }

        public String getMerchantNumber() { return merchantNumber; }
        public void setMerchantNumber(String merchantNumber) { this.merchantNumber = merchantNumber; }

        public String getMerchantName() { return merchantName; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

        public String getBankCode() { return bankCode; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    }

    /**
     * Generate JWT for customer QR
     */
    public JwtGenerationResult generateJwtForCustomerQR(String customerIdentifier, BigDecimal amount, String walletNumber) {
        try {
            log.info("Generating JWT for customer: {} with amount: {}", customerIdentifier, amount);

            // Generate unique token ID
            String tokenId = UUID.randomUUID().toString();

            // Set expiry date
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(jwtExpirationMinutes);
            Date expiryDate = Date.from(expiresAt.atZone(java.time.ZoneId.systemDefault()).toInstant());

            // Create JWT payload
            Map<String, Object> claims = new HashMap<>();
            claims.put("tokenId", tokenId);
            claims.put("customerIdentifier", customerIdentifier);
            claims.put("walletNumber", walletNumber);
            claims.put("amount", amount.toString());
            claims.put("currency", "GNF");
            claims.put("type", "CUSTOMER_PAYMENT_REQUEST");
            claims.put("entityType", "CUSTOMER");
            claims.put("iat", System.currentTimeMillis() / 1000);

            // Generate JWT token
            String jwtToken = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(customerIdentifier)
                    .setIssuedAt(new Date())
                    .setExpiration(expiryDate)
                    .setIssuer("CUSTOMER_WALLET_SYSTEM")
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

            // Save token to database
            JwtToken tokenEntity = new JwtToken();
            tokenEntity.setTokenId(tokenId);
            tokenEntity.setJwtToken(jwtToken);
            tokenEntity.setMerchantWallet(walletNumber); // Store wallet number
            tokenEntity.setMerchantNumber(walletNumber); // Store wallet number for both
            tokenEntity.setMerchantName(customerIdentifier); // Store customer identifier as name
            tokenEntity.setBankCode("00100");
            tokenEntity.setAmount(amount);
            tokenEntity.setCurrency("GNF");
            tokenEntity.setExpiresAt(expiresAt);
            tokenEntity.setStatus("ACTIVE");

            jwtTokenRepository.save(tokenEntity);

            log.info("JWT generated successfully for customer. Token ID: {}", tokenId);

            return JwtGenerationResult.success(jwtToken, tokenId);

        } catch (Exception e) {
            log.error("Error generating JWT for customer: " + customerIdentifier, e);
            return JwtGenerationResult.error("Failed to generate JWT: " + e.getMessage());
        }
    }

    /**
     * Generate JWT with EMV QR code for Customer
     */
    public JwtGenerationResult generateJwtForCustomerQRWithEMVCode(String customerIdentifier, BigDecimal amount, String emvQRCode) {
        try {
            log.info("Generating JWT with only EMV QR code for customer: {}", customerIdentifier);

            // Generate unique token ID
            String tokenId = UUID.randomUUID().toString();

            // Set expiry date
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(jwtExpirationMinutes);
            Date expiryDate = Date.from(expiresAt.atZone(java.time.ZoneId.systemDefault()).toInstant());

            // Create JWT payload with ONLY EMV QR code
            Map<String, Object> claims = new HashMap<>();
            claims.put("emvQRCode", emvQRCode);
            claims.put("entityType", "CUSTOMER");

            // Generate JWT token
            String jwtToken = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(tokenId)
                    .setIssuedAt(new Date())
                    .setExpiration(expiryDate)
                    .setIssuer("CUSTOMER_WALLET_SYSTEM")
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

            log.info("JWT with only EMV QR code generated successfully for customer. Token ID: {}", tokenId);

            return JwtGenerationResult.success(jwtToken, tokenId);

        } catch (Exception e) {
            log.error("Error generating JWT with EMV QR code for customer: " + customerIdentifier, e);
            return JwtGenerationResult.error("Failed to generate JWT with EMV QR code: " + e.getMessage());
        }
    }

    /**
     * JWT Generation Result class
     */
    public static class JwtGenerationResult {
        private final boolean success;
        private final String jwtToken;
        private final String tokenId;
        private final String errorMessage;

        private JwtGenerationResult(boolean success, String jwtToken, String tokenId, String errorMessage) {
            this.success = success;
            this.jwtToken = jwtToken;
            this.tokenId = tokenId;
            this.errorMessage = errorMessage;
        }

        public static JwtGenerationResult success(String jwtToken, String tokenId) {
            return new JwtGenerationResult(true, jwtToken, tokenId, null);
        }

        public static JwtGenerationResult error(String errorMessage) {
            return new JwtGenerationResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getJwtToken() { return jwtToken; }
        public String getTokenId() { return tokenId; }
        public String getErrorMessage() { return errorMessage; }
    }
}
