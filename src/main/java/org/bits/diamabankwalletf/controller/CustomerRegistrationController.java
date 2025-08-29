package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.service.OtpVerificationService;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.bits.diamabankwalletf.service.WalletCreationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerRegistrationController {

    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final OtpVerificationService otpVerificationService;

    // Storage for complete registration data (including biometrics)
    private static final Map<String, CustomerRegistrationRequest> CREATE_CUSTOMER_MAP = new ConcurrentHashMap<>();

    @PostMapping("/api/customer/register")
    public ResponseEntity<CustomerRegistrationResponse> registerCustomer(
            @RequestBody CustomerRegistrationRequest request) {

        log.info("Starting customer registration for phone: {}", request.getPhone());

        try {
            if (request.getEntityId() == null || "000000".equals(request.getEntityId())) {
                // Set a valid non-null entityId - use phone number as fallback for tracking
                request.setEntityId(request.getPhone());
            }

            // Create wallet request (without biometric data)
            WalletCreationRequest walletRequest = createWalletRequest(request);

            // Call wallet creation service
            CustomerRegistrationResponse response = walletCreationService.createWallet(walletRequest);

            // Handle response
            if ("000".equals(response.getRespCode())) {
                // Store complete request data with biometrics for future reference
                CREATE_CUSTOMER_MAP.put(response.getAuthCode(), request);
                log.info("Customer registration successful with authCode: {}", response.getAuthCode());
            } else {
                log.warn("Customer registration failed with code: {}, message: {}",
                        response.getRespCode(), response.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during customer registration", e);

            CustomerRegistrationResponse errorResponse = new CustomerRegistrationResponse();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Registration failed: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private WalletCreationRequest createWalletRequest(CustomerRegistrationRequest request) {

        WalletCreationRequest walletRequest = new WalletCreationRequest();

        walletRequest.setFirstName(request.getFirstName());
        walletRequest.setLastName(request.getLastName());
        walletRequest.setEmail(request.getEmail());
        walletRequest.setBirthDate(request.getBirthDate());
        walletRequest.setNationality(request.getNationality());
        walletRequest.setDocumentCode(request.getDocumentCode());
        walletRequest.setDocumentId(request.getDocumentId());
        walletRequest.setGender(request.getGender());
        walletRequest.setPhone(request.getPhone());
        walletRequest.setCurrencyCode(request.getCurrencyCode());
        walletRequest.setBank(request.getBank());
        walletRequest.setBranchCode(request.getBranchCode());
        walletRequest.setSecretQ(request.getSecretQ());
        walletRequest.setAnswer(request.getAnswer());
        walletRequest.setAddress(request.getAddress());
        walletRequest.setType(request.getType());
        walletRequest.setProductCode(request.getProductCode());
        walletRequest.setEntityId(request.getEntityId());
        walletRequest.setNkinName(request.getNkinName());
        walletRequest.setNkinPhone(request.getNkinPhone());
        walletRequest.setSrcFunds(request.getSrcFunds());
        walletRequest.setOccupation(request.getOccupation());
        walletRequest.setEstimatedIncome(request.getEstimatedIncome());
        walletRequest.setEntityId(request.getEntityId());
        walletRequest.setLastActionUser(request.getPhone());
        if (request.getPin() != null) {
            String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), request.getBank());
            walletRequest.setPin(encryptedPin);
        }

        walletRequest.setRequestId(generateRequestId());
        walletRequest.setRequestDate(generateTimestamp());

        return walletRequest;
    }
    /**
     * Generate request ID
     */
    private String generateRequestId() {
        // Format: YYMMDD + 6 random digits
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String datePrefix = sdf.format(new Date());

        // Generate 6 random digits
        Random random = new Random();
        String randomDigits = String.format("%06d", random.nextInt(1000000));

        return datePrefix + randomDigits;
    }

    /**
     * Generate timestamp for request
     */
    private String generateTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    @PostMapping("/api/otp/verify")
    public ResponseEntity<ResponseServiceJson> verifyOtp(@RequestBody OtpVerRequest request) {
        log.info("Received OTP verification request for phone: {}", request.getPhoneNumber());

        try {
            // Validate input
            if (request.getPhoneNumber() == null || request.getAuthCode() == null ||
                    request.getOtp() == null) {

                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("400");
                errorResponse.setMessage("Missing required fields: phoneNumber, authCode, or otp");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Set default entityId if not provided
            String entityId = request.getEntityId();
            if (entityId == null || entityId.isEmpty() || "000000".equals(entityId)) {
                entityId = request.getPhoneNumber(); // Use phone as fallback
            }

            // Call the service
            ResponseServiceJson response = otpVerificationService.verifyOtp(
                    request.getPhoneNumber(),
                    request.getAuthCode(),
                    request.getOtp(),
                    entityId
            );

            // Return success or error based on the response code
            if (response != null && "000".equals(response.getRespCode())) {
                log.info("OTP verification successful for phone: {}", request.getPhoneNumber());
                return ResponseEntity.ok(response);
            } else {
                log.warn("OTP verification failed with code: {}, message: {}",
                        response != null ? response.getRespCode() : "null",
                        response != null ? response.getMessage() : "null");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error processing OTP verification", e);

            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error processing OTP verification: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
