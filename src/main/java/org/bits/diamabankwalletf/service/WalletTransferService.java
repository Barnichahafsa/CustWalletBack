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
public class WalletTransferService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.wallet-to-wallet}")
    private String walletToWalletEndpoint;

    @Value("${wallet.backend.endpoints.wallet-to-account}")
    private String walletToAccountEndpoint;

    @Value("${wallet.backend.endpoints.wallet-to-account-ne}")
    private String walletToAccountNeEndpoint;

    @Value("${wallet.backend.endpoints.account-to-wallet}")
    private String accountToWalletEndpoint;

    @Value("${wallet.backend.endpoints.mobile-money-ne}")
    private String mobileMoneyNeEndpoint;

    public ResponseServiceJson transferToWallet(RequestWalletToWallet request) {
        try {
            String url = walletBackendUrl + walletToWalletEndpoint;
            log.info("Calling wallet-to-wallet transfer endpoint at: {}", url);

            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            // Reuse the token generation from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body: {}", request);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            request.setSrcBank("00100");


            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token) // Add Bearer prefix as required by API
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet-to-wallet transfer", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet-to-wallet transfer: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet-to-wallet transfer", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet-to-wallet transfer: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson transferToAccount(RequestWalletToBankAccount request) {
        try {
            String url = walletBackendUrl + walletToAccountEndpoint;
            log.info("Calling wallet-to-account transfer endpoint at: {}", url);

            // Always set the source bank
            request.setSrcBank("00100");
            log.debug("Set srcBank: {}", request.getSrcBank());

            if (request.getAuthCode() == null || request.getAuthCode().isEmpty()) {
                log.warn("No authCode provided - name enquiry should be performed first");
            }

            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            // Reuse the token generation from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body before PIN encryption: {}", request);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            request.setBankDes("00100");
            log.debug("Final request body: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet-to-account transfer", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet-to-account transfer: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet-to-account transfer", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet-to-account transfer: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson accountNameEnquiry(RequestWalletToBankAccountNE request) {
        try {
            String url = walletBackendUrl + walletToAccountNeEndpoint;
            log.info("Calling account name enquiry endpoint at: {}", url);

            // Generate requestId if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            // Generate requestDate if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            // Set default source if not provided
            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                } else {
                    throw new IllegalArgumentException("Either walletNumber or phoneNumber must be provided");
                }
            }

            // Set default values for srcBank if needed
            if (request.getSrcBank() == null || request.getSrcBank().isEmpty()) {
                request.setSrcBank("00100"); // Default bank code if not available
            }
            request.setDesBank("00100");

            // Reuse the token generation from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars) : {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body : {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling account name enquiry", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling account name enquiry: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling account name enquiry", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling account name enquiry: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson accountToWallet(AccountToWalletRequest request) {
        try {
            String url = walletBackendUrl + accountToWalletEndpoint;
            log.info("Calling account-to-wallet transfer endpoint at: {}", url);

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

            // Validate source type
            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W"); // Wallet number provided
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P"); // Phone number provided
                } else {
                    throw new IllegalArgumentException("Either walletNumber or phoneNumber must be provided");
                }
            }

            // Set default banks if not provided
            if (request.getSrcBank() == null || request.getSrcBank().isEmpty()) {
                request.setSrcBank("00100"); // Default source bank
            }

            if (request.getDesBank() == null || request.getDesBank().isEmpty()) {
                request.setDesBank("00100"); // Default destination bank
            }


            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }
            // Get authentication token
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Final request body: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling account-to-wallet transfer", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling account-to-wallet transfer: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = walletCreationService.getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling account-to-wallet transfer", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling account-to-wallet transfer: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService transferToMobileMoney(RequestWalletToMB request) {
        try {
            String url = walletBackendUrl + "/WalletToMobileMoney";
            log.info("Calling mobile money transfer endpoint at: {}", url);

            // Add requestId and requestDate if not provided
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

            // Get token from token service
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Request details: {}", request);


            // Make the API call
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for mobile money transfer", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setStatus("NOK");
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for mobile money transfer", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setStatus("NOK");
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson mobileMoneyNameEnquiry(RequestWalletToMobileMoneyNE request) {
        try {
            // Generate necessary fields if not provided
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

            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
            }

            String url = walletBackendUrl + mobileMoneyNeEndpoint;
            log.info("Calling mobile money name enquiry endpoint at: {}", url);

            // Get service account token
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body: {}", request);

            // Make the API call
            ResponseServiceJson response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet backend", error))
                    .block();

            log.info("Mobile money name enquiry completed with response code: {}",
                    response != null ? response.getRespCode() : "null");

            return response;
        } catch (Exception e) {
            log.error("Error calling wallet backend", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

}
