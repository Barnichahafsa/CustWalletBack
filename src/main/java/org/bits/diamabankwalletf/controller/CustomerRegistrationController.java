package org.bits.diamabankwalletf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.Nationality;
import org.bits.diamabankwalletf.repository.NationalityRepository;
import org.bits.diamabankwalletf.service.DocumentUploadService;
import org.bits.diamabankwalletf.service.OtpVerificationService;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.bits.diamabankwalletf.service.WalletCreationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerRegistrationController {

    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final OtpVerificationService otpVerificationService;
    private final DocumentUploadService documentUploadService;
    private final NationalityRepository nationalityRepository;

    // Storage for complete registration data (including biometrics) - keyed by authCode
    private static final Map<String, CustomerRegistrationRequest> CREATE_CUSTOMER_MAP = new ConcurrentHashMap<>();


    @GetMapping("/api/nationalities")
    public ResponseEntity<List<Nationality>> getAllNationalities() {
        List<Nationality> nationalities = nationalityRepository.findAll();
        log.info("Retrieved {} nationalities", nationalities.size());
        return ResponseEntity.ok(nationalities);
    }

    @PostMapping("/api/customer/register")
    public ResponseEntity<ResponseService> registerCustomer(
            @RequestBody CustomerRegistrationRequest request) {

        log.info("Starting customer registration for phone: {}", request.getPhone());

        try {
            // Check upload directories before processing
            if (!documentUploadService.checkUploadDirectories()) {
                log.error("Upload directories are not accessible");
                ResponseService errorResponse = new ResponseService();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("System error: Unable to access upload directories");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getEntityId() == null || "000000".equals(request.getEntityId())) {
                // Set a valid non-null entityId - use phone number as fallback for tracking
                request.setEntityId(request.getPhone());
            }

            // Create wallet request (without biometric data)
            WalletCreationRequest walletRequest = createWalletRequest(request);

            // Call wallet creation service
            ResponseService response = walletCreationService.createWallet(walletRequest);

            // Handle response
            if ("000".equals(response.getRespCode())) {
                // Store complete request data with biometrics for later use during OTP verification
                CREATE_CUSTOMER_MAP.put(response.getAuthCode(), request);

                log.info("Customer registration successful with authCode: {}, stored biometric data for upload",
                        response.getAuthCode());
            } else {
                log.warn("Customer registration failed with code: {}, message: {}",
                        response.getRespCode(), response.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during customer registration", e);

            ResponseService errorResponse = new ResponseService();
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

            // Call the OTP verification service
            ResponseServiceJson response = otpVerificationService.verifyOtp(
                    request.getPhoneNumber(),
                    request.getAuthCode(),
                    request.getOtp(),
                    entityId
            );

            // If OTP verification is successful, upload documents
            if (response != null && "000".equals(response.getRespCode())) {
                log.info("OTP verification successful for phone: {}, proceeding with document upload",
                        request.getPhoneNumber());

                try {
                    // Extract customer ID from the response
                    String customerId = extractCustomerIdFromResponse(response);

                    if (customerId != null) {
                        // Retrieve stored registration data with biometrics
                        CustomerRegistrationRequest registrationData = CREATE_CUSTOMER_MAP.get(request.getAuthCode());

                        if (registrationData != null) {
                            log.info("Found stored registration data for authCode: {}, uploading documents for customer: {}",
                                    request.getAuthCode(), customerId);

                            // Upload documents
                            int uploadResult = documentUploadService.uploadAllDocuments(
                                    customerId,
                                    registrationData.getFaceId(),
                                    registrationData.getDocumentImg(),
                                    registrationData.getDocumentImgBack()
                            );

                            if (uploadResult == 0) {
                                log.info("Document upload successful for customer: {}", customerId);
                            } else {
                                log.warn("Document upload failed for customer: {}, but OTP verification was successful",
                                        customerId);
                                // Don't fail the OTP verification, just log the warning
                            }

                            // Clean up stored data after successful processing
                            CREATE_CUSTOMER_MAP.remove(request.getAuthCode());

                        } else {
                            log.warn("No stored registration data found for authCode: {}, skipping document upload",
                                    request.getAuthCode());
                        }
                    } else {
                        log.warn("Could not extract customer ID from OTP verification response, skipping document upload");
                    }

                } catch (Exception uploadError) {
                    log.error("Error during document upload after successful OTP verification", uploadError);
                    // Don't fail the OTP verification response, just log the error
                    // The wallet creation and OTP verification were successful
                }

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

    /**
     * Extract customer ID from OTP verification response
     * Expected response structure:
     * {
     *   "status": "OK",
     *   "response": "000",
     *   "result": {
     *     "number": "wallet_number",
     *     "customerId": "customer_id"
     *   }
     * }
     */
    private String extractCustomerIdFromResponse(ResponseServiceJson response) {
        try {
            JsonNode resultNode = response.getResult();

            if (resultNode != null && resultNode.isObject()) {
                JsonNode customerIdNode = resultNode.get("customerId");

                if (customerIdNode != null && !customerIdNode.isNull()) {
                    String customerId = customerIdNode.asText();

                    if (customerId != null && !customerId.trim().isEmpty()) {
                        log.info("Extracted customer ID from response: {}", customerId);
                        return customerId;
                    } else {
                        log.warn("Customer ID is null or empty in response result");
                    }
                } else {
                    log.warn("customerId field not found in response result");
                }
            } else {
                log.warn("Response result is not a valid JSON object, type: {}",
                        resultNode != null ? resultNode.getNodeType() : "null");
            }
        } catch (Exception e) {
            log.error("Error extracting customer ID from response", e);
        }

        return null;
    }

    /**
     * Debug endpoint to analyze base64 images from frontend
     */
    @PostMapping("/api/debug/analyze-base64")
    public ResponseEntity<Map<String, Object>> analyzeBase64(@RequestBody CustomerRegistrationRequest request) {
        Map<String, Object> analysis = new HashMap<>();

        try {
            // Analyze face image
            if (request.getFaceId() != null) {
                Map<String, Object> faceAnalysis = analyzeBase64String("faceId", request.getFaceId());
                analysis.put("faceId", faceAnalysis);
            }

            // Analyze document front
            if (request.getDocumentImg() != null) {
                Map<String, Object> docFrontAnalysis = analyzeBase64String("documentImg", request.getDocumentImg());
                analysis.put("documentImg", docFrontAnalysis);
            }

            // Analyze document back
            if (request.getDocumentImgBack() != null) {
                Map<String, Object> docBackAnalysis = analyzeBase64String("documentImgBack", request.getDocumentImgBack());
                analysis.put("documentImgBack", docBackAnalysis);
            }

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("Error analyzing base64 data", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private Map<String, Object> analyzeBase64String(String fieldName, String base64String) {
        Map<String, Object> analysis = new HashMap<>();

        if (base64String == null) {
            analysis.put("status", "NULL");
            return analysis;
        }

        analysis.put("originalLength", base64String.length());
        analysis.put("isEmpty", base64String.trim().isEmpty());
        analysis.put("containsComma", base64String.contains(","));
        analysis.put("containsWhitespace", base64String.matches(".*\\s.*"));
        analysis.put("containsNewlines", base64String.contains("\n") || base64String.contains("\r"));

        // Show first and last 50 characters
        String first50 = base64String.length() > 50 ? base64String.substring(0, 50) : base64String;
        String last50 = base64String.length() > 50 ?
                base64String.substring(base64String.length() - 50) : base64String;

        analysis.put("first50chars", first50);
        analysis.put("last50chars", last50);

        // Try to decode with our service
        try {
            DocumentUploadService tempService = new DocumentUploadService();
            byte[] decoded = tempService.decodeBase64Image(base64String);
            if (decoded != null) {
                analysis.put("decodingStatus", "SUCCESS");
                analysis.put("decodedBytes", decoded.length);

                // Try to determine if it's a valid image
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
                    BufferedImage image = ImageIO.read(bis);
                    bis.close();

                    if (image != null) {
                        analysis.put("isValidImage", true);
                        analysis.put("imageWidth", image.getWidth());
                        analysis.put("imageHeight", image.getHeight());
                        analysis.put("imageType", image.getType());
                    } else {
                        analysis.put("isValidImage", false);
                        analysis.put("imageError", "Could not create BufferedImage from decoded bytes");
                    }
                } catch (Exception imgError) {
                    analysis.put("isValidImage", false);
                    analysis.put("imageError", imgError.getMessage());
                }

            } else {
                analysis.put("decodingStatus", "FAILED");
                analysis.put("decodingError", "All decoding strategies failed");
            }
        } catch (Exception e) {
            analysis.put("decodingStatus", "ERROR");
            analysis.put("decodingError", e.getMessage());
        }

        return analysis;
    }

    @PostMapping("/api/customer/upload-documents")
    public ResponseEntity<CustomerRegistrationResponse> uploadDocuments(@RequestBody DocumentUploadRequest request) {
        log.info("Manual document upload request for customer: {}", request.getCustomerId());

        try {
            if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
                CustomerRegistrationResponse errorResponse = new CustomerRegistrationResponse();
                errorResponse.setRespCode("400");
                errorResponse.setMessage("Customer ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            int uploadResult = documentUploadService.uploadAllDocuments(
                    request.getCustomerId(),
                    request.getFaceImage(),
                    request.getDocumentFront(),
                    request.getDocumentBack()
            );

            CustomerRegistrationResponse response = new CustomerRegistrationResponse();
            if (uploadResult == 0) {
                response.setRespCode("000");
                response.setMessage("Documents uploaded successfully");
                log.info("Manual document upload successful for customer: {}", request.getCustomerId());
            } else {
                response.setRespCode("999");
                response.setMessage("Document upload failed");
                log.error("Manual document upload failed for customer: {}", request.getCustomerId());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during manual document upload for customer: " + request.getCustomerId(), e);
            CustomerRegistrationResponse errorResponse = new CustomerRegistrationResponse();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * DTO for manual document upload requests
     */
    public static class DocumentUploadRequest {
        private String customerId;
        private String faceImage;
        private String documentFront;
        private String documentBack;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getFaceImage() { return faceImage; }
        public void setFaceImage(String faceImage) { this.faceImage = faceImage; }

        public String getDocumentFront() { return documentFront; }
        public void setDocumentFront(String documentFront) { this.documentFront = documentFront; }

        public String getDocumentBack() { return documentBack; }
        public void setDocumentBack(String documentBack) { this.documentBack = documentBack; }
    }
}
