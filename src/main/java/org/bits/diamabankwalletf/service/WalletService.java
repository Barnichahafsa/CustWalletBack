package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.model.Wallet;
import org.bits.diamabankwalletf.repository.WalletRepository;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.deactivate-wallet}")
    private String deactivateWalletEndpoint;

    @Value("${wallet.backend.endpoints.list-secretQ}")
    private String secretQEndpoint;

    @Value("${wallet.backend.endpoints.estatement}")
    private String estatementendpoint;

    private final WebClient webClient;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final WalletCreationService walletCreationService;

    @Value("${wallet.backend.endpoints.last-30-days-trx}")
    private String last30DaysTrxEndpoint;

    public JResponseService getLast30DaysTransactions(RequestEStatement request) {
        try {
            String url = walletBackendUrl + last30DaysTrxEndpoint;
            log.info("Calling last 30 days transactions endpoint at: {}", url);

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
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                } else {
                    throw new IllegalArgumentException("Either walletNumber or phoneNumber must be provided");
                }
            }

            // Encrypt PIN if provided
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
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> log.error("Error calling last 30 days transactions", error))
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling last 30 days transactions: " + error.getMessage());
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
            log.error("Error calling last 30 days transactions", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling last 30 days transactions: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService deactivateWallet(RequestWalletDeactivation request) {
        try {
            String url = walletBackendUrl + deactivateWalletEndpoint;
            log.info("Calling deactivatewallet transfer endpoint at: {}", url);

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
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                } else {
                    throw new IllegalArgumentException("Either walletNumber or phoneNumber must be provided");
                }
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
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling deactivatewallet transfer", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling deactivatewallet transfer: " + error.getMessage());
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
            log.error("Error calling deactivatewallet transfer", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling deactivatewallet transfer: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService getPdfStatement(RequestEStatement request) {
        try {
            String url = walletBackendUrl + estatementendpoint;
            log.info("Calling estatement endpoint at: {}", url);

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
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                } else {
                    throw new IllegalArgumentException("Either walletNumber or phoneNumber must be provided");
                }
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
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling estatement", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling estatement: " + error.getMessage());
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
            log.error("Error calling estatement", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling estatement transfer: " + e.getMessage());
            return errorResponse;
        }
    }


    public JResponseService getWalletQuestionsList() {
        RequestListSecretQuestions request = new RequestListSecretQuestions();
        try {
            String url = walletBackendUrl + secretQEndpoint;
            log.info("Calling secretQ list endpoint at: {}", url);

            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
                log.debug("Generated requestId : {}", request.getRequestId());
            }

            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
                log.debug("Generated requestDate : {}", request.getRequestDate());
            }
            request.setBank("00100");
            request.setEntityId("CUSTOMER");
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first  10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Final request body : {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> log.error("Error calling secretQ list", error))
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling secretQ List: " + error.getMessage());
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
            log.error("Error calling secretQ list", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling secretQ list: " + e.getMessage());
            return errorResponse;
        }
    }

    public List<Map<String, Object>> getReasonList(String bankCode, String customerId) {
        log.info("Getting reason list for bankCode=[{}], customerId=[{}]", bankCode, customerId);

        try {
            // Similar to getWalletQuestionsList, this would call an external service
            // String url = "/api/wallet-services/reasons?bankCode=" + bankCode + "&customerId=" + customerId;
            // return restTemplate.getForObject(url, List.class);

            // For now, return an empty list or mock data
            List<Map<String, Object>> reasons = new ArrayList<>();

            // Example mock data (remove in production)
            Map<String, Object> r1 = new HashMap<>();
            r1.put("id", "R001");
            r1.put("label", "Personal Transfer");
            reasons.add(r1);

            Map<String, Object> r2 = new HashMap<>();
            r2.put("id", "R002");
            r2.put("label", "Bill Payment");
            reasons.add(r2);

            return reasons;
        } catch (Exception e) {
            log.error("Error getting reason list", e);
            return new ArrayList<>();
        }
    }





}
