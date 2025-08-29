package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandingInstructionService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.standing-instruction-list}")
    private String standingInstructionListEndpoint;

    @Value("${wallet.backend.endpoints.create-si}")
    private String createSiEndpoint;

    @Value("${wallet.backend.endpoints.update-si}")
    private String updateSIEndpoint;

    @Value("${wallet.backend.endpoints.delete-standing-instruction}")
    private String deleteStandingInstructionEndpoint;

    public ResponseService deleteStandingInstruction(RequestStandingInstructionDelete request) {
        try {
            String url = walletBackendUrl + deleteStandingInstructionEndpoint;
            log.info("Calling standing instruction deletion endpoint at: {}", url);

            // Get token using WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);



            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend", error))
                    .onErrorResume(error -> {
                        ResponseService errorResponse = new ResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setStatus("NOK");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setStatus("NOK");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseService updateStandingInstruction(StandingInstructionUpdateRequest request) {
        try {
            String url = walletBackendUrl + updateSIEndpoint;
            log.info("Calling standing instruction update endpoint at: {}", url);
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            request.setPin(pinEncryptionUtil.encryptPin(request.getPin(), "00100"));

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
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
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson createStandingInstruction(StandingInstructionDTO request) {
        try {
            String url = walletBackendUrl + createSiEndpoint;
            log.info("Calling standing instruction creation endpoint at: {}", url);

            // Get the token from wallet creation service
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Generate requestId in format yymmddxxxxxx if not provided
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            // Generate requestDate in format yyyy-mm-dd hh:mm:ss if not provided
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("phoneNumber", request.getPhoneNumber());
            requestMap.put("trxType", request.getTrxType());
            requestMap.put("destBank", request.getDestBank());
            requestMap.put("receiver", request.getReceiver());
            requestMap.put("destination", request.getDestination());
            requestMap.put("amount", request.getAmount());
            requestMap.put("currency", request.getCurrency());
            requestMap.put("details", request.getDetails());
            requestMap.put("pin", pinEncryptionUtil.encryptPin(request.getPin(), "00100"));
            requestMap.put("frequency", request.getFrequency());
            requestMap.put("startDate", request.getStartDate());
            requestMap.put("requestId", request.getRequestId());
            requestMap.put("requestDate", request.getRequestDate());
            requestMap.put("entityId", request.getEntityId());

            // Include endDate only if it's provided
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                requestMap.put("endDate", request.getEndDate());
            }

            log.debug("Request body: {}", requestMap);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestMap)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling standing instruction API", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setStatus("NOK");
                        errorResponse.setMessage("Error calling standing instruction API: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling standing instruction API", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setStatus("NOK");
            errorResponse.setMessage("Error calling standing instruction API: " + e.getMessage());
            return errorResponse;
        }
    }

    public JResponseService listStandingInstructions(RequestStandingInstructionList request) {
        try {
            String url = walletBackendUrl + standingInstructionListEndpoint;
            log.info("Calling standing instruction list endpoint at: {}", url);

            // Get the token from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Generate required fields if not provided
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

            log.debug("Request body: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> log.error("Error calling wallet backend", error))
                    .onErrorResume(error -> {
                        JResponseService errorResponse = new JResponseService();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling standing instruction list service", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }


}
