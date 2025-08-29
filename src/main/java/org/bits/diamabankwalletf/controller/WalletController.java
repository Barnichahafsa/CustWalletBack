package org.bits.diamabankwalletf.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.RequestListBanks;
import org.bits.diamabankwalletf.service.*;
import org.bits.diamabankwalletf.service.WalletTransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
