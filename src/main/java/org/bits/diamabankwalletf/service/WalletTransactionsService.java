package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletTransactionsService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.last5-transactions}")
    private String last5TransactionsEndpoint;

    @Value("${wallet.backend.endpoints.pending-transactions}")
    private String pendingTransactionsEndpoint;


    @Value("${wallet.backend.endpoints.validate-transaction}")
    private String validateTransactionEndpoint;

    public ResponseService validateTransaction(RequestApproval request) {
        try {
            String url = walletBackendUrl + validateTransactionEndpoint;
            log.info("Calling validate transaction endpoint at: {}", url);
            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
            }

            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.debug("Generated requestId: {}", request.getRequestId());
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.debug("Generated requestDate: {}", request.getRequestDate());
            }

            // Encrypt PIN if provided
            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String bankCode = request.getBank();
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), bankCode);
                request.setPin(encryptedPin);
                log.info("PIN encrypted for bank code: {}", bankCode);
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for transaction validation");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for transaction validation", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for transaction validation", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling validation service: " + e.getMessage());
            return errorResponse;
        }
    }

    public JResponseService getLast5Transactions(RequestLast5Transactions request) {
        try {
            String url = walletBackendUrl + last5TransactionsEndpoint;
            log.info("Calling last 5 transactions endpoint at: {}", url);

            // Ensure required fields are set
            setupRequestFields(request);

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for transaction history");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for last 5 transactions", error))
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for last 5 transactions", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling last 5 transactions service: " + e.getMessage());
            return errorResponse;
        }
    }

    public JResponseService getPendingTransactions(RequestListPendingTransactions request) {
        try {
            String url = walletBackendUrl + pendingTransactionsEndpoint;
            log.info("Calling pending transactions endpoint at: {}", url);
            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.debug("Generated requestId: {}", request.getRequestId());
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.debug("Generated requestDate: {}", request.getRequestDate());
            }

            String plainPin = request.getPin();
            if (plainPin != null && !plainPin.isEmpty()) {
                String bankCode = request.getBank();
                if (bankCode == null || bankCode.isEmpty()) {
                    // Try to get bank code from the wallet number if not specified
                    bankCode = "00100"; // Default bank code if not available
                }

                // Encrypt the PIN
                String encryptedPin = pinEncryptionUtil.encryptPin(plainPin, bankCode);
                log.info("PIN encrypted for bank code: {}", bankCode);

                // Set the encrypted PIN back in the request
                request.setPin(encryptedPin);
            }

            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
                log.debug("Using default bank code: 00100");
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for transaction history");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for pending transactions", error))
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for pending transactions", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling pending transactions service: " + e.getMessage());
            return errorResponse;
        }
    }


    private void setupRequestFields(RequestLast5Transactions request) {
        // Generate requestId if not provided
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        // Generate requestDate if not provided
        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }

        String plainPin = request.getPin();
        if (plainPin != null && !plainPin.isEmpty()) {
            String bankCode = request.getBank();
            if (bankCode == null || bankCode.isEmpty()) {
                // Try to get bank code from the wallet number if not specified
                bankCode = "00100"; // Default bank code if not available
            }

            // Encrypt the PIN
            String encryptedPin = pinEncryptionUtil.encryptPin(plainPin, bankCode);
            log.info("PIN encrypted for bank code: {}", bankCode);

            // Set the encrypted PIN back in the request
            request.setPin(encryptedPin);
        }

        // Set default bank code if not provided
        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100");
            log.debug("Using default bank code: 00100");
        }
    }
}
