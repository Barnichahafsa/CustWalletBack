package org.bits.diamabankwalletf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DSDService {

    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.pending-operations}")
    private String pendingOperationsEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    // ================================================================
    // INITIATE PAYMENT
    // ================================================================
    public DSDResponse initiateDSDPayment(String phoneNumber, String chassisNumber,
                                          String category, String amount) {

        log.info("➡️ INITIATE DSD PAYMENT | phone={}, chassis={}, category={}, amount={}",
                phoneNumber, chassisNumber, category, amount);

        DSDResponse response = new DSDResponse();

        try {
            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                return error401(response);
            }

            // Build request body using a Map (Cleaner than JSONObject)
            Map<String, Object> body = new HashMap<>();
            body.put("phoneNumber", phoneNumber);
            body.put("chassisNumber", chassisNumber);
            body.put("category", category);
            body.put("amount", amount);
            body.put("bank", "DBANK");
            body.put("provider", "DSD");
            body.put("destination", chassisNumber);
            body.put("currency", "324");
            body.put("entityId", "DSD_SYSTEM");
            body.put("requestId", generateRequestId());
            body.put("requestDate", now());
            body.put("key", generateTransactionKey());

            HttpEntity<?> request = buildRequest(token, body);

            String url = walletBackendUrl + "/dsd/initiate";
            log.info("🌐 POST {}", url);
            log.info("📤 Payload: {}", body);

            ResponseEntity<Map> apiResponse =
                    restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            return parseDSDResponse(apiResponse, response);

        } catch (Exception e) {
            return handleException(e, response);
        }
    }

    // ================================================================
    // GET PENDING OPERATIONS
    // ================================================================
    public ResponseService getPendingOperations(String phoneNumber) {
        log.info("️ GET PENDING OPERATIONS | phone={}", phoneNumber);

        ResponseService response = new ResponseService();

        try {
            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                return error401(response);
            }

            HttpEntity<?> request = buildRequest(token, null);

            String url = walletBackendUrl + pendingOperationsEndpoint + "/" + phoneNumber;
            log.info("🌐 GET {}", url);

            ResponseEntity<Map> apiResponse =
                    restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            return parseResponseService(apiResponse, response);

        } catch (Exception e) {
            return handleExceptionRS(e, response);
        }
    }

    public ResponseService queryPaymentStatus(RequestInitiateDSD request) {
        log.info("➡️ QUERY DSD PAYMENT STATUS | phone={}, category={}, chassis={}, amount={}",
                request.getNumeroTelephone(),
                request.getCategVehicule(),
                request.getNumeroChassis(),
                request.getAmount());

        ResponseService response = new ResponseService();

        try {
            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                response.setStatus("ANNULE");
                response.setRespCode("401");
                response.setMessage("Unauthorized - Failed to get service token");
                return response;
            }

            // Build request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("numeroTelephone", request.getNumeroTelephone());
            requestBody.put("categVehicule", request.getCategVehicule());
            requestBody.put("numeroChassis", request.getNumeroChassis());
            requestBody.put("amount", request.getAmount());

            HttpEntity<?> httpRequest = buildRequest(token, requestBody);

            String url = walletBackendUrl + "/dsd/status";
            log.info("🌐 POST {} with body: {}", url, requestBody);

            ResponseEntity<Map> apiResponse =
                    restTemplate.exchange(url, HttpMethod.POST, httpRequest, Map.class);

            // Parse response from backend
            return parseResponseServiceFromMap(apiResponse, response);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle HTTP error responses (4xx, 5xx)
            log.error("HTTP error from backend: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            try {
                // Try to parse the error response body
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> errorBody = mapper.readValue(e.getResponseBodyAsString(), Map.class);

                response.setStatus((String) errorBody.get("status"));
                response.setRespCode((String) errorBody.get("respCode"));
                response.setMessage((String) errorBody.get("message"));
                response.setAuthCode((String) errorBody.get("authCode"));
                response.setRequestId((String) errorBody.get("requestId"));
                response.setResult((String) errorBody.get("result"));

                return response;
            } catch (Exception parseEx) {
                log.error("Failed to parse error response: {}", parseEx.getMessage());
                response.setStatus("ANNULE");
                response.setRespCode("500");
                response.setMessage("System error");
                return response;
            }

        } catch (Exception e) {
            log.error("Error querying DSD payment status: {}", e.getMessage(), e);
            response.setStatus("ANNULE");
            response.setRespCode("500");
            response.setMessage("System error");
            return response;
        }
    }

    // Helper method to parse Map response to ResponseService
    private ResponseService parseResponseServiceFromMap(ResponseEntity<Map> apiResponse, ResponseService response) {
        if (apiResponse == null || apiResponse.getBody() == null) {
            response.setStatus("ANNULE");
            response.setRespCode("500");
            response.setMessage("Empty response from backend");
            return response;
        }

        Map<String, Object> body = apiResponse.getBody();

        response.setStatus((String) body.get("status"));
        response.setRespCode((String) body.get("respCode"));
        response.setMessage((String) body.get("message"));
        response.setAuthCode((String) body.get("authCode"));
        response.setRequestId((String) body.get("requestId"));
        response.setResult((String) body.get("result"));

        log.info("✓ Backend response parsed - Status: {}, Code: {}",
                response.getStatus(), response.getRespCode());

        return response;
    }

    // ================================================================
    // VALIDATE PAYMENT
    // ================================================================
    public DSDResponse validatePayment(String operationId, String phoneNumber, String pin) {
        log.info("➡️ VALIDATE DSD PAYMENT | opId={}", operationId);

        DSDResponse response = new DSDResponse();

        try {
            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                return error401(response);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("operationId", operationId);
            body.put("phoneNumber", phoneNumber);
            String encryptedPin = "";
            // Encrypt PIN if provided
            if (pin != null && !pin.isEmpty()) {
               encryptedPin = pinEncryptionUtil.encryptPin(pin, "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
            }
            body.put("pin", encryptedPin);
            body.put("requestId", generateRequestId());

            HttpEntity<?> request = buildRequest(token, body);

            String url = walletBackendUrl + "/validate";
            log.info("🌐 POST {}", url);

            ResponseEntity<Map> apiResponse =
                    restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            return parseDSDResponse(apiResponse, response);

        } catch (Exception e) {
            return handleException(e, response);
        }
    }

    // ================================================================
    // REJECT PAYMENT
    // ================================================================
    public DSDResponse rejectPayment(String operationId, String phoneNumber) {
        log.info("➡️ REJECT DSD PAYMENT | opId={}", operationId);

        DSDResponse response = new DSDResponse();

        try {
            String token = walletCreationService.getServiceAccountToken();
            if (token == null) {
                return error401(response);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("operationId", operationId);
            body.put("phoneNumber", phoneNumber);
            body.put("requestId", generateRequestId());

            HttpEntity<?> request = buildRequest(token, body);

            String url = walletBackendUrl + "/reject";
            log.info("🌐 POST {}", url);

            ResponseEntity<Map> apiResponse =
                    restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            return parseDSDResponse(apiResponse, response);

        } catch (Exception e) {
            return handleException(e, response);
        }
    }

    // ================================================================
    // HELPERS — SAME STYLE AS WalletService
    // ================================================================
    private HttpEntity<?> buildRequest(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", token);

        return (body == null)
                ? new HttpEntity<>(headers)
                : new HttpEntity<>(body, headers);
    }

    private DSDResponse parseDSDResponse(ResponseEntity<Map> apiResponse, DSDResponse out) {
        Map<String, Object> json = apiResponse.getBody();
        if (json == null) return out;

        out.setStatus(str(json.get("status")));
        out.setRespCode(str(json.get("respCode")));
        out.setMessage(str(json.get("message")));
        out.setResult(str(json.get("result")));
        out.setAuthCode(str(json.get("authCode")));
        out.setRequestId(str(json.get("requestId")));

        return out;
    }

    private ResponseService parseResponseService(ResponseEntity<Map> apiResponse, ResponseService out) {
        Map<String, Object> json = apiResponse.getBody();
        if (json == null) return out;

        out.setStatus(str(json.get("status")));
        out.setRespCode(str(json.get("respCode")));
        out.setMessage(str(json.get("message")));
        out.setResult(str(json.get("result")));

        return out;
    }

    private String str(Object o) {
        return (o == null) ? null : o.toString();
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
    }

    private String generateRequestId() {
        return "DSD" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + (int)(Math.random() * 900 + 100);
    }

    private String generateTransactionKey() {
        return "KEY" + System.currentTimeMillis();
    }

    private DSDResponse error401(DSDResponse r) {
        r.setStatus("ANNULE");
        r.setRespCode("401");
        r.setMessage("Authentication failed");
        return r;
    }

    private ResponseService error401(ResponseService r) {
        r.setStatus("ANNULE");
        r.setRespCode("401");
        r.setMessage("Authentication failed");
        return r;
    }

    private DSDResponse handleException(Exception e, DSDResponse r) {
        log.error("❌ ERROR: {}", e.getMessage(), e);
        r.setStatus("ANNULE");
        r.setRespCode("500");
        r.setMessage("Error: " + e.getMessage());
        return r;
    }

    private ResponseService handleExceptionRS(Exception e, ResponseService r) {
        log.error("❌ ERROR: {}", e.getMessage(), e);
        r.setStatus("ANNULE");
        r.setRespCode("500");
        r.setMessage("Error: " + e.getMessage());
        return r;
    }
}
