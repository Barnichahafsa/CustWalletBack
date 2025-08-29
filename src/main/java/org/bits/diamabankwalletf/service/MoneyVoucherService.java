package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestCheckPin;
import org.bits.diamabankwalletf.dto.RequestMoneyVoucher;
import org.bits.diamabankwalletf.dto.ResponseService;
import org.bits.diamabankwalletf.dto.ResponseServiceJson;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoneyVoucherService {

    private final WebClient webClient;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final WalletCreationService walletCreationService;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.money-voucher}")
    private String moneyVoucherEndpoint;

    public ResponseService moneyVoucher(RequestMoneyVoucher request) {
        try {
            String url = walletBackendUrl + moneyVoucherEndpoint;
            log.info("Calling money voucher endpoint at: {}", url);

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
                    request.setSource("W"); // W for Wallet Number
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P"); // P for Phone Number
                }
            }

            // Ensure entityId is set
            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }

            // Extract bank code from the wallet number (first 5 digits = bank code)
            String bankCode = "00100"; // Default bank code
            if (request.getWalletNumber() != null && request.getWalletNumber().length() >= 5) {

            }

            // Encrypt PIN using the proper bank encryption
            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), bankCode);
                log.info("PIN encrypted for bank code: {}", bankCode);
                request.setPin(encryptedPin);
            }

            // Get token using the working WalletCreationService method
            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for PIN check");
                ResponseService errorResponse = new ResponseService();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            // Log request details (excluding sensitive data)
            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId());

            log.debug("Request details: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)  // Use token directly without Bearer prefix, like in WalletCreationService
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> {
                        log.error("Error calling PIN check API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling PIN check API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        log.info("PIN check response: code={}, message={}",
                                response.getRespCode(), response.getMessage());
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling PIN check API", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling PIN check API: " + e.getMessage());
            return errorResponse;
        }
    }


}
