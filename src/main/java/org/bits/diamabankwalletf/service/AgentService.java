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
public class AgentService {

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.agent-activity}")
    private String agentActivityEndpoint;

    @Value("${wallet.backend.endpoints.agent-statement}")
    private String agentStatementEndpoint;

    @Value("${wallet.backend.endpoints.agent-cashout}")
    private String agentCashOutEndpoint;

    @Value("${wallet.backend.endpoints.agent-voucher}")
    private String agentVoucherEndpoint;

    @Value("${wallet.backend.endpoints.agent-cash-voucher}")
    private String agentCashVoucherEndpoint;

    @Value("${wallet.backend.endpoints.agent-wallet-to-account}")
    private String agentWalletToAccountEndpoint;

    @Value("${wallet.backend.endpoints.agent-to-wallet}")
    private String agentToWalletEndpoint;

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;


    public JResponseService getAgentActivity(RequestAgentActivity request) {
        try {
            String url = walletBackendUrl + agentActivityEndpoint;
            log.info("Calling agent activity endpoint at: {}", url);

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
                    .doOnError(error -> log.error("Error calling wallet backend for agent activity", error))
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for agent activity", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling agent activity service: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService getAgentStatement(RequestAgenteStatement request) {
        try {
            String url = walletBackendUrl + agentStatementEndpoint;
            log.info("Calling agent statement endpoint at: {}", url);

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

            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
                log.debug("Using default bank code: 00100");
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for agent statement");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for agent statement", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for agent statement", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling agent statement service: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService debitWallet(RequestDebitWallet request) {
        try {
            String url = walletBackendUrl + agentCashOutEndpoint;
            log.info("Calling agent cashout at: {}", url);

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

            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
                log.debug("Using default bank code: 00100");
            }

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for agent cashout");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for agent cashout", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for agent cashout", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling agent cashout service: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService agentMoneyVoucher(RequestMoneyVoucher request) {
        try {
            String url = walletBackendUrl + agentVoucherEndpoint;
            log.info("Calling agent voucher at: {}", url);

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

            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
                log.debug("Using default bank code: 00100");
            }

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for agent voucher");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for agent voucher", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for agent voucher", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling agent voucher service: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson agentCashMoneyVoucher(RequestCashMoneyVoucher request) {
        try {
            String url = walletBackendUrl + agentCashVoucherEndpoint;
            log.info("Calling agent cash voucher at: {}", url);

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

            // Set default bank code if not provided
            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
                log.debug("Using default bank code: 00100");
            }

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for agent cash voucher");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet backend for agent cash voucher", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for agent cash voucher", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling agent cash voucher service: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService agentToAccount(RequestWalletToBankAccount request) {
        try {
            String url = walletBackendUrl + agentWalletToAccountEndpoint;
            log.info("Calling agent wallet to account at: {}", url);

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

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for agent wallet to account ");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for agent to account", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for agent wallet to account", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling agent wallet to account service: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService agentToWallet(RequestWalletToWallet request) {
        try {
            String url = walletBackendUrl + agentToWalletEndpoint;
            log.info("Calling agent to wallet at: {}", url);

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

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Request body: {}", request);

            // Get the token from the existing WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for agent to wallet");

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend for agent to wallet", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend for agent to wallet", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling agent to wallet service: " + e.getMessage());
            return errorResponse;
        }
    }



}
