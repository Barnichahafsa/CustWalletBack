package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestTopUpPurchase;
import org.bits.diamabankwalletf.dto.ResponseService;
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
public class TopUpPurchaseService {

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.top-up-purchase}")
    private String topUpPurchaseEndpoint;
    private final WebClient webClient;
    private final PinEncryptionUtil pinEncryptionUtil;
    private final WalletCreationService walletCreationService;

    public ResponseService topUpPurchase(RequestTopUpPurchase request) {
        try {
            String url = walletBackendUrl + topUpPurchaseEndpoint;
            log.info("Calling topUpPurchase transfer endpoint at: {}", url);

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
                    .doOnError(error -> log.error("Error calling topUpPurchase transfer", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling topUpPurchase transfer: " + error.getMessage());
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
            log.error("Error calling topUpPurchase transfer", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling topUpPurchase transfer: " + e.getMessage());
            return errorResponse;
        }
    }

}
