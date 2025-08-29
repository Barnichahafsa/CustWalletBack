package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestCheckPin;
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
public class PinCheckService {

    private final WebClient webClient;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final WalletCreationService walletCreationService;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.check-pin}")
    private String checkPinEndpoint;

    public ResponseServiceJson checkPin(RequestCheckPin request) {
        try {
            String url = walletBackendUrl + checkPinEndpoint;
            log.info("Calling PIN check endpoint at: {}", url);

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

            if (request.getSource() == null || request.getSource().isEmpty()) {
                if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                    request.setSource("W");
                } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                    request.setSource("P");
                }
            }

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                request.setEntityId("CUSTOMER");
            }

            String bankCode = "00100";
            if (request.getWalletNumber() != null && request.getWalletNumber().length() >= 5) {

            }

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), bankCode);
                log.info("PIN encrypted for bank code: {}", bankCode);
                request.setPin(encryptedPin);
            }

            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                log.error("Failed to obtain token for PIN check");
                ResponseServiceJson errorResponse = new ResponseServiceJson();
                errorResponse.setRespCode("999");
                errorResponse.setMessage("Error obtaining authentication token");
                return errorResponse;
            }

            log.debug("Request details: walletNumber={}, source={}, requestId={}, requestDate={}, entityId={}",
                    request.getWalletNumber(), request.getSource(), request.getRequestId(),
                    request.getRequestDate(), request.getEntityId());

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> {
                        log.error("Error calling PIN check API", error);
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException wcError = (WebClientResponseException) error;
                            log.error("Response status: {}", wcError.getStatusCode());
                            log.error("Response body: {}", wcError.getResponseBodyAsString());
                        }
                    })
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
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
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling PIN check API: " + e.getMessage());
            return errorResponse;
        }
    }
}
