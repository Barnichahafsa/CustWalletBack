package org.bits.diamabankwalletf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.Customer;
import org.bits.diamabankwalletf.model.RequestListBanks;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.CustomerRepository;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.service.*;
import org.bits.diamabankwalletf.service.WalletTransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletEnquiryService walletEnquiryService;
    private final WalletTransferService walletTransferService;
    private final WalletTransactionsService walletTransactionsService;
    private final WalletAccountService walletAccountService;
    private final MoneyVoucherService moneyVoucherService;
    private final WalletService walletService;
    private final JwtTokenService jwtTokenService;
    private final BankService bankService;
    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;

    @PostMapping("/qr/customer")
    public ResponseEntity<ResponseServiceJson> getOrGenerateCustomerQR(@RequestBody RequestGetCustomerQR request) {
        log.info("Received customer QR request for: {}", request.getCustomerIdentifier());

        ResponseServiceJson response = new ResponseServiceJson();

        try {
            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.info("Generated requestId: {}", request.getRequestId());
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.info("Generated requestDate: {}", request.getRequestDate());
            }

            // Validate customer identifier
            if (request.getCustomerIdentifier() == null || request.getCustomerIdentifier().trim().isEmpty()) {
                response.setStatus("NOK");
                response.setRespCode("400");
                response.setMessage("Customer identifier is required");
                response.setRequestId(request.getRequestId());
                return ResponseEntity.badRequest().body(response);
            }

            // Try to find customer by phone number first, then by customer ID
            Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(request.getCustomerIdentifier());

            if (!customerOpt.isPresent()) {
                customerOpt = customerRepository.findByCustomerId(request.getCustomerIdentifier());
            }

            if (!customerOpt.isPresent()) {
                response.setStatus("NOK");
                response.setRespCode("111");
                response.setMessage("Customer not found");
                response.setRequestId(request.getRequestId());
                log.info("Customer not found with identifier: {}", request.getCustomerIdentifier());
                return ResponseEntity.ok(response);
            }

            Customer customer = customerOpt.get();
            log.info("Customer found: customerId={}, phoneNumber={}", customer.getCustomerId(), customer.getPhoneNumber());

            // Get wallet number
            Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(customer.getPhoneNumber());
            if (!walletOpt.isPresent()) {
                response.setStatus("NOK");
                response.setRespCode("111");
                response.setMessage("Wallet not found for customer");
                response.setRequestId(request.getRequestId());
                log.error("Wallet not found for customer phone number: {}", customer.getPhoneNumber());
                return ResponseEntity.ok(response);
            }

            String walletNumber = walletOpt.get().getWalletNumber();

            // Check if customer already has QR data
            if (customer.getQrData() != null && !customer.getQrData().isEmpty()) {
                log.info("Customer already has QR data, returning existing QR");

                // Create response with existing QR data
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode resultNode = objectMapper.createObjectNode();
                resultNode.put("qrData", customer.getQrData());
                resultNode.put("customerId", customer.getCustomerId());
                resultNode.put("customerName", buildCustomerName(customer));
                resultNode.put("walletNumber", walletNumber);
                resultNode.put("entityType", "CUSTOMER");

                response.setStatus("OK");
                response.setRespCode("000");
                response.setMessage("Customer QR data retrieved successfully");
                response.setRequestId(request.getRequestId());
                response.setResult(resultNode);

                return ResponseEntity.ok(response);
            }

            // Generate new static QR code
            log.info("Generating new static QR code for customer: {} with wallet: {}",
                    customer.getCustomerId(), walletNumber);

            // Generate QR using local method
            String qrData = generateCustomerQRData(walletNumber, customer);

            if (qrData == null || qrData.isEmpty()) {
                response.setStatus("NOK");
                response.setRespCode("999");
                response.setMessage("Failed to generate QR code");
                response.setRequestId(request.getRequestId());
                return ResponseEntity.ok(response);
            }

            // Save QR data to customer
            customer.setQrData(qrData);
            customerRepository.save(customer);
            log.info("QR data saved to customer: {}", customer.getCustomerId());

            // Create success response
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode resultNode = objectMapper.createObjectNode();
            resultNode.put("qrData", qrData);
            resultNode.put("customerId", customer.getCustomerId());
            resultNode.put("customerName", buildCustomerName(customer));
            resultNode.put("walletNumber", walletNumber);
            resultNode.put("entityType", "CUSTOMER");

            response.setStatus("OK");
            response.setRespCode("000");
            response.setMessage("Customer QR code generated successfully");
            response.setRequestId(request.getRequestId());
            response.setResult(resultNode);

            log.info("Customer QR generation completed successfully for: {}", customer.getCustomerId());

        } catch (Exception e) {
            log.error("Error processing customer QR request for: {}", request.getCustomerIdentifier(), e);
            response.setStatus("NOK");
            response.setRespCode("999");
            response.setMessage("Internal server error: " + e.getMessage());
            response.setRequestId(request.getRequestId());
            return ResponseEntity.status(500).body(response);
        }

        log.info("Customer QR request completed with response code: {}", response.getRespCode());
        return ResponseEntity.ok(response);
    }

    // Helper method to build customer name
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

    // Helper method to extract city from customer or wallet data
    private String extractCityFromCustomer(Wallet wallet) {
        // Try to get city from wallet address
        if (wallet.getAddress1() != null && !wallet.getAddress1().isEmpty()) {
            String[] parts = wallet.getAddress1().split(",");
            if (parts.length > 0) {
                String city = parts[parts.length - 1].trim();
                if (!city.isEmpty()) return city;
            }
        }

        // Check city code
        if (wallet.getCityCode1() != null && !wallet.getCityCode1().isEmpty()) {
            return wallet.getCityCode1();
        }

        return "CONAKRY"; // Default city
    }

    // Helper method to generate static EMVCo compliant QR data for customer
    private String generateCustomerQRData(String walletNumber, Customer customer) {
        try {
            log.info("Generating static EMVCo QR code for customer wallet: {}", walletNumber);

            StringBuilder qrPayload = new StringBuilder();

            // 1. MANDATORY: Payload Format Indicator (ID "00")
            qrPayload.append(buildDataObject("00", "01"));
            log.info("Added Payload Format Indicator: 00");

            // 2. MANDATORY: Point of Initiation Method (ID "01") - static
            qrPayload.append(buildDataObject("01", "11"));
            log.info("Added Point of Initiation Method: 01 (static)");

            // 3. MANDATORY: Merchant Account Information (ID "26")
            String merchantAccountInfo = buildCustomerAccountTemplate(walletNumber, customer);
            qrPayload.append(buildDataObject("26", merchantAccountInfo));
            log.info("Added Merchant Account Information: 26");

            // 4. MANDATORY: Merchant Category Code (ID "52")
            String mcc = "6211"; // Personal financial services
            qrPayload.append(buildDataObject("52", mcc));
            log.info("Added Merchant Category Code: 52 = {}", mcc);

            // 5. MANDATORY: Transaction Currency (ID "53") - ISO 4217 numeric code for GNF
            qrPayload.append(buildDataObject("53", "324")); // GNF = 324
            log.info("Added Transaction Currency: 53 = 324 (GNF)");

            // 6. MANDATORY: Country Code (ID "58") - ISO 3166-1 alpha-2
            qrPayload.append(buildDataObject("58", "GN")); // Guinea
            log.info("Added Country Code: 58 = GN");

            // 7. MANDATORY: Merchant Name (ID "59")
            String customerName = buildCustomerName(customer);
            qrPayload.append(buildDataObject("59", truncateString(customerName, 25)));
            log.info("Added Merchant Name: 59 = {}", truncateString(customerName, 25));

            // 8. MANDATORY: Merchant City (ID "60")
            // Get wallet for city information
            Optional<Wallet> walletOpt = walletRepository.findByPhoneNumber(customer.getPhoneNumber());
            String city = walletOpt.isPresent() ? extractCityFromCustomer(walletOpt.get()) : "CONAKRY";
            qrPayload.append(buildDataObject("60", truncateString(city, 15)));
            log.info("Added Merchant City: 60 = {}", truncateString(city, 15));

            // 9. OPTIONAL: Additional Data Field Template (ID "62")
            String additionalData = buildCustomerAdditionalDataTemplate(customer);
            if (!additionalData.isEmpty()) {
                qrPayload.append(buildDataObject("62", additionalData));
                log.info("Added Additional Data Field Template: 62");
            }

            // 10. MANDATORY: CRC (ID "63") - must be last
            String crcInput = qrPayload.toString() + "6304";
            String crc = calculateCRC16CCITT(crcInput);
            qrPayload.append(buildDataObject("63", crc));
            log.info("Added CRC: 63 = {}", crc);

            String finalQRCode = qrPayload.toString();
            log.info("EMVCo QR Code generated. Length: {} characters", finalQRCode.length());

            return finalQRCode;

        } catch (Exception e) {
            log.error("Error generating customer QR code", e);
            return null;
        }
    }

    // Helper method to build customer account template
    private String buildCustomerAccountTemplate(String walletNumber, Customer customer) {
        StringBuilder template = new StringBuilder();

        // Sub-field 00: Globally Unique Identifier
        template.append(buildDataObject("00", "org.bits.diamabankwalletf"));

        // Sub-field 01: Merchant Account (wallet number)
        template.append(buildDataObject("01", truncateString(walletNumber, 32)));

        // Sub-field 02: Bank Code
        if (customer.getBankCode() != null && !customer.getBankCode().isEmpty()) {
            template.append(buildDataObject("02", truncateString(customer.getBankCode(), 20)));
        }

        // Sub-field 03: Customer ID
        if (customer.getCustomerId() != null && !customer.getCustomerId().isEmpty()) {
            String shortCustomerId = customer.getCustomerId().length() >= 16 ?
                    customer.getCustomerId().substring(0, 16) : customer.getCustomerId();
            template.append(buildDataObject("03", shortCustomerId));
        }

        // Sub-field 04: Entity Type
        template.append(buildDataObject("04", "CUSTOMER"));

        return template.toString();
    }

    // Helper method to build customer additional data template
    private String buildCustomerAdditionalDataTemplate(Customer customer) {
        StringBuilder template = new StringBuilder();

        // Sub-field 05: Reference Label (customer ID)
        if (customer.getCustomerId() != null && !customer.getCustomerId().isEmpty()) {
            String refLabel = truncateString(customer.getCustomerId(), 25);
            template.append(buildDataObject("05", refLabel));
        }

        // Sub-field 07: Terminal Label (entity type)
        template.append(buildDataObject("07", "CUSTOMER"));

        return template.toString();
    }

    // Helper method to build data object in EMVCo format
    private String buildDataObject(String id, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (id.length() != 2) {
            throw new IllegalArgumentException("ID must be exactly 2 digits: " + id);
        }

        byte[] valueBytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int length = valueBytes.length;

        if (length > 99) {
            throw new IllegalArgumentException("Value too long (max 99 bytes): " + length);
        }

        String lengthStr = String.format("%02d", length);
        return id + lengthStr + value;
    }

    // Helper method to calculate CRC16-CCITT
    private String calculateCRC16CCITT(String data) {
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
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

    // Helper method to truncate string to max length
    private String truncateString(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private BillPaymentResponse transformResponse(ResponseService backendResponse, RequestInitiateDSD request) {
        BillPaymentResponse response = new BillPaymentResponse();

        // Map respCode to code
        response.setCode(backendResponse.getRespCode() != null ? backendResponse.getRespCode() : "999");
        response.setMessage(backendResponse.getMessage());

        // Build data object
        BillPaymentResponse.BillPaymentData data = new BillPaymentResponse.BillPaymentData();

        // Map status: "OK" if respCode is "000" or "00", otherwise "KO"
        if ("000".equals(backendResponse.getRespCode()) || "00".equals(backendResponse.getRespCode())) {
            data.setStatus("OK");
            data.setResult("EN ATTENTE");
        } else {
            data.setStatus("KO");
            data.setResult("Error");
        }

        data.setNumeroTelephone(request.getNumeroTelephone());
        data.setCategVehicule(request.getCategVehicule());
        data.setNumeroChassis(request.getNumeroChassis());
        data.setAmount(request.getAmount());

        response.setData(data);
        return response;
    }


    @PostMapping("/initiateBill")
    public ResponseEntity<BillPaymentResponse> initiatebill(@RequestBody RequestInitiateDSD request) {
        log.info("Initiate bill request: {}", request);

        // Call backend and get ResponseService
        ResponseService backendResponse = walletTransferService.initiateBillDsd(request);

        // Transform to BillPaymentResponse
        BillPaymentResponse response = transformResponse(backendResponse, request);

        if ("000".equals(backendResponse.getRespCode()) || "00".equals(backendResponse.getRespCode())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    @PostMapping("/limits")
    public ResponseEntity<ResponseGetWalletLimits> getWalletLimits(@RequestBody RequestGetWalletLimits request) {
        log.info("Get wallet limits request: {}", request);
        ResponseGetWalletLimits response = walletTransferService.getWalletLimits(request);

        if ("000".equals(response.getRespCode())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/calculate-fee")
    public ResponseEntity<Map<String, Object>> calculateTransactionFee(@RequestBody FeeCalculationRequest request) {
        try {
            log.info("Received fee calculation request for processing code: {}", request.getProcessingCode());

            // Validate required fields
            if (request.getProcessingCode() == null || request.getProcessingCode().trim().isEmpty()) {
                return createFeeErrorResponse("001", "Processing code is required");
            }

            if (request.getBankCode() == null || request.getBankCode().trim().isEmpty()) {
                request.setBankCode("00100"); // Default bank code
            }

            if (request.getActionCode() == null || request.getActionCode().trim().isEmpty()) {
                return createFeeErrorResponse("002", "Action code is required (DEB/CRD)");
            }

            if (request.getCurrencyCode() == null || request.getCurrencyCode().trim().isEmpty()) {
                request.setCurrencyCode("324"); // Default currency
            }

            if (request.getTransactionAmount() == null || request.getTransactionAmount() <= 0) {
                return createFeeErrorResponse("003", "Valid transaction amount is required");
            }

            // Set defaults for optional fields
            if (request.getWalletProductCode() == null) {
                request.setWalletProductCode("001");
            }
            if (request.getWalletType() == null) {
                request.setWalletType("P");
            }
            if (request.getOrigin() == null) {
                request.setOrigin("W");
            }

            // Generate request tracking fields if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            log.debug("Calculating fee for: ProcessingCode={}, Amount={}, ActionCode={}",
                    request.getProcessingCode(), request.getTransactionAmount(), request.getActionCode());

            // Call the simple fee calculation service (direct database call)
            FeeCalculationResponse feeResponse = walletTransferService.calculateFee(request);

            // Build response
            Map<String, Object> response = new HashMap<>();

            if ("000".equals(feeResponse.getRespCode())) {
                response.put("respCode", "000");
                response.put("message", "Fee calculated successfully");

                Map<String, Object> feeData = new HashMap<>();
                feeData.put("feeAmount", feeResponse.getFeeAmount());
                feeData.put("transactionAmount", request.getTransactionAmount());
                feeData.put("totalAmount", feeResponse.getFeeAmount() + request.getTransactionAmount());
                feeData.put("processingCode", request.getProcessingCode());
                feeData.put("actionCode", request.getActionCode());
                feeData.put("currency", request.getCurrencyCode());
                feeData.put("ruleIndex", feeResponse.getRuleIndex());
                feeData.put("requestId", request.getRequestId());

                response.put("result", feeData);

                log.info("Fee calculated successfully: {} for amount {}",
                        feeResponse.getFeeAmount(), request.getTransactionAmount());
            } else {
                response.put("respCode", feeResponse.getRespCode());
                response.put("message", feeResponse.getMessage());

                log.warn("Fee calculation failed: {} - {}",
                        feeResponse.getRespCode(), feeResponse.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calculating transaction fee", e);
            return createFeeErrorResponse("999", "Internal server error: " + e.getMessage());
        }
    }


    private ResponseEntity<Map<String, Object>> createFeeErrorResponse(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("respCode", code);
        response.put("message", message);
        response.put("result", Map.of("feeAmount", 0));

        log.debug("Created fee error response - Code: {}, Message: {}", code, message);

        return ResponseEntity.ok(response);
    }
    @PostMapping("/list-secretQ")
    public ResponseEntity<JResponseService> listSecretQ() {
        JResponseService response = walletService.getWalletQuestionsList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/last5")
    public ResponseEntity<JResponseService> getLast5Transactions(@RequestBody RequestLast5Transactions request) {
        log.info("Received request for last 5 transactions for wallet from source: {}", request.getSource());

        String identifier = "W".equals(request.getSource()) ?
                "wallet: " + request.getWalletNumber() :
                "phone: " + request.getPhoneNumber();
        log.info("Getting last 5 transactions for {}", identifier);

        // Set type to "P" for last 5 transactions
        request.setType("P");

        // Fill in missing required fields
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
        }

        JResponseService response = walletTransactionsService.getLast5Transactions(request);

        log.info("Last 5 transactions request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/pending")
    public ResponseEntity<JResponseService> getPendingTransactions(@RequestBody RequestListPendingTransactions request) {
        log.info("Received request for pending transactions for wallet from source: {}", request.getSource());

        String identifier = "W".equals(request.getSource()) ?
                "wallet: " + request.getWalletNumber() :
                "phone: " + request.getPhoneNumber();
        log.info("Getting pending transactions for {}", identifier);


        // Fill in missing required fields
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
        }

        JResponseService response = walletTransactionsService.getPendingTransactions(request);

        log.info("Pending transactions request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-transaction")
    public ResponseEntity<ResponseService> validateTransaction(@RequestBody RequestApproval request) {
        log.info("Received validate transaction request: {}", request.getRequestId());

        ResponseService response = walletTransactionsService.validateTransaction(request);

        log.info("Validate transaction completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/enquiry")
    public ResponseEntity<ResponseServiceJson> getWalletInfo(@RequestBody RequestWalletEnquiry request) {
        log.info("Received wallet enquiry request for wallet: {}", request.getWalletNumber());

        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }
        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100");
        }

        // Call the service
        ResponseServiceJson response = walletEnquiryService.getWalletInfo(request);

        log.info("Wallet enquiry completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/qrtransfer")
    public ResponseEntity<ResponseService> qrtransferToWallet(@RequestBody RequestQrPayment request) {
        log.info("Received qr transfer request: {}", request.getRequestId());
        request.setBank("00100");
        ResponseService response = walletTransferService.qrtransferToWallet(request);
        log.info("QR transfer response code: {}", response.getRespCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<ResponseServiceJson> transferToWallet(@RequestBody RequestWalletToWallet request) {
        log.info("Received wallet-to-wallet transfer request: {}", request.getRequestId());
        request.setSrcBank("00100");
        ResponseServiceJson response = walletTransferService.transferToWallet(request);
        log.info("Wallet transfer response code: {}", response.getRespCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer-to-account")
    public ResponseEntity<ResponseServiceJson> transferToAccount(@RequestBody RequestWalletToBankAccount request) {
        log.info("Received wallet-to-account transfer request: {}", request.getRequestId());
        ResponseServiceJson response = walletTransferService.transferToAccount(request);

        log.info("Wallet-to-account transfer completed with response code: {}", response.getRespCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/account-name-enquiry")
    public ResponseEntity<ResponseServiceJson> accountNameEnquiry(@RequestBody RequestWalletToBankAccountNE request) {
        log.info("Received account name enquiry request for account: {}", request.getDesAccount());

        ResponseServiceJson response = walletTransferService.accountNameEnquiry(request);

        log.info("Account name enquiry completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/linked-accounts")
    public ResponseEntity<JResponseService> getLinkedAccounts(@RequestBody RequestListLinkedAcc request) {
        String identifier = "W".equals(request.getSource()) ?
                "wallet: " + request.getWalletNumber() :
                "phone: " + request.getPhoneNumber();

        log.info("Received linked accounts request for {}", identifier);

        JResponseService response = walletAccountService.getLinkedAccounts(request);

        log.info("Linked accounts request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/account-to-wallet")
    public ResponseEntity<ResponseServiceJson> accountToWallet(@RequestBody AccountToWalletRequest request) {
        String identifier = "W".equals(request.getSource()) ?
                "wallet: " + request.getWalletNumber() :
                "phone: " + request.getPhoneNumber();

        log.info("Received account-to-wallet transfer request for {} from account: {}",
                identifier, request.getSrcAccount());

        ResponseServiceJson response = walletTransferService.accountToWallet(request);

        log.info("Account-to-wallet transfer completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/wallet-to-mobile-money")
    public ResponseEntity<ResponseService> transferToMobileMoney(@RequestBody RequestWalletToMB request) {
        log.info("Received wallet-to-mobile-money transfer request: {}", request.getRequestId());

        ResponseService response = walletTransferService.transferToMobileMoney(request);

        log.info("Wallet-to-mobile-money transfer completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/money-voucher")
    public ResponseEntity<ResponseService> moneyVoucher(@RequestBody RequestMoneyVoucher request) {
        log.info("Received money voucher: {}", request.getRequestId());
        ResponseService response = moneyVoucherService.moneyVoucher(request);
        log.info("Money voucher request completed with response code: {}", response.getRespCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/lastThirty")
    public ResponseEntity<JResponseService> lastThirty(@RequestBody RequestEStatement request) {
        log.info("Received wallet last thirty request: {}", request.getRequestId());

        JResponseService response = walletService.getLast30DaysTransactions(request);

        log.info("Wallet 30 days transactions request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }


    @PostMapping("/deactivate")
    public ResponseEntity<ResponseService> deactivateWallet(@RequestBody RequestWalletDeactivation request) {
        log.info("Received wallet deactivation request: {}", request.getRequestId());

        ResponseService response = walletService.deactivateWallet(request);

        log.info("Wallet deactivation request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/estatement")
    public ResponseEntity<ResponseService> getPdfStatement(@RequestBody RequestEStatement request) {
        log.info("Received wallet statement request: {}", request.getRequestId());

        ResponseService response = walletService.getPdfStatement(request);

        log.info("Wallet statement request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/listBanks")
    public ResponseEntity<JResponseService> listBanks(@RequestBody RequestListBanks request) {
        log.info("Received list banks request: {}", request.getRequestId());

        JResponseService response = bankService.listbanks(request);

        log.info("List banks request completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/mobile-money-enquiry")
    public ResponseEntity<ResponseServiceJson> mobileMoneyNameEnquiry(@RequestBody RequestWalletToMobileMoneyNE request) {
        log.info("Received mobile money name enquiry request for receiver: {}", request.getReceiverPhone());

        // Ensure required fields are present
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }

        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100");
        }

        // Call the service
        ResponseServiceJson response = walletTransferService.mobileMoneyNameEnquiry(request);

        log.info("Mobile money name enquiry completed with response code: {}", response.getRespCode());

        return ResponseEntity.ok(response);
    }

    /**
     * Enhanced JWT/QR validation endpoint that handles both EMV QR codes and JWT tokens
     */
    @PostMapping("/validate-jwt")
    public ResponseEntity<Map<String, Object>> validateJWT(@RequestBody ValidateJwtRequest request) {
        try {
            log.info("Received QR/JWT validation request");

            // Validate input
            if (request.getJwtToken() == null || request.getJwtToken().trim().isEmpty()) {
                return createErrorResponse("001", "QR code or JWT token is required");
            }

            String qrContent = request.getJwtToken().trim();
            log.debug("Validating content of length: {}", qrContent.length());

            // Use the enhanced validation method that handles both EMV QR codes and JWT tokens
            JwtTokenService.JwtValidationResult result = jwtTokenService.validateQRCode(qrContent);

            if (result.isSuccess()) {
                JwtTokenService.PaymentData paymentData = result.getPaymentData();

                Map<String, Object> response = new HashMap<>();
                response.put("respCode", "000");
                response.put("message", "QR code validated successfully");

                Map<String, Object> data = new HashMap<>();
                data.put("tokenId", paymentData.getTokenId());
                data.put("merchantWallet", paymentData.getMerchantWallet());
                data.put("merchantNumber", paymentData.getMerchantNumber());
                data.put("merchantName", paymentData.getMerchantName());
                data.put("bankCode", paymentData.getBankCode());
                data.put("amount", paymentData.getAmount().toString());
                data.put("currency", paymentData.getCurrency());
                data.put("expiresAt", paymentData.getExpiresAt().toString());

                response.put("result", data);

                log.info("QR code validated successfully for tokenId: {}", paymentData.getTokenId());

                return ResponseEntity.ok(response);
            } else {
                log.error("QR code validation failed: {}", result.getErrorMessage());

                // Determine specific error code based on error message
                String errorCode = determineErrorCode(result.getErrorMessage());

                return createErrorResponse(errorCode, result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error in QR/JWT validation", e);
            return createErrorResponse("999", "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Enhanced token marking endpoint that handles both EMV QR codes and token IDs
     */
    @PostMapping("/mark-token-used")
    public ResponseEntity<Map<String, Object>> markTokenUsed(@RequestBody MarkTokenUsedRequest request) {
        try {
            log.info("Marking token as used - TokenId/QR: {}",
                    request.getTokenId() != null && request.getTokenId().length() > 20 ?
                            request.getTokenId().substring(0, 20) + "..." : request.getTokenId());

            // Validate input
            if (request.getTokenId() == null || request.getTokenId().trim().isEmpty()) {
                return createErrorResponse("001", "Token ID or QR code is required");
            }

            if (request.getCustomerWallet() == null || request.getCustomerWallet().trim().isEmpty()) {
                return createErrorResponse("002", "Customer wallet is required");
            }

            if (request.getTransactionRef() == null || request.getTransactionRef().trim().isEmpty()) {
                return createErrorResponse("003", "Transaction reference is required");
            }

            // Use enhanced method that handles both EMV QR codes and token IDs
            boolean success = jwtTokenService.markTokenAsUsed(
                    request.getTokenId().trim(),
                    request.getCustomerWallet().trim(),
                    request.getTransactionRef().trim()
            );

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("respCode", "000");
                response.put("message", "Token marked as used successfully");

                log.info("Token marked as used successfully");
                return ResponseEntity.ok(response);
            } else {
                log.error("Failed to mark token as used");
                return createErrorResponse("007", "Failed to mark token as used");
            }

        } catch (Exception e) {
            log.error("Error marking token as used", e);
            return createErrorResponse("999", "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Determine error code based on error message content
     */
    private String determineErrorCode(String errorMessage) {
        if (errorMessage == null) return "004";

        String lowerMessage = errorMessage.toLowerCase();

        if (lowerMessage.contains("expired")) {
            return "005"; // QR code expired
        } else if (lowerMessage.contains("used") || lowerMessage.contains("already")) {
            return "006"; // QR code already used
        } else if (lowerMessage.contains("invalid token") || lowerMessage.contains("not found")) {
            return "004"; // Invalid token
        } else if (lowerMessage.contains("crc") || lowerMessage.contains("format") ||
                lowerMessage.contains("parse") || lowerMessage.contains("emv")) {
            return "007"; // QR code parsing/format error
        } else if (lowerMessage.contains("database") || lowerMessage.contains("sql")) {
            return "999"; // Database error
        } else {
            return "004"; // Default validation error
        }
    }

    /**
     * Create standardized error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("respCode", code);
        response.put("message", message);

        log.debug("Created error response - Code: {}, Message: {}", code, message);

        return ResponseEntity.ok(response);
    }
}
