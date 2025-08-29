package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.JResponseService;
import org.bits.diamabankwalletf.dto.RequestListLinkedAcc;
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
public class WalletAccountService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;
    @Value("${wallet.backend.endpoints.wallet-linked-accounts}")
    private String walletLinkedAccountsUrl;


    public JResponseService getLinkedAccounts(RequestListLinkedAcc request) {
        try {
            log.info("Fetching linked accounts for: {}",
                    "W".equals(request.getSource()) ? request.getWalletNumber() : request.getPhoneNumber());

            // Ensure request parameters are set
            prepareRequest(request);

            // Get authentication token using WalletCreationService
            String token = walletCreationService.getServiceAccountToken();

            // Call the wallet backend
            return webClient.post()
                    .uri(walletBackendUrl + walletLinkedAccountsUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization",  token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnSuccess(response -> log.info("Successfully retrieved linked accounts data"))
                    .doOnError(error -> log.error("Error retrieving linked accounts: {}", error.getMessage()))
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error retrieving linked accounts: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();

        } catch (Exception e) {
            log.error("Exception while fetching linked accounts", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error retrieving linked accounts: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Ensures required request parameters are set according to API specifications
     */
    private void prepareRequest(RequestListLinkedAcc request) {
        // Set default source if not provided
        if (request.getSource() == null || request.getSource().isEmpty()) {
            if (request.getWalletNumber() != null && !request.getWalletNumber().isEmpty()) {
                request.setSource("W");
                log.debug("Set source to 'W' (Wallet Number)");
            } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                request.setSource("P");
                log.debug("Set source to 'P' (Phone Number)");
            } else {
                log.warn("Both walletNumber and phoneNumber are missing");
            }
        }

        // Generate requestId if not provided (format: yymmddxxxxxx)
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            request.setRequestId(datePrefix + randomDigits);
            log.debug("Generated requestId: {}", request.getRequestId());
        }

        // Generate requestDate if not provided (format: yyyy-MM-dd HH:mm:ss)
        if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.setRequestDate(dateFormat.format(new Date()));
            log.debug("Generated requestDate: {}", request.getRequestDate());
        }

        // Set default bank code if not provided
        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100");
            log.debug("Set default bank code: 00100");
        }

        // Set entityId if not provided
        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("WEBAPP");
            log.debug("Set default entityId: WEBAPP");
        }
    }
}
