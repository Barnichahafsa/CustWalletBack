package org.bits.diamabankwalletf.service;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletCreationService {

    private final WebClient webClient;
    private final JdbcTemplate jdbcTemplate;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.create-wallet}")
    private String createWalletEndpoint;

    @Value("${wallet.backend.api.username}")
    private String apiUsername;

    @Value("${wallet.backend.api.password}")
    private String apiPassword;


    public String getServiceAccountToken() {
        try {
            log.info("Obtaining API token with service account: {}", apiUsername);
            TokenRequest tokenRequest = new TokenRequest();
            tokenRequest.setUsername(apiUsername);
            tokenRequest.setPassword(apiPassword);
            SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
            String datePrefix = idFormat.format(new Date());
            Random random = new Random();
            String randomDigits = String.format("%06d", random.nextInt(1000000));
            tokenRequest.setRequestId(datePrefix + randomDigits);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            tokenRequest.setRequestDate(dateFormat.format(new Date()));

            log.debug("Token request: {}", tokenRequest);

            ResponseEntity<ResponseServiceJson> response = webClient.post()
                    .uri(walletBackendUrl + "/getToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(tokenRequest)
                    .retrieve()
                    .toEntity(ResponseServiceJson.class)
                    .block();

            if (response != null && response.getBody() != null &&
                    "000".equals(response.getBody().getRespCode())) {

                log.info("Successfully obtained API token");
                String token = null;

                // PRIMARY METHOD: Extract token from Authorization header
                List<String> authHeaders = response.getHeaders().get(HttpHeaders.AUTHORIZATION);
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    String authHeader = authHeaders.get(0);
                    if (authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                        log.debug("Token extracted from Authorization header (first 15 chars): {}",
                                token.length() > 15 ? token.substring(0, 15) + "..." : token);
                        return token;
                    } else {
                        token = authHeader;
                        log.debug("Token extracted from Authorization header without Bearer prefix");
                    }
                }

                List<String> tokenHeaders = response.getHeaders().get("Token");
                if (token == null && tokenHeaders != null && !tokenHeaders.isEmpty()) {
                    token = tokenHeaders.get(0);
                    log.debug("Token extracted from Token header");
                    return token;
                }

                ResponseServiceJson body = response.getBody();
                if (token == null && body != null) {
                    if (body.getAuthCode() != null && body.getAuthCode().length() > 20) {
                        token = body.getAuthCode();
                        log.debug("Token extracted from authCode field");
                        return token;
                    }

                    if (body.getResult() != null) {
                        JsonNode resultNode = body.getResult();
                        if (resultNode.has("token")) {
                            token = resultNode.get("token").asText();
                            log.debug("Token extracted from result.token field");
                            return token;
                        } else if (resultNode.has("access_token")) {
                            token = resultNode.get("access_token").asText();
                            log.debug("Token extracted from result.access_token field");
                            return token;
                        } else if (resultNode.has("jwt")) {
                            token = resultNode.get("jwt").asText();
                            log.debug("Token extracted from result.jwt field");
                            return token;
                        }

                        log.debug("Result structure: {}", resultNode.toString());
                    }
                }
                if (token != null) {
                    return token;
                }
                log.error("Could not extract JWT token from response");
                throw new RuntimeException("Token not found in response");
            } else {
                String errorMsg = (response != null && response.getBody() != null) ?
                        response.getBody().getMessage() : "null response";
                log.error("Failed to obtain API token: {}", errorMsg);
                throw new RuntimeException("Failed to obtain API token: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Error obtaining API token", e);
            throw new RuntimeException("Error obtaining API token", e);
        }
    }

    public ResponseService createWallet(WalletCreationRequest request) {
        try {
            String url = walletBackendUrl + createWalletEndpoint;
            log.info("Calling wallet creation endpoint at: {}", url);
            String token = getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            Map<String, Object> requestMap = new HashMap<>();

            requestMap.put("firstName", request.getFirstName());
            requestMap.put("lastName", request.getLastName());
            requestMap.put("phone", request.getPhone());
            requestMap.put("email", request.getEmail());
            requestMap.put("birthDate", request.getBirthDate());
            requestMap.put("nationality", request.getNationality());
            requestMap.put("documentCode", request.getDocumentCode());
            requestMap.put("documentId","  ");
            requestMap.put("gender", request.getGender());

            requestMap.put("currencyCode", request.getCurrencyCode());
            requestMap.put("bank", request.getBank());
            requestMap.put("branchCode", request.getBranchCode());
            requestMap.put("secretQ", request.getSecretQ());
            requestMap.put("answer", request.getAnswer());
            requestMap.put("address", request.getAddress());
            requestMap.put("type", request.getType());
            requestMap.put("pin", request.getPin());
            requestMap.put("requestId", request.getRequestId());
            requestMap.put("requestDate", request.getRequestDate());
            requestMap.put("entityId", request.getEntityId());
            requestMap.put("nkinName", request.getNkinName());
            requestMap.put("nkinPhone", request.getNkinPhone());
            requestMap.put("srcFunds", "  ");
            requestMap.put("occupation", "  ");
            requestMap.put("estimatedIncome", "  ");

            log.debug("Request body: {}", requestMap);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestMap)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
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
            log.error("Error calling wallet backend", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
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
