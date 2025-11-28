package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.model.Customer;
import org.bits.diamabankwalletf.model.JwtToken;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.CustomerRepository;
import org.bits.diamabankwalletf.repository.JwtTokenRepository;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class EMVQRCodeService {

    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenRepository jwtTokenRepository;

    /**
     * Generate EMV QR code for customer wallet
     */
    public EMVQRResult generateCustomerEMVQR(String customerIdentifier, BigDecimal amount, boolean isDynamic) {
        try {
            log.info("Generating EMV QR code for customer: {} with amount: {}", customerIdentifier, amount);

            // Find customer by phone number or customer ID
            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(customerIdentifier);

            if (!customerOpt.isPresent()) {
                customerOpt = customerRepository.findByCustomerId(customerIdentifier);
            }

            if (!customerOpt.isPresent()) {
                log.error("Customer not found with identifier: {}", customerIdentifier);
                return EMVQRResult.error("Customer not found with identifier: " + customerIdentifier);
            }

            Customer customer = customerOpt.get();
            log.info("Customer found: customerId={}, phoneNumber={}",
                    customer.getCustomerId(), customer.getPhoneNumber());

            // Find customer's wallet
            Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(customer.getPhoneNumber());

            if (!walletOpt.isPresent()) {
                log.error("Wallet not found for customer phone number: {}", customer.getPhoneNumber());
                return EMVQRResult.error("Wallet not found for customer");
            }

            Wallet wallet = walletOpt.get();
            String walletNumber = wallet.getWalletNumber();

            log.info("Wallet found: walletNumber={}", walletNumber);

            // Build customer name
            String customerName = buildCustomerName(customer);
            String customerCity = extractCityFromWallet(wallet);
            String bankCode = wallet.getBankCode() != null ? wallet.getBankCode() : "00100";

            return generateQRCodeInternal(
                    walletNumber,
                    amount,
                    isDynamic,
                    customerName,
                    "0000", // MCC for person-to-person
                    customerCity,
                    bankCode,
                    walletNumber,
                    customer.getCustomerId(),
                    "CUSTOMER",
                    customerIdentifier
            );

        } catch (Exception e) {
            log.error("Error generating EMV QR code for customer", e);
            return EMVQRResult.error("Failed to generate EMV QR code: " + e.getMessage());
        }
    }

    /**
     * Generate QR code internal logic
     */
    private EMVQRResult generateQRCodeInternal(
            String walletNumber,
            BigDecimal amount,
            boolean isDynamic,
            String name,
            String mcc,
            String city,
            String bankCode,
            String walletRef,
            String entityNumber,
            String entityType,
            String originalIdentifier
    ) {
        try {
            // 1. Generate initial JWT for the transaction
            JwtTokenService.JwtGenerationResult jwtResult =
                    jwtTokenService.generateJwtForCustomerQR(originalIdentifier, amount, walletNumber);

            if (!jwtResult.isSuccess()) {
                return EMVQRResult.error("Failed to generate JWT: " + jwtResult.getErrorMessage());
            }

            // 2. Build EMV QR payload
            StringBuilder qrPayload = new StringBuilder();

            // Payload Format Indicator (ID "00")
            qrPayload.append(buildDataObject("00", "01"));

            // Point of Initiation Method (ID "01")
            qrPayload.append(buildDataObject("01", isDynamic ? "12" : "11"));

            // Merchant Account Information (ID "26")
            String accountInfo = buildAccountInfoTemplate(walletRef, bankCode, jwtResult.getTokenId(), entityType);
            qrPayload.append(buildDataObject("26", accountInfo));

            // Merchant Category Code (ID "52")
            qrPayload.append(buildDataObject("52", mcc));

            // Transaction Currency (ID "53") - GNF
            qrPayload.append(buildDataObject("53", "324"));

            // Transaction Amount (ID "54")
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                qrPayload.append(buildDataObject("54", formatAmount(amount)));
            }

            // Country Code (ID "58")
            qrPayload.append(buildDataObject("58", "GN"));

            // Merchant Name (ID "59")
            qrPayload.append(buildDataObject("59", truncateString(name, 25)));

            // Merchant City (ID "60")
            qrPayload.append(buildDataObject("60", truncateString(city, 15)));

            // Additional Data Field Template (ID "62")
            String additionalData = buildAdditionalDataTemplate(jwtResult.getTokenId(), entityType);
            if (!additionalData.isEmpty()) {
                qrPayload.append(buildDataObject("62", additionalData));
            }

            // Calculate and append CRC (ID "63")
            String crcPayload = qrPayload.toString() + "6304";
            String crc = calculateCRC16(crcPayload);
            qrPayload.append(buildDataObject("63", crc));

            String finalQRCode = qrPayload.toString();

            // 3. Generate new JWT with EMV QR code
            JwtTokenService.JwtGenerationResult jwtWithQRResult =
                    jwtTokenService.generateJwtForCustomerQRWithEMVCode(originalIdentifier, amount, finalQRCode);

            if (!jwtWithQRResult.isSuccess()) {
                return EMVQRResult.error("Failed to generate JWT with EMV QR code: " + jwtWithQRResult.getErrorMessage());
            }

            // 4. Update JWT token in database
            updateJwtToken(jwtResult.getTokenId(), jwtWithQRResult.getJwtToken());

            log.info("EMV QR code generated successfully for {}. Length: {}", entityType, finalQRCode.length());

            return EMVQRResult.success(finalQRCode, jwtResult.getTokenId(), name, jwtWithQRResult.getJwtToken(), entityType);

        } catch (Exception e) {
            log.error("Error generating EMV QR code", e);
            return EMVQRResult.error("Failed to generate EMV QR code: " + e.getMessage());
        }
    }

    private void updateJwtToken(String tokenId, String jwtToken) {
        try {
            Optional<JwtToken> existingToken = jwtTokenRepository.findByTokenId(tokenId);

            if (existingToken.isPresent()) {
                JwtToken tokenEntity = existingToken.get();
                tokenEntity.setJwtToken(jwtToken);
                jwtTokenRepository.save(tokenEntity);
                log.info("Updated existing token with EMV QR code. Token ID: {}", tokenId);
            } else {
                log.warn("Token not found for update: {}", tokenId);
            }
        } catch (Exception e) {
            log.error("Failed to update JWT token", e);
            throw new RuntimeException("Failed to update JWT token: " + e.getMessage());
        }
    }

    private String buildAccountInfoTemplate(String walletNumber, String bankCode, String tokenId, String entityType) {
        StringBuilder template = new StringBuilder();
        template.append(buildDataObject("00", "org.bits.diamabankwallet"));
        template.append(buildDataObject("01", walletNumber));

        if (bankCode != null && !bankCode.isEmpty()) {
            template.append(buildDataObject("02", bankCode));
        }

        String shortTokenId = tokenId.length() >= 16 ? tokenId.substring(0, 16) : tokenId;
        template.append(buildDataObject("03", shortTokenId));
        template.append(buildDataObject("04", entityType));

        return template.toString();
    }

    private String buildAdditionalDataTemplate(String tokenId, String entityType) {
        StringBuilder template = new StringBuilder();

        if (tokenId != null && !tokenId.isEmpty()) {
            String shortTokenId = tokenId.length() >= 16 ? tokenId.substring(0, 16) : tokenId;
            template.append(buildDataObject("05", shortTokenId));
        }

        template.append(buildDataObject("07", entityType));
        return template.toString();
    }

    private String buildCustomerName(Customer customer) {
        StringBuilder name = new StringBuilder();

        if (customer.getFirstName() != null && !customer.getFirstName().isEmpty()) {
            name.append(customer.getFirstName());
        }

        if (customer.getLastName() != null && !customer.getLastName().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(customer.getLastName());
        }

        return name.length() > 0 ? name.toString() : "CUSTOMER";
    }

    private String extractCityFromWallet(Wallet wallet) {
        if (wallet.getAddress1() != null && !wallet.getAddress1().isEmpty()) {
            String[] parts = wallet.getAddress1().split(",");
            if (parts.length > 1) {
                String city = parts[parts.length - 1].trim();
                if (!city.isEmpty()) {
                    return city;
                }
            }
        }
        return "CONAKRY";
    }

    private String buildDataObject(String id, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (id.length() != 2) {
            throw new IllegalArgumentException("ID must be exactly 2 digits: " + id);
        }

        int length = value.length();
        if (length > 99) {
            throw new IllegalArgumentException("Value too long (max 99 characters): " + length);
        }

        String lengthStr = String.format("%02d", length);
        return id + lengthStr + value;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private String truncateString(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String calculateCRC16(String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        int crc = 0xFFFF;
        int polynomial = 0x1021;

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

    public static class EMVQRResult {
        private final boolean success;
        private final String qrCode;
        private final String tokenId;
        private final String entityName;
        private final String jwtToken;
        private final String entityType;
        private final String errorMessage;

        private EMVQRResult(boolean success, String qrCode, String tokenId,
                            String entityName, String jwtToken, String entityType, String errorMessage) {
            this.success = success;
            this.qrCode = qrCode;
            this.tokenId = tokenId;
            this.entityName = entityName;
            this.jwtToken = jwtToken;
            this.entityType = entityType;
            this.errorMessage = errorMessage;
        }

        public static EMVQRResult success(String qrCode, String tokenId, String entityName, String jwtToken, String entityType) {
            return new EMVQRResult(true, qrCode, tokenId, entityName, jwtToken, entityType, null);
        }

        public static EMVQRResult error(String errorMessage) {
            return new EMVQRResult(false, null, null, null, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getQrCode() { return qrCode; }
        public String getTokenId() { return tokenId; }
        public String getEntityName() { return entityName; }
        public String getJwtToken() { return jwtToken; }
        public String getEntityType() { return entityType; }
        public String getErrorMessage() { return errorMessage; }
    }
}
