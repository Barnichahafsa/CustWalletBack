package org.bits.diamabankwalletf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEnquiryService {

    private final WebClient webClient;
    private final JdbcTemplate jdbcTemplate;
    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.wallet-name-enquiry}")
    private String walletEnquiryEndpoint;

    // Réutiliser cette méthode du WalletCreationService
    public String getServiceAccountToken() {
        return walletCreationService.getServiceAccountToken();
    }

    public ResponseServiceJson getWalletInfo(RequestWalletEnquiry request) {
        try {
            String url = walletBackendUrl + walletEnquiryEndpoint;
            log.info("Calling wallet enquiry endpoint at: {}", url);
            String token = getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            if (request.getBank() == null || request.getBank().isEmpty()) {
                request.setBank("00100");
            }

            // Ajoutez un log pour vérifier tous les champs
            Gson gson = new Gson();
            log.debug("Full request as JSON: {}", gson.toJson(request));

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet enquiry", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet enquiry: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .map(response -> {
                        if (!"000".equals(response.getRespCode())) {
                            String errorMessage = getErrorMessage(response.getRespCode());
                            if (errorMessage != null) {
                                response.setMessage(errorMessage);
                            }
                        }
                        return response;
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet enquiry", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet enquiry: " + e.getMessage());
            return errorResponse;
        }
    }

    public String getErrorMessage(String errorCode) {
        try {
            String sql = "SELECT MESSAGE FROM WALLET_MESSAGES WHERE RESPONSE_CODE = ?";
            return jdbcTemplate.queryForObject(sql, String.class, errorCode);
        } catch (Exception e) {
            log.error("Error getting error message for code: {}", errorCode, e);
            return null;
        }
    }
}
