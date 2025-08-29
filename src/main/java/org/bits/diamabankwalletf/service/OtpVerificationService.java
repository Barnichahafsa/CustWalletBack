package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.RequestCheckOtp;
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
public class OtpVerificationService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService; // Needed for token

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.verify-otp}")
    private String verifyOtpEndpoint;

    /**
     * Verify OTP with the wallet backend service
     *
     * @param phoneNumber User's phone number
     * @param authCode Authorization code received from registration
     * @param otp OTP code entered by the user
     * @param entityId Entity ID for the transaction
     * @return Response from the OTP verification service
     */
    public ResponseServiceJson verifyOtp(String phoneNumber, String authCode, String otp, String entityId) {
        try {
            log.info("Verifying OTP for phone: {}, authCode: {}", phoneNumber, authCode);

            // Get token from existing service
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Create the request object
            RequestCheckOtp request = new RequestCheckOtp();
            request.setPhoneNumber(phoneNumber);
            request.setAuthCode(authCode);
            request.setOtp(otp);
            request.setRequestId(generateRequestId());
            request.setRequestDate(generateTimestamp());
            request.setEntityId(entityId);

            log.debug("OTP verification request: {}", request);

            String url = walletBackendUrl + verifyOtpEndpoint;
            log.info("Calling OTP verification endpoint at: {}", url);

            // Use WebClient to make the call
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error verifying OTP", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setMessage("Error verifying OTP: " + error.getMessage());
                        return Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error verifying OTP", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error verifying OTP: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Generate request ID in the format yyMMddXXXXXX where XXXXXX is 6 random digits
     */
    private String generateRequestId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String datePrefix = sdf.format(new Date());

        Random random = new Random();
        String randomDigits = String.format("%06d", random.nextInt(1000000));

        return datePrefix + randomDigits;
    }

    /**
     * Generate timestamp for request in the format yyyy-MM-dd HH:mm:ss
     */
    private String generateTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
