package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestBalanceEnquiry;
import org.bits.diamabankwalletf.dto.ResponseServiceJson;
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
public class WalletBalanceService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.wallet-balance}")
    private String walletBalanceEndpoint;

    public ResponseServiceJson getWalletBalance(RequestBalanceEnquiry request) {
        try {
            String url = walletBackendUrl + walletBalanceEndpoint;
            log.info("Calling wallet balance endpoint at: {}", url);

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

            // Reuse the token generation from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.debug("Request body: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet balance API", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet balance API: " + error.getMessage());
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
            log.error("Error calling wallet balance API", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet balance API: " + e.getMessage());
            return errorResponse;
        }
    }

}
