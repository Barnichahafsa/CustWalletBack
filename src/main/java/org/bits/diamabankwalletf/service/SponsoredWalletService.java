package org.bits.diamabankwalletf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bits.diamabankwalletf.dto.*;
import org.bits.diamabankwalletf.utils.PinEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class SponsoredWalletService {

    private final WebClient webClient;
    private final WalletCreationService walletCreationService;
    private final PinEncryptionUtil pinEncryptionUtil;

    @Value("${wallet.backend.url}")
    private String walletBackendUrl;

    @Value("${wallet.backend.endpoints.sponsor-wallet}")
    private String sponsorWalletEndpoint;

    @Value("${wallet.backend.endpoints.list-sponsored-wallets}")
    private String listSponsoredWalletsEndpoint;

    @Value("${wallet.backend.endpoints.list-sponsoring-wallets}")
    private String listSponsoringWalletsEndpoint;

    @Value("${wallet.backend.endpoints.unlink-sponsored-wallet}")
    private String unlinkSponsoredWalletEndpoint;

    @Value("${wallet.backend.endpoints.update-sponsored-wallet}")
    private String updateSponsoredWalletEndpoint;

    @Value("${wallet.backend.endpoints.daily-limit-sponsored-wallet}")
    private String dailyLimitsSponsoredWalletEndpoint;

    public ResponseService sponsorWalletDailyLimits(RequestGetDailyLimitSponsorWallet request) {
        try {
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

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                // Utiliser une valeur par défaut pour entityId
                request.setEntityId("WEBUSER");
                log.debug("Set default entityId: {}", request.getEntityId());
            }

            // Construire l'URL
            String url = walletBackendUrl + listSponsoringWalletsEndpoint;
            log.info("Calling daily limits endpoint at: {}", url);

            // Obtenir le token d'authentification
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Appeler le service backend
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
                        return reactor.core.publisher.Mono.just(errorResponse);
                    })
                    .block();

        } catch (Exception e) {
            log.error("Error calling list sponsoring wallets backend", e);
            ResponseService errorResponse = new ResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson updateSponsoredWallet(SponsoredWalletDto request) {
        try {
            log.info("Calling update sponsored wallet endpoint with request: {}", request);

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

            // Get authentication token
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token for sponsored wallet update (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            String url = walletBackendUrl + updateSponsoredWalletEndpoint;
            log.info("Calling update sponsored wallet endpoint at: {}", url);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            // Make API call to backend
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet backend", error))
                    .block();
        } catch (Exception e) {
            log.error("Error calling update sponsored wallet endpoint", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setStatus("NOK");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public ResponseServiceJson unlinkSponsoredWallet(SponsoredWalletDto request) {
        try {
            String url = walletBackendUrl + unlinkSponsoredWalletEndpoint;
            log.info("Calling unlink sponsored wallet endpoint at: {}", url);

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

            // Get API token
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }
            log.info(request.toString());
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization",  token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Error calling wallet backend", error))
                    .onErrorResume(error -> {
                        ResponseServiceJson errorResponse = new ResponseServiceJson();
                        errorResponse.setRespCode("999");
                        errorResponse.setStatus("NOK");
                        errorResponse.setMessage("Error calling wallet backend: " + error.getMessage());
                        return reactor.core.publisher.Mono.just(errorResponse);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling wallet backend", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setStatus("NOK");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public JResponseService getListSponsoringWallets(RequestListSponsoringWallets request) {
        try {
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

            if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
                // Utiliser une valeur par défaut pour entityId
                request.setEntityId("WEBUSER");
                log.debug("Set default entityId: {}", request.getEntityId());
            }

            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            // Construire l'URL
            String url = walletBackendUrl + listSponsoringWalletsEndpoint;
            log.info("Calling list sponsoring wallets endpoint at: {}", url);

            // Obtenir le token d'authentification
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            // Appeler le service backend
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
                        return reactor.core.publisher.Mono.just(errorResponse);
                    })
                    .block();

        } catch (Exception e) {
            log.error("Error calling list sponsoring wallets backend", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setMessage("Error calling wallet backend: " + e.getMessage());
            return errorResponse;
        }
    }

    public JResponseService getSponsoredWallets(RequestListSponsoredWallets request) {
        try {
            String url = walletBackendUrl +
                    (listSponsoredWalletsEndpoint != null ? listSponsoredWalletsEndpoint : "/ListSponsoredWallets");
            log.info("Calling sponsored wallets endpoint at: {}", url);

            // Get token from WalletCreationService
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Using token (first 10 chars): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            enrichRequestWithDefaults(request);

            log.debug("Request to sponsored wallets API: {}", request);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(JResponseService.class)
                    .doOnError(error -> log.error("Erreur lors de l'appel au backend wallet", error))
                    .block();
        } catch (Exception e) {
            log.error("Exception while calling sponsored wallets API", e);
            JResponseService errorResponse = new JResponseService();
            errorResponse.setRespCode("999");
            errorResponse.setStatus("NOK");
            errorResponse.setMessage("Error calling sponsored wallets API: " + e.getMessage());
            return errorResponse;
        }
    }

    private void enrichRequestWithDefaults(RequestListSponsoredWallets request) {
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

        // Set default bank if not provided
        if (request.getBank() == null || request.getBank().isEmpty()) {
            request.setBank("00100");  // Using a default bank code
            log.debug("Set default bank code: {}", request.getBank());
        }

        // Set default entityId if not provided
        if (request.getEntityId() == null || request.getEntityId().isEmpty()) {
            request.setEntityId("MOBILE_APP");  // Some default value
            log.debug("Set default entityId: {}", request.getEntityId());
        }

        if (request.getPin() != null && !request.getPin().isEmpty()) {
            String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
            log.info("PIN encrypted for bank code: {}", "00100");
            request.setPin(encryptedPin);
        }
    }

    public ResponseServiceJson sponsorWallet(SponsoredWalletDto request) {
        try {
            // Construire l'URL complète
            String url = walletBackendUrl + sponsorWalletEndpoint;
            log.info("Appel de l'API pour lier un portefeuille sponsorisé à l'URL: {}", url);

            // Générer un requestId si non fourni
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                SimpleDateFormat idFormat = new SimpleDateFormat("yyMMdd");
                String datePrefix = idFormat.format(new Date());
                Random random = new Random();
                String randomDigits = String.format("%06d", random.nextInt(1000000));
                request.setRequestId(datePrefix + randomDigits);
            }

            // Générer une requestDate si non fournie
            if (request.getRequestDate() == null || request.getRequestDate().isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                request.setRequestDate(dateFormat.format(new Date()));
            }

            // Obtenir le token d'accès
            String token = walletCreationService.getServiceAccountToken();
            log.debug("Utilisation du token (premiers 10 caractères): {}",
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);


            if (request.getPin() != null && !request.getPin().isEmpty()) {
                String encryptedPin = pinEncryptionUtil.encryptPin(request.getPin(), "00100");
                log.info("PIN encrypted for bank code: {}", "00100");
                request.setPin(encryptedPin);
            }

            log.debug("Corps de la requête: {}", request);



            // Appeler l'API de wallet backend
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(ResponseServiceJson.class)
                    .doOnError(error -> log.error("Erreur lors de l'appel au backend wallet", error))
                    .block();

        } catch (Exception e) {
            log.error("Erreur lors de l'appel au backend wallet", e);
            ResponseServiceJson errorResponse = new ResponseServiceJson();
            errorResponse.setRespCode("999");
            errorResponse.setStatus("NOK");
            errorResponse.setMessage("Erreur lors de l'appel au backend wallet: " + e.getMessage());
            return errorResponse;
        }
    }
}
